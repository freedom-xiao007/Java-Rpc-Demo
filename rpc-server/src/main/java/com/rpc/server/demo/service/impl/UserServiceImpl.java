package com.rpc.server.demo.service.impl;

import com.rpc.core.demo.annotation.ProviderService;
import com.rpc.demo.model.User;
import com.rpc.demo.service.UserService;

/**
 * @author lw
 */
@ProviderService(service = "com.rpc.demo.service.UserService", weight = 2)
public class UserServiceImpl implements UserService {

    @Override
    public User findById(Integer id) {
        return new User(id, "RPC weight 2");
    }
}
