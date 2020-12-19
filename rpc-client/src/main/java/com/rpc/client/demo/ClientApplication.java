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
import com.rpc.core.demo.proxy.RpcByteBuddy;
import com.rpc.core.demo.proxy.RpcClient;
import com.rpc.demo.model.Order;
import com.rpc.demo.model.User;
import com.rpc.demo.service.OrderService;
import com.rpc.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lw1243925457
 */
@Slf4j
public class ClientApplication {

    public static void main(String[] args) {
        ParserConfig.getGlobalInstance().addAccept("com.rpc.demo.model.Order");
        ParserConfig.getGlobalInstance().addAccept("com.rpc.demo.model.User");

        RpcClient client = new RpcByteBuddy();

//        System.out.println("----------------- v1 ---------------------");
//        UserService userService = client.create(UserService.class, "http://localhost:8080/");
        System.out.println("----------------- v2 ---------------------");
        UserService userService = client.create(UserService.class, "http://localhost:8080/", "group2", "v2");
        User user = userService.findById(1);
        if (user == null) {
            log.info("Clint service invoke Error");
            return;
        }
        System.out.println("find user id=1 from server: " + user.getName());

        OrderService orderService = client.create(OrderService.class, "http://localhost:8080/");
        Order order = orderService.findById(1992129);
        if (order == null) {
            log.info("Clint service invoke Error");
            return;
        }
        System.out.println(String.format("find order name=%s, user=%d",order.getName(),order.getUserId()));
    }
}
