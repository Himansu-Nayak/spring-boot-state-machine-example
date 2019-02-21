package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.enums.OrderEvents;
import com.example.orderservice.enums.OrderStates;
import com.example.orderservice.repos.OrderRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class OrderService {

    private static final String ORDER_ID_HEADER = "orderId";
    private final OrderRepository orderRepository;
    private final StateMachineFactory<OrderStates, OrderEvents> factory;

    OrderService(OrderRepository orderRepository, StateMachineFactory<OrderStates, OrderEvents> factory) {
        this.orderRepository = orderRepository;
        this.factory = factory;
    }

    public Order create(Date when) {
        return this.orderRepository.save(new Order(when, OrderStates.SUBMITTED));
    }

    public Order byId(Long id) {
        return this.orderRepository.findById(id).orElse(null);
    }

    public StateMachine<OrderStates, OrderEvents> fulfill(Long orderId) {
        StateMachine<OrderStates, OrderEvents> sm = this.build(orderId);
        Message<OrderEvents> fulfillMessage = MessageBuilder.withPayload(OrderEvents.FULFILL)
                .setHeader(ORDER_ID_HEADER, orderId)
                .build();
        sm.sendEvent(fulfillMessage);
        return sm;
    }

    public StateMachine<OrderStates, OrderEvents> pay(Long orderId, String paymentConfirmationNumber) {
        StateMachine<OrderStates, OrderEvents> sm = this.build(orderId);

        Message<OrderEvents> paymentMessage = MessageBuilder.withPayload(OrderEvents.PAY)
                .setHeader(ORDER_ID_HEADER, orderId)
                .setHeader("paymentConfirmationNumber", paymentConfirmationNumber)
                .build();

        sm.sendEvent(paymentMessage);
        return sm;
    }

    private StateMachine<OrderStates, OrderEvents> build(Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElse(null);
        String orderIdKey = Long.toString(order.getId());
        StateMachine<OrderStates, OrderEvents> sm = this.factory.getStateMachine(orderIdKey);
        sm.stop();
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {

                    sma.addStateMachineInterceptor(new StateMachineInterceptorAdapter<OrderStates, OrderEvents>() {

                        @Override
                        public void preStateChange(State<OrderStates, OrderEvents> state, Message<OrderEvents> message,
                                                   Transition<OrderStates, OrderEvents> transition,
                                                   StateMachine<OrderStates, OrderEvents> stateMachine) {

                            Optional.ofNullable(message).ifPresent(msg ->
                                    Optional.ofNullable(Long.class.cast(msg.getHeaders().getOrDefault(ORDER_ID_HEADER, -1L)))
                                            .ifPresent(orderId1 -> {
                                                Order order1 = orderRepository.findById(orderId1).orElse(null);
                                                order1.setOrderState(state.getId());
                                                orderRepository.save(order1);
                                            }));
                        }
                    });
                    sma.resetStateMachine(new DefaultStateMachineContext<>(order.getOrderState(), null, null, null));
                });
        sm.start();
        return sm;
    }
}
