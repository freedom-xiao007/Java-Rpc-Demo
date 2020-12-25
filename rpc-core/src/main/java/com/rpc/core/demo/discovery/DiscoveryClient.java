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
import com.rpc.core.demo.api.ServiceProviderDesc;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author lw1243925457
 */
@Slf4j
public class DiscoveryClient extends ZookeeperClient {

    /**
     * group -> version -> provider cache -> provider instance
     */
    private Map<String, Map<String, Map<String, List<ServiceProviderDesc>>>> providersCache = new HashMap<>();

    private final ServiceDiscovery<ServiceProviderDesc> serviceDiscovery;
    private final CuratorCache resourcesCache;

    public DiscoveryClient() {
        JsonInstanceSerializer<ServiceProviderDesc> serializer = new JsonInstanceSerializer<>(ServiceProviderDesc.class);
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceProviderDesc.class)
                .client(client)
                .basePath(REGISTER_ROOT_PATH)
                .serializer(serializer)
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
        ServiceProvider<ServiceProviderDesc> providers = serviceDiscovery.serviceProviderBuilder()
                .serviceName("")
                .build();
        providers.start();

        Collection<ServiceInstance<ServiceProviderDesc>> instances = providers.getAllInstances();
        for (ServiceInstance<ServiceProviderDesc> instance: instances) {
            System.out.println(instance.toString());
            ServiceProviderDesc serviceProviderDesc = instance.getPayload();
            serviceProviderDesc.setId(instance.getId());

            addToCache(serviceProviderDesc);

            System.out.println("add provider: " + instance.toString());
        }

        System.out.println("======================= init : get all provider end\n\n");
    }

    public void addToCache(ServiceProviderDesc serviceProviderDesc) {
        String group = serviceProviderDesc.getGroup();
        String version = serviceProviderDesc.getVersion();
        String provider = Joiner.on(":").join(serviceProviderDesc.getServiceClass(), group, version);

        Map<String, Map<String, List<ServiceProviderDesc>>> groupMap = providersCache.getOrDefault(group, new HashMap<>());
        Map<String, List<ServiceProviderDesc>> versionMap = groupMap.getOrDefault(version, new HashMap<>());
        List<ServiceProviderDesc> instanceList = versionMap.getOrDefault(provider, new ArrayList<>());

        instanceList.add(serviceProviderDesc);

        versionMap.put(provider, instanceList);
        groupMap.put(version, versionMap);
        providersCache.put(group, groupMap);
    }

    public String getProviders(String service, String group, String version) throws Exception {
        String key = Joiner.on(":").join(service, group, version);
        if (!providersCache.containsKey(group) || !providersCache.get(group).containsKey(version)) {
            return null;
        }
        List<ServiceProviderDesc> instanceList = providersCache.get(group).get(version).get(key);
        if (instanceList == null || instanceList.isEmpty()) {
            return null;
        }
        return instanceList.get(0).getHost() + ":" + instanceList.get(0).getPort();
    }

    private void watchResources() {
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(node -> addProvider(node))
                .forChanges((oldNode, node) -> updateProvider(oldNode, node))
                .forDeletes(oldNode -> deleteProvider(oldNode))
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
        JSONObject jsonObject = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(jsonObject.toString());

        ServiceProviderDesc serviceProviderDesc = JSON.parseObject(jsonObject.getString("payload"), ServiceProviderDesc.class);
        serviceProviderDesc.setId(jsonObject.getString("id"));
        System.out.println(serviceProviderDesc.toString());

        addToCache(serviceProviderDesc);

        System.out.println("=================== add new provider end ============================\n\n");
    }

    private void updateProvider(ChildData oldNode, ChildData newNode) {
        System.out.printf("Node changed, Old: [%s: %s] New: [%s: %s]%n", oldNode.getPath(),
                new String(oldNode.getData()), newNode.getPath(), new String(newNode.getData()));
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

        ServiceProviderDesc serviceProviderDesc = JSON.parseObject(instance.getString("payload"), ServiceProviderDesc.class);
        System.out.println(serviceProviderDesc.toString());

        String group = serviceProviderDesc.getGroup();
        String version = serviceProviderDesc.getVersion();
        String provider = Joiner.on(":").join(serviceProviderDesc.getServiceClass(), group, version);

        deleteCache(provider, group, version, instance.getString("id"));

        System.out.println("=================== delete provider end ============================\n\n");
    }

    private void deleteCache(String provider, String group, String version, String id) {
        if (!providersCache.containsKey(group) || !providersCache.get(group).containsKey(version)) {
            return;
        }
        List<ServiceProviderDesc> instanceList = providersCache.get(group).get(version).get(provider);
        if (instanceList == null || instanceList.isEmpty()) {
            return;
        }

        List<ServiceProviderDesc> duplicate = new ArrayList<>(instanceList);
        int removeIndex = -1;
        for (int i = 0; i < instanceList.size(); i++) {
            if (id.equals(instanceList.get(i).getId())) {
                removeIndex = i;
                break;
            }
        }

        if (removeIndex != -1) {
            duplicate.remove(removeIndex);
        }

        providersCache.get(group).get(version).put(provider, duplicate);
        System.out.println("delete provider: " + provider);
    }

    private boolean providerDataEmpty(ChildData node) {
        return node.getData().length == 0;
    }

    public synchronized void close() {
        client.close();
    }
}
