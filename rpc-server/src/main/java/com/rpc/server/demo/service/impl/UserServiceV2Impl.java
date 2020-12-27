package com.rpc.server.demo.service.impl;

import com.rpc.core.demo.annotation.ProviderService;
import com.rpc.demo.model.User;
import com.rpc.demo.service.UserService;

/**
 * @author lw
 */
@ProviderService(service = "com.rpc.demo.service.UserService", group = "group2", version = "v2", tags = "tag2")
public class UserServiceV2Impl implements UserService {

    @Override
    public User findById(Integer id) {
        return new User(id, "RPC group2 v2");
    }
}
