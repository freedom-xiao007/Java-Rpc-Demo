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
import java.util.Random;

/**
 * @author lw1243925457
 */
public class WeightBalance extends AbstractLoadBalance {

    public static final String NAME = "weight_balance";

    @Override
    public String select(List<ProviderInfo> providers) {
        int totalWeight = 0;
        for (ProviderInfo provider: providers) {
            totalWeight += provider.getWeight();
        }

        int random = new Random().nextInt(totalWeight);
        System.out.printf("provider amount: %s, random : %d\n", providers.size(), random);
        for (ProviderInfo provider: providers) {
            random -= provider.getWeight();
            if (random <= 0) {
                return provider.getUrl();
            }
        }
        return providers.get(providers.size() - 1).getUrl();
    }
}
