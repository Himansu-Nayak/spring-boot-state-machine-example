package com.example.orderservice;

import com.example.orderservice.entity.Order;
import com.example.orderservice.enums.OrderEvents;
import com.example.orderservice.enums.OrderStates;
import com.example.orderservice.service.OrderService;
import lombok.extern.java.Log;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Log
@Component
public class Runner implements ApplicationRunner {

    private final OrderService orderService;

    Runner(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Order order = this.orderService.create(new Date());
        StateMachine<OrderStates, OrderEvents> paymentStateMachine = orderService.pay(order.getId(), UUID.randomUUID()
                .toString());
        log.info("after calling pay(): " + paymentStateMachine.getState().getId().name());
        log.info("order: " + orderService.byId(order.getId()));

        StateMachine<OrderStates, OrderEvents> fulfilledStateMachine = orderService.fulfill(order.getId());
        log.info("after calling fulfill(): " + fulfilledStateMachine.getState().getId().name());
        log.info("order: " + orderService.byId(order.getId()));


    }

}

