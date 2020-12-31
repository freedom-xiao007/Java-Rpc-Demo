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

import com.google.common.base.Joiner;
import com.rpc.core.demo.api.ProviderInfo;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务发现服务器：用于注册Provider
 *
 * @author lw1243925457
 */
public class DiscoveryServer extends ZookeeperClient {

    private List<ServiceDiscovery<ProviderInfo>> discoveryList = new ArrayList<>();

    public DiscoveryServer() {
    }

    /**
     * 生成Provider的相关信息，注册到ZK中
     * @param service Service impl name
     * @param group group
     * @param version version
     * @param port service listen port
     * @param tags route tags
     * @param weight load balance weight
     * @throws Exception exception
     */
    public void registerService(String service, String group, String version, int port, List<String> tags,
                                int weight) throws Exception {
        ProviderInfo provider = new ProviderInfo(null, null, tags, weight);

        ServiceInstance<ProviderInfo> instance = ServiceInstance.<ProviderInfo>builder()
                .name(Joiner.on(":").join(service, group, version))
                .port(port)
                .address(InetAddress.getLocalHost().getHostAddress())
                .payload(provider)
                .build();

        JsonInstanceSerializer<ProviderInfo> serializer = new JsonInstanceSerializer<>(ProviderInfo.class);
        ServiceDiscovery<ProviderInfo> discovery = ServiceDiscoveryBuilder.builder(ProviderInfo.class)
                .client(client)
                .basePath(REGISTER_ROOT_PATH)
                .thisInstance(instance)
                .serializer(serializer)
                .build();
        discovery.start();

        discoveryList.add(discovery);
    }

    public void close() throws IOException {
        for (ServiceDiscovery<ProviderInfo> discovery: discoveryList) {
            discovery.close();
        }
        client.close();
    }
}
