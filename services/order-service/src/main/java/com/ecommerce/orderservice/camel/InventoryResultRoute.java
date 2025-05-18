package com.ecommerce.orderservice.camel;

import com.ecommerce.orderservice.event.InventoryEvent;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

@Component
public class InventoryResultRoute extends RouteBuilder {

    private final OrderRepository orderRepository;

    public InventoryResultRoute(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void configure() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat(InventoryEvent.class);
        jacksonDataFormat.setObjectMapper(mapper);

        from("kafka:inventory-events?brokers={{kafka.bootstrap-servers}}&groupId=order-group")
                .log("Received inventory result: ${body}")
                .unmarshal(jacksonDataFormat)
                .process(exchange -> {
                    InventoryEvent event = exchange.getIn().getBody(InventoryEvent.class);
                    log.info("Updating status for Order ID: {}", event.getOrderId());

                    orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                        order.setStatus(event.getStatus());
                        orderRepository.save(order);
                        log.info("Order ID {} updated to status {}", order.getId(), order.getStatus());
                    });
                });
    }
}
