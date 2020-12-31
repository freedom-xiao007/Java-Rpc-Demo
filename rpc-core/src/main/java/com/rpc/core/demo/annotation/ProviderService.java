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

package com.rpc.core.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC provider service 初始化注解
 *
 * group,version,targs 都有默认值，是为了兼容以前的版本
 *
 * @author lw1243925457
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProviderService {

    /**
     * 对应 API 接口名称
     * @return API service
     */
    String service();

    /**
     * 分组
     * @return group
     */
    String group() default "default";

    /**
     * version
     * @return version
     */
    String version() default "default";

    /**
     * tags:用于简单路由
     * 多个标签使用逗号分隔
     * @return tags
     */
    String tags() default "";

    /**
     * 权重：用于加权负载均衡
     * @return
     */
    int weight() default 1;
}
