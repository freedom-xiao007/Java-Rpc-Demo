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

package com.rpc.core.demo.filter;

import com.rpc.core.demo.api.ProviderInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw1243925457
 */
public class FilterLine {

    private static boolean isInit = false;
    private static List<RpcFilter> rpcFilters = new ArrayList<>();

    private static void init() {
        addFilter(new TagFilter());
    }

    public static void addFilter(RpcFilter filter) {
        rpcFilters.add(filter);
    }

    public static List<ProviderInfo> filter(List<ProviderInfo> providers, List<String> tags) {
        if (!isInit) {
            init();
            isInit = true;
        }

        List<ProviderInfo> filterResult = new ArrayList<>(providers);

        for (RpcFilter filter: rpcFilters) {
            filterResult = filter.filter(filterResult, tags);
        }

        System.out.printf("\n%s filter to %s\n", providers, filterResult);
        return filterResult;
    }
}
