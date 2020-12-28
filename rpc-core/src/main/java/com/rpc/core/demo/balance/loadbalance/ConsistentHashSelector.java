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

package com.rpc.core.demo.balance.loadbalance;

import com.rpc.core.demo.api.ProviderInfo;

import java.util.List;
import java.util.TreeMap;

/**
 * @author lw1243925457
 */
class ConsistentHashSelector {

    private final int providersHashCode;
    private final TreeMap<Long, String> virtualInvokers;
    private final int identityHashCode;
    private final int replicaNumber = 1024;

    ConsistentHashSelector(List<ProviderInfo> providers, String methodName, int providersHashCode) {
        this.providersHashCode = providersHashCode;
        this.virtualInvokers = new TreeMap<>();
        this.identityHashCode = providersHashCode;

        for (ProviderInfo provider: providers) {
            String address = provider.getUrl();
            for (int i = 0; i < replicaNumber / 4; i++) {
                byte[] digest = (address + i).getBytes();
                for (int h = 0; h < 4; h++) {
                    long m = hash(digest, h);
                    virtualInvokers.put(m, provider.getUrl());
                }
            }
        }
    }

    int getIdentityHashCode() {
        return identityHashCode;
    }

    String select(String key) {
        byte[] digest = key.getBytes();
        return virtualInvokers.ceilingEntry(hash(digest, 0)).getValue();
    }

    private long hash(byte[] digest, int number) {
        return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                | (digest[number * 4] & 0xFF))
                & 0xFFFFFFFFL;
    }
}
