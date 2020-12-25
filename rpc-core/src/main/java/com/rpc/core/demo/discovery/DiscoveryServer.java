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

import com.rpc.core.demo.api.ServiceProviderDesc;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lw1243925457
 */
public class DiscoveryServer extends ZookeeperClient {

    List<ServiceDiscovery<ServiceProviderDesc>> discoveryList = new ArrayList<>();

    public DiscoveryServer() throws Exception {
    }

    public void registerService(String service, String group, String version, int port) throws Exception {
        ServiceProviderDesc serviceProviderDesc = ServiceProviderDesc.builder()
                .serviceClass(service)
                .host(InetAddress.getLocalHost().getHostAddress())
                .port(port)
                .group(group)
                .version(version)
                .build();

        ServiceInstance<ServiceProviderDesc> instance = ServiceInstance.<ServiceProviderDesc>builder()
                .name(service)
                .port(port)
                .address(InetAddress.getLocalHost().getHostAddress())
                .payload(serviceProviderDesc)
                .build();

        JsonInstanceSerializer<ServiceProviderDesc> serializer = new JsonInstanceSerializer<>(ServiceProviderDesc.class);

        ServiceDiscovery<ServiceProviderDesc> discovery = ServiceDiscoveryBuilder.builder(ServiceProviderDesc.class)
                .client(client)
                .basePath(REGISTER_ROOT_PATH)
                .serializer(serializer)
                .thisInstance(instance)
                .build();
        discovery.start();

        discoveryList.add(discovery);
    }

    public void close() throws IOException {
        for (ServiceDiscovery<ServiceProviderDesc> discovery: discoveryList) {
            discovery.close();
        }
        client.close();
    }
}
