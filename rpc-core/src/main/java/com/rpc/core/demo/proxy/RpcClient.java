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

import com.google.common.base.Joiner;
import com.rpc.core.demo.balance.loadbalance.WeightBalance;
import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lw1243925457
 */
public class RpcClient {

    private static String balanceAlgorithmName = WeightBalance.NAME;

    private ConcurrentHashMap<String, Object> proxyCache = new ConcurrentHashMap<>();

    public static void setBalanceAlgorithmName(String balanceAlgorithm) {
        balanceAlgorithmName = balanceAlgorithm;
    }

    public static String getBalanceAlgorithmName() {
        return balanceAlgorithmName;
    }

    private Object getProxy(String className) {
        return proxyCache.get(className);
    }

    private Boolean isExit(String className) {
        return proxyCache.containsKey(className);
    }

    private void add(String className, Object proxy) {
        proxyCache.put(className, proxy);
    }

    public <T> T create(Class<T> serviceClass) {
        String invoker = serviceClass.getName();
        if (!isExit(invoker)) {
            add(invoker, newProxy(serviceClass));
        }
        return (T) getProxy(invoker);
    }

    public <T> T create(Class<T> serviceClass, String group, String version) {
        String invoker = Joiner.on(":").join(serviceClass.getName(), group, version);
        if (!isExit(invoker)) {
            add(invoker, newProxy(serviceClass, group, version));
        }
        return (T) getProxy(invoker);
    }

    public <T> T create(Class<T> serviceClass, String group, String version, List<String> tags) {
        String invoker = Joiner.on(":").join(serviceClass.getName(), group, version, tags.toString());
        if (!isExit(invoker)) {
            add(invoker, newProxy(serviceClass, group, version, tags));
        }
        return (T) getProxy(invoker);
    }

    @SneakyThrows
    private <T> T newProxy(Class<T> serviceClass, String group, String version) {
        return (T) new ByteBuddy().subclass(Object.class)
                .implement(serviceClass)
                .intercept(InvocationHandlerAdapter.of(new RpcInvocationHandler(serviceClass, group, version)))
                .make()
                .load(RpcClient.class.getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    @SneakyThrows
    private <T> T newProxy(Class<T> serviceClass, String group, String version, List<String> tags) {
        return (T) new ByteBuddy().subclass(Object.class)
                .implement(serviceClass)
                .intercept(InvocationHandlerAdapter.of(new RpcInvocationHandler(serviceClass, group, version, tags)))
                .make()
                .load(RpcClient.class.getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    @SneakyThrows
    private <T> T newProxy(Class<T> serviceClass) {
        return (T) new ByteBuddy().subclass(Object.class)
                .implement(serviceClass)
                .intercept(InvocationHandlerAdapter.of(new RpcInvocationHandler(serviceClass)))
                .make()
                .load(RpcClient.class.getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }
}
