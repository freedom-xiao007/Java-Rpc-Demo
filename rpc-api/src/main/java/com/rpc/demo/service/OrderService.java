package com.rpc.demo.service;


import com.rpc.demo.model.Order;

/**
 * @author lw
 */
public interface OrderService {

    /**
     * find by id
     * @param id id
     * @return order
     */
    Order findById(Integer id);

    /**
     * return exception
     * @return exception
     */
    Order findError();
}
