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

package com.rpc.core.demo.balance;

import com.rpc.core.demo.api.ProviderInfo;

import java.util.List;

/**
 * @author lw1243925457
 */
public interface LoadBalance {

    /**
     * 从当前Provider列表中，通过负载均衡，返回其中一个Provider的请求地址
     * @param providers provider list
     * @param serviceName service name
     * @param methodName method name
     * @return provider host url
     */
    String select(List<ProviderInfo> providers, String serviceName, String methodName);
}
