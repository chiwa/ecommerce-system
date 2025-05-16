package com.ecommerce.inventoryservice.camel;

import com.ecommerce.inventoryservice.event.OrderEvent;
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
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat(OrderEvent.class);
        jacksonDataFormat.setObjectMapper(mapper);

        from("kafka:inventory-events?brokers={{kafka.bootstrap-servers}}")
                .log("Received inventory result: ${body}")
                .unmarshal(jacksonDataFormat)
                .process(exchange -> {
                    OrderEvent event = exchange.getIn().getBody(OrderEvent.class);
                    log.info("Updating status for Order ID: {}", event.getId());

                    orderRepository.findById(event.getId()).ifPresent(order -> {
                        order.setStatus(event.getStatus());
                        orderRepository.save(order);
                        log.info("Order ID {} updated to status {}", order.getId(), order.getStatus());
                    });
                });
    }
}
