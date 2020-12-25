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
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author lw1243925457
 */
public class DiscoveryClientTest {

    @Test
    public void test() throws Exception {
        DiscoveryServer discoveryServer = new DiscoveryServer();
        discoveryServer.registerService("service", "group", "version", 8080);
        Thread.sleep(3000);

        DiscoveryClient discoveryClient = new DiscoveryClient();

        // add get provider of not exist
        assertNull(discoveryClient.getProviders("service1", "group", "version"));

        // add get provider
        String exceptValue = Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), 8080);
        assertEquals(discoveryClient.getProviders("service", "group", "version"), exceptValue);

        // server add new provider, test client add new provider
        discoveryServer.registerService("service1", "group", "version", 8080);
        Thread.sleep(3000);
        assertEquals(discoveryClient.getProviders("service1", "group", "version"), exceptValue);

        // server delete provider, test client delete provider
        discoveryServer.close();
        Thread.sleep(5000);
        assertNull(discoveryClient.getProviders("service1", "group", "version"));

        discoveryClient.close();
    }
}
