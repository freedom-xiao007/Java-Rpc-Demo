package com.rpc.server.demo.service.impl;

import com.rpc.core.demo.annotation.ProviderService;
import com.rpc.core.demo.exception.CustomException;
import com.rpc.demo.model.Order;
import com.rpc.demo.service.OrderService;

/**
 * @author lw
 */
@ProviderService(service = "com.rpc.demo.service.OrderService")
public class OrderServiceImpl implements OrderService {

    @Override
    public Order findById(Integer id) {
        return new Order(1, "RPC", 1);
    }

    @Override
    public Order findError() {
        throw new CustomException("Custom exception");
    }
}
