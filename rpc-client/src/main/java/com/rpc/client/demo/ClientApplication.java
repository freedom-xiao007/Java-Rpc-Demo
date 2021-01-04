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

package com.rpc.client.demo;

import com.alibaba.fastjson.parser.ParserConfig;
import com.rpc.core.demo.balance.loadbalance.ConsistentHashBalance;
import com.rpc.core.demo.filter.client.Retry;
import com.rpc.core.demo.proxy.RpcClient;
import com.rpc.demo.model.Order;
import com.rpc.demo.model.User;
import com.rpc.demo.service.OrderService;
import com.rpc.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lw1243925457
 */
@Slf4j
public class ClientApplication {

    public static void main(String[] args) {
        // fastjson auto setting
        ParserConfig.getGlobalInstance().addAccept("com.rpc.demo.model.Order");
        ParserConfig.getGlobalInstance().addAccept("com.rpc.demo.model.User");

        // set retry time
        Retry.setRetryLimit(3);

        RpcClient client = new RpcClient();
        RpcClient.setBalanceAlgorithmName(ConsistentHashBalance.NAME);

        UserService userService = client.create(UserService.class, "group2", "v2");
        User user = userService.findById(1);
        if (user == null) {
            log.info("Clint service invoke Error");
        } else {
            System.out.println("\n\nuser1 :: find user id=1 from server: " + user.getName());
        }

        OrderService orderService = client.create(OrderService.class);
        Order order = orderService.findById(1992129);
        if (order == null) {
            log.info("Clint service invoke Error");
        } else {
            System.out.println("\n\n" + String.format("find order name=%s, user=%d", order.getName(), order.getUserId()));
        }

        UserService userService2 = client.create(UserService.class, "group2", "v2", Arrays.asList("tag1", "tag2"));
        User user2 = userService2.findById(1);
        if (user2 == null) {
            log.info("Clint service invoke Error");
        } else {
            System.out.println("\n\nuser2 :: find user id=1 from server: " + user2.getName());
        }

        UserService userService3 = client.create(UserService.class, "group2", "v2", Arrays.asList("tag3", "tag4"));
        User user3 = userService3.findById(1);
        if (user3 == null) {
            log.info("Clint service invoke Error");
        } else {
            System.out.println("\n\nuser3 :: find user id=1 from server: " + user3.getName());
        }

        // test load balance
        System.out.println("\n=============== test load balance =======================\n");
        UserService userService4 = client.create(UserService.class);
        List<User> userList = new ArrayList<>(10);
        for(int i = 0; i < 10; i++) {
            userList.add(userService4.findById(1));
        }
        for (User item: userList) {
            System.out.println(item);
        }
    }
}
