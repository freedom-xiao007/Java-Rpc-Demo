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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author lw1243925457
 */
public class ConsistentHashBalance extends AbstractLoadBalance {

    public static final String NAME = "consistent_hash_balance";

    private final ConcurrentMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    public String select(List<ProviderInfo> providers, String serviceName, String methodName) {
        String key = serviceName + "." + methodName;
        int providersHashCode = providers.hashCode();

        ConsistentHashSelector selector = selectors.get(key);
        if (selector == null || selector.getIdentityHashCode() != providersHashCode) {
            selectors.put(key, new ConsistentHashSelector(providers, methodName, providersHashCode));
            selector = selectors.get(key);
        }
        return selector.select(key);
    }
}
