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
import com.rpc.core.demo.api.ServiceProviderDesc;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lw1243925457
 */
@Slf4j
public class DiscoveryClient extends ZookeeperClient {

    /**
     * group -> version -> provider cache -> provider instance
     */
    private Map<String, Map<String, Map<String, Map<String, ServiceInstance<ServiceProviderDesc>>>>> providersCache = new HashMap<>();

    private final ConcurrentHashMap<String, ServiceProvider<ServiceProviderDesc>> cache = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    private final List<Closeable> closeableList = new ArrayList<>();

    private final ServiceDiscovery<ServiceProviderDesc> serviceDiscovery;
    private final CuratorCache resourcesCache;

    public DiscoveryClient() throws Exception {
        JsonInstanceSerializer<ServiceProviderDesc> serializer = new JsonInstanceSerializer<>(ServiceProviderDesc.class);
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceProviderDesc.class)
                .client(client)
                .basePath(REGISTER_ROOT_PATH)
                .serializer(serializer)
                .build();
        serviceDiscovery.start();

        getAllProviders();

        this.resourcesCache = CuratorCache.build(this.client, "/");
        watchResources();
    }

    private void getAllProviders() throws Exception {
        ServiceProvider<ServiceProviderDesc> providers = serviceDiscovery.serviceProviderBuilder()
                .serviceName("")
                .build();
        providers.start();

        Collection<ServiceInstance<ServiceProviderDesc>> instances = providers.getAllInstances();
        for (ServiceInstance<ServiceProviderDesc> instance: instances) {
            System.out.println(instance.toString());
            ServiceProviderDesc serviceProviderDesc = instance.getPayload();
            String group = serviceProviderDesc.getGroup();
            String version = serviceProviderDesc.getVersion();

            groupMap = providersCache.getOrDefault(group, new HashMap<>());
        }
    }

    public ServiceInstance<ServiceProviderDesc> getProviders(String service, String group, String version) throws Exception {
        String key = Joiner.on(":").join(Arrays.asList(service, group, version));
        ServiceProvider<ServiceProviderDesc> provider = cache.get(key);

        if (provider == null) {
            synchronized (lock) {
                provider = cache.get(key);
                if (provider == null) {
                    provider = serviceDiscovery.serviceProviderBuilder()
                            .serviceName(service)
                            .build();

                    provider.start();
                    Collection<ServiceInstance<ServiceProviderDesc>> instances = provider.getAllInstances();
                    for (ServiceInstance<ServiceProviderDesc> instance: instances) {
                        System.out.println(instance.toString());
                    }

                    closeableList.add(provider);
                    cache.put(key, provider);
                }
            }
        }

        return provider.getInstance();
    }

    public synchronized void close() {
        for (Closeable closeable: closeableList) {
            CloseableUtils.closeQuietly(closeable);
        }
    }

    public void listener() {
        CuratorCache cache = CuratorCache.build(this.client, "/");
        CuratorCacheListener cacheListener = new CuratorCacheListener() {
            @Override
            public void event(Type type, ChildData childData, ChildData childData1) {
                System.out.println("Type:: " + type);
                System.out.println(childData.toString());
                System.out.println(childData1.toString());
            }
        };
        cache.listenable().addListener(cacheListener);
        cache.start();
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
        System.out.printf("Node created: [%s:%s]%n", node.getPath(), new String(node.getData()));
        String jsonValue = new String(node.getData(), StandardCharsets.UTF_8);
        JSONObject obj = (JSONObject) JSONObject.parse(jsonValue);
        System.out.println(obj.toString());
    }

    private void updateProvider(ChildData oldNode, ChildData newNode) {
        System.out.printf("Node changed, Old: [%s: %s] New: [%s: %s]%n", oldNode.getPath(),
                new String(oldNode.getData()), newNode.getPath(), new String(newNode.getData()));
    }

    private void deleteProvider(ChildData oldNode) {
        System.out.printf("Node deleted, Old value: [%s: %s]%n", oldNode.getPath(), new String(oldNode.getData()));
    }

    public static void main(String[] args) throws Exception {
        DiscoveryClient discoveryClient = new DiscoveryClient();
//        ServiceInstance<ServiceProviderDesc> provider = discoveryClient.getProviders("com.rpc.demo.service.UserService", "group2", "v2");
//        System.out.println(provider.toString());
//
//        ServiceProviderDesc serviceProviderDesc = provider.getPayload();
//        System.out.println(serviceProviderDesc.toString());

//        discoveryClient.listener();

        while (true) {
            Thread.sleep(10);
        }
    }
}
