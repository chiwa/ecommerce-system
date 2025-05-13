package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.event.PlaceOrderEvent;
import com.ecommerce.orderservice.publisher.OrderEventPublisher;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderController(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        order.setCreateDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        PlaceOrderEvent event = new PlaceOrderEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getProductName(),
                savedOrder.getQuantity(),
                savedOrder.getPrice(),
                savedOrder.getCreateDate()
        );

        eventPublisher.publish(event);  // ส่ง Event

        return savedOrder;
    }
}
