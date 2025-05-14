package com.ecommerce.orderservice.publisher;

import com.ecommerce.orderservice.event.PlaceOrderEvent;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final ProducerTemplate producerTemplate;

    @Autowired
    public OrderEventPublisher(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public void publish(PlaceOrderEvent event) {
        producerTemplate.sendBody("direct:place-order", event);
    }
}