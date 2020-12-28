package com.rpc.server.demo.service.impl2;

import com.rpc.core.demo.annotation.ProviderService;
import com.rpc.core.demo.exception.CustomException;
import com.rpc.demo.model.Order;
import com.rpc.demo.service.OrderService;

/**
 * @author lw
 */
@ProviderService(service = "com.rpc.demo.service.OrderService", tags = "tag2", weight = 8)
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
