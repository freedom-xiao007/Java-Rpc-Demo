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

package com.rpc.core.demo.netty.server;

import com.alibaba.fastjson.JSON;
import com.rpc.core.demo.api.RpcResponse;
import com.rpc.core.demo.filter.server.BackListFilter;
import com.rpc.core.demo.netty.common.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * @author lw1243925457
 */
public class BackListFilterHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = socket.getAddress().getHostAddress();
        System.out.println(clientIp);

        if (BackListFilter.checkAddress(clientIp)) {
            RpcResponse response = new RpcResponse();
            response.setStatus(false);
            response.setException(new Exception("back list"));

            RpcProtocol message = new RpcProtocol();
            String requestJson = JSON.toJSONString(response);
            message.setLen(requestJson.getBytes(CharsetUtil.UTF_8).length);
            message.setContent(requestJson.getBytes(CharsetUtil.UTF_8));

            ctx.channel().writeAndFlush(message).sync();
        }

        ctx.fireChannelRead(msg);
    }
}
