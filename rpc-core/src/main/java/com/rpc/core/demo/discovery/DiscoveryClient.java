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

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.rpc.core.demo.api.ProviderInfo;
import com.rpc.core.demo.filter.FilterLine;
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
     * group -> version -> provider cache -> provider instance
     */
    private Map<String, List<ProviderInfo>> providersCache = new HashMap<>();

    private final ServiceDiscovery<String> serviceDiscovery;
    private final CuratorCache resourcesCache;

    public DiscoveryClient() {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(String.class)
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
    }

    private void getAllProviders() throws Exception {
        System.out.println("\n\n======================= init : get all provider");

        Collection<String>  serviceNames = serviceDiscovery.queryForNames();
        System.out.println(serviceNames.size() + " type(s)");
        for ( String serviceName : serviceNames ) {
            Collection<ServiceInstance<String>> instances = serviceDiscovery.queryForInstances(serviceName);
            System.out.println(serviceName);

            for ( ServiceInstance<String> instance : instances ) {
                System.out.println(instance.toString());

                String url = "http://" + instance.getAddress() + ":" + instance.getPort();
                List<String> tags = Arrays.asList(instance.getPayload().split(","));
                ProviderInfo providerInfo = new ProviderInfo(instance.getId(), url, tags);

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

    public String getProviders(String service, String group, String version, List<String> tags) throws Exception {
        String provider = Joiner.on(":").join(service, group, version);
        if (!providersCache.containsKey(provider) || providersCache.get(provider).isEmpty()) {
            return null;
        }

        List<ProviderInfo> providers = FilterLine.filter(providersCache.get(provider), tags);
        if (providers.isEmpty()) {
            return null;
        }
        return providers.get(0).getUrl();
    }

    private void watchResources() {
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(this::addProvider)
                .forChanges(this::updateProvider)
                .forDeletes(this::deleteProvider)
                .forInitialized(() -> { log.info("Resources Cache initialized"); })
                .build();
        resourcesCache.listenable().addListener(listener);
        resourcesCache.start();
    }

    private void addProvider(ChildData node) {
        System.out.println("\n\n=================== add new provider ============================");

        System.out.printf("Node created: [%s:%s]%n", node.getPath(), new String(node.getData()));
        if (providerDataEmpty(node)) {
            return;
        }

        String jsonValue = new String(node.getData(), StandardCharsets.UTF_8);
        JSONObject instance = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(instance.toString());

        String url = "http://" + instance.get("address") + ":" + instance.get("port");
        List<String> tags = Arrays.asList(instance.get("payload").toString().split(","));
        ProviderInfo providerInfo = new ProviderInfo(instance.get("id").toString(), url, tags);

        List<ProviderInfo> providerList = providersCache.getOrDefault(instance.get("name").toString(), new ArrayList<>());
        providerList.add(providerInfo);
        providersCache.put(instance.get("name").toString(), providerList);

        System.out.println("=================== add new provider end ============================\n\n");
    }

    private void updateProvider(ChildData oldNode, ChildData newNode) {
        System.out.printf("Node changed, Old: [%s: %s] New: [%s: %s]%n", oldNode.getPath(),
                new String(oldNode.getData()), newNode.getPath(), new String(newNode.getData()));

        if (providerDataEmpty(newNode)) {
            return;
        }

        String jsonValue = new String(newNode.getData(), StandardCharsets.UTF_8);
        JSONObject instance = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(instance.toString());

        String url = "http://" + instance.get("address") + ":" + instance.get("port");
        List<String> tags = Arrays.asList(instance.get("payload").toString().split(","));
        ProviderInfo providerInfo = new ProviderInfo(instance.get("id").toString(), url, tags);

        List<ProviderInfo> providerList = providersCache.getOrDefault(instance.get("name").toString(), new ArrayList<>());
        providerList.add(providerInfo);
        providersCache.put(instance.get("name").toString(), providerList);
    }

    private void deleteProvider(ChildData oldNode) {
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
