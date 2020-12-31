/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rpc.core.demo.discovery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.rpc.core.demo.api.ProviderInfo;
import com.rpc.core.demo.balance.LoadBalance;
import com.rpc.core.demo.balance.loadbalance.ConsistentHashBalance;
import com.rpc.core.demo.balance.loadbalance.WeightBalance;
import com.rpc.core.demo.filter.client.FilterLine;
import com.rpc.core.demo.proxy.RpcClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 服务发现客户端
 * 获取Provider列表
 * 监听Provider更新
 * 查找返回接口的Provider（先tag路由，后负载均衡）
 *
 * @author lw1243925457
 */
@Slf4j
public class DiscoveryClient extends ZookeeperClient {

    private enum EnumSingleton {
        /**
         * 懒汉枚举单例
         */
        INSTANCE;
        private DiscoveryClient instance;

        EnumSingleton(){
            instance = new DiscoveryClient();
        }
        public DiscoveryClient getSingleton(){
            return instance;
        }
    }

    public static DiscoveryClient getInstance(){
        return EnumSingleton.INSTANCE.getSingleton();
    }

    /**
     * Provider缓存列表
     * server:group:version -> provider instance list
     */
    private Map<String, List<ProviderInfo>> providersCache = new HashMap<>();

    private final ServiceDiscovery<ProviderInfo> serviceDiscovery;

    private final CuratorCache resourcesCache;

    private LoadBalance balance = new WeightBalance();

    private DiscoveryClient() {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ProviderInfo.class)
                .client(client)
                .basePath("/" + REGISTER_ROOT_PATH)
                .build();

        try {
            serviceDiscovery.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            getAllProviders();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.resourcesCache = CuratorCache.build(this.client, "/");
        watchResources();

        if (RpcClient.getBalanceAlgorithmName().equals(WeightBalance.NAME)) {
            this.balance = new WeightBalance();
        }
        else if (RpcClient.getBalanceAlgorithmName().equals(ConsistentHashBalance.NAME)) {
            this.balance = new ConsistentHashBalance();
        }
    }

    /**
     * 从ZK中获取所有的Provider列表，保存下来
     * @throws Exception exception
     */
    private void getAllProviders() throws Exception {
        System.out.println("\n\n======================= init : get all provider");

        Collection<String>  serviceNames = serviceDiscovery.queryForNames();
        System.out.println(serviceNames.size() + " type(s)");
        for ( String serviceName : serviceNames ) {
            Collection<ServiceInstance<ProviderInfo>> instances = serviceDiscovery.queryForInstances(serviceName);
            System.out.println(serviceName);

            for ( ServiceInstance<ProviderInfo> instance : instances ) {
                System.out.println(instance.toString());

                String url = "http://" + instance.getAddress() + ":" + instance.getPort();
                ProviderInfo providerInfo = instance.getPayload();
                providerInfo.setId(instance.getId());
                providerInfo.setUrl(url);

                List<ProviderInfo> providerList = providersCache.getOrDefault(instance.getName(), new ArrayList<>());
                providerList.add(providerInfo);
                providersCache.put(instance.getName(), providerList);

                System.out.println("add provider: " + instance.toString());
            }
        }

        System.out.println();
        for(String key: providersCache.keySet()) {
            System.out.println(key + " : " + providersCache.get(key));
        }

        System.out.println("======================= init : get all provider end\n\n");
    }

    /**
     * 根据传入的接口名称、分组、版本，返回讲过tag路由，负载均衡后的一个Provider服务器地址
     * @param service service name
     * @param group group
     * @param version version
     * @param tags tags
     * @param methodName method name
     * @return provider host ip
     */
    public String getProviders(String service, String group, String version, List<String> tags, String methodName) {
        String provider = Joiner.on(":").join(service, group, version);
        if (!providersCache.containsKey(provider) || providersCache.get(provider).isEmpty()) {
            return null;
        }

        List<ProviderInfo> providers = FilterLine.filter(providersCache.get(provider), tags);
        if (providers.isEmpty()) {
            return null;
        }

        return balance.select(providers, service, methodName);
    }

    /**
     * 监听Provider的更新
     */
    private void watchResources() {
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(this::addHandler)
                .forChanges(this::changeHandler)
                .forDeletes(this::deleteHandler)
                .forInitialized(() -> log.info("Resources Cache initialized"))
                .build();
        resourcesCache.listenable().addListener(listener);
        resourcesCache.start();
    }

    /**
     * 增加Provider
     * @param node new provider
     */
    private void addHandler(ChildData node) {
        System.out.println("\n\n=================== add new provider ============================");

        System.out.printf("Node created: [%s:%s]%n", node.getPath(), new String(node.getData()));
        if (providerDataEmpty(node)) {
            return;
        }

        updateProvider(node);

        System.out.println("=================== add new provider end ============================\n\n");
    }

    /**
     * Provider更新
     * @param oldNode old provider
     * @param newNode updated provider
     */
    private void changeHandler(ChildData oldNode, ChildData newNode) {
        System.out.printf("Node changed, Old: [%s: %s] New: [%s: %s]%n", oldNode.getPath(),
                new String(oldNode.getData()), newNode.getPath(), new String(newNode.getData()));

        if (providerDataEmpty(newNode)) {
            return;
        }

        updateProvider(newNode);
    }

    /**
     * 增加或更新本地Provider
     * @param newNode updated provider
     */
    private void updateProvider(ChildData newNode) {
        String jsonValue = new String(newNode.getData(), StandardCharsets.UTF_8);
        JSONObject instance = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(instance.toString());

        String url = "http://" + instance.get("address") + ":" + instance.get("port");
        ProviderInfo providerInfo = JSON.parseObject(instance.get("payload").toString(), ProviderInfo.class);
        providerInfo.setId(instance.get("id").toString());
        providerInfo.setUrl(url);

        List<ProviderInfo> providerList = providersCache.getOrDefault(instance.get("name").toString(), new ArrayList<>());
        providerList.add(providerInfo);
        providersCache.put(instance.get("name").toString(), providerList);
    }

    /**
     * 删除Provider
     * @param oldNode provider
     */
    private void deleteHandler(ChildData oldNode) {
        System.out.println("\n\n=================== delete provider ============================");

        System.out.printf("Node deleted, Old value: [%s: %s]%n", oldNode.getPath(), new String(oldNode.getData()));
        if (providerDataEmpty(oldNode)) {
            return;
        }

        String jsonValue = new String(oldNode.getData(), StandardCharsets.UTF_8);
        JSONObject instance = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(instance.toString());

        String provider = instance.get("name").toString();
        int deleteIndex = -1;
        for (int i = 0; i < providersCache.get(provider).size(); i++) {
            if (providersCache.get(provider).get(i).getId().equals(instance.get("id").toString())) {
                deleteIndex = i;
                break;
            }
        }

        if (deleteIndex != -1) {
            providersCache.get(provider).remove(deleteIndex);
        }

        System.out.println("=================== delete provider end ============================\n\n");
    }

    private boolean providerDataEmpty(ChildData node) {
        return node.getData().length == 0;
    }

    public synchronized void close() {
        client.close();
    }
}
