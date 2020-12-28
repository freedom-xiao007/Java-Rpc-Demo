package com.rpc.server.demo.service.impl2;

import com.rpc.core.demo.annotation.ProviderService;
import com.rpc.demo.model.User;
import com.rpc.demo.service.UserService;

/**
 * @author lw
 */
@ProviderService(service = "com.rpc.demo.service.UserService", weight = 8)
public class UserServiceImpl implements UserService {

    @Override
    public User findById(Integer id) {
        return new User(id, "RPC weight8");
    }
}
