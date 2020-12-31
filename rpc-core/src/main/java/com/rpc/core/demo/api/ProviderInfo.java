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

package com.rpc.core.demo.api;

import lombok.Data;

import java.util.List;

/**
 * @author lw1243925457
 */
@Data
public class ProviderInfo {

    /**
     * Provider ID：ZK注册后会生成一个ID
     * Client 获取Provider列表时，将此ID设置为获取的ZK生成的ID
     */
    String id;

    /**
     * Provider对应的后端服务器地址
     */
    String url;

    /**
     * 标签：用于简单路由
     */
    List<String> tags;

    /**
     * 权重：用于加权负载均衡
     */
    Integer weight;

    public ProviderInfo() {}

    public ProviderInfo(String id, String url, List<String> tags, int weight) {
        this.id = id;
        this.url = url;
        this.tags = tags;
        this.weight = weight;
    }
}
