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

package com.rpc.core.demo.proxy;

import com.alibaba.fastjson.JSON;
import com.rpc.core.demo.api.RpcRequest;
import com.rpc.core.demo.api.RpcResponse;
import com.rpc.core.demo.discovery.DiscoveryClient;
import com.rpc.core.demo.netty.client.RpcNettyClientSync;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于jdk、cglib、buddy
 *
 * @author lw1243925457
 */
@Slf4j
public class RpcInvocationHandler implements InvocationHandler, MethodInterceptor {

    private final Class<?> serviceClass;
    private final String group;
    private final String version;
    private final DiscoveryClient discoveryClient = DiscoveryClient.getInstance();
    private final List<String> tags = new ArrayList<>();

    <T> RpcInvocationHandler(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
        this.group = "default";
        this.version = "default";
        System.out.println("Client Service Class Reflect: " + group + ":" + version);
    }

    <T> RpcInvocationHandler(Class<T> serviceClass, String group, String version) {
        this.serviceClass = serviceClass;
        this.group = group;
        this.version = version;
        System.out.println("Client Service Class Reflect: " + group + ":" + version);
    }

    <T> RpcInvocationHandler(Class<T> serviceClass, String group, String version, List<String> tags) {
        this.serviceClass = serviceClass;
        this.group = group;
        this.version = version;
        this.tags.addAll(tags);
        System.out.println("Client Service Class Reflect: " + group + ":" + version);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return process(serviceClass, method, args);
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) {
        return process(serviceClass, method, args);
    }

    /**
     * 发送请求到服务端
     * 获取结果后序列号成对象，返回
     * @param service service name
     * @param method service method
     * @param params method params
     * @return object
     */
    private Object process(Class<?> service, Method method, Object[] params) {
        log.info("Client proxy instance method invoke");

        // 自定义了Rpc请求的结构 RpcRequest,放入接口名称、方法名、参数
        log.info("Build Rpc request");
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceClass(service.getName());
        rpcRequest.setMethod(method.getName());
        rpcRequest.setArgv(params);
        rpcRequest.setGroup(group);
        rpcRequest.setVersion(version);

        String url = null;
        try {
            url = discoveryClient.getProviders(service.getName(), group, version, tags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (url == null) {
            System.out.println("\nCan't find provider\n");
            return null;
        }

        // 客户端使用的 netty，发送请求到服务端，拿到结果（自定义结构：rpcfxResponse)
        log.info("Client send request to Server");
        RpcResponse rpcResponse;
        try {
            rpcResponse = RpcNettyClientSync.getInstance().getResponse(rpcRequest, url);
        } catch (InterruptedException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        log.info("Client receive response Object");
        assert rpcResponse != null;
        if (!rpcResponse.getStatus()) {
            log.info("Client receive exception");
            rpcResponse.getException().printStackTrace();
            return null;
        }

        // 序列化成对象返回
        log.info("Response:: " + rpcResponse.getResult());
        return JSON.parse(rpcResponse.getResult().toString());
    }
}
