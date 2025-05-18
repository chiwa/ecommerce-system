package com.ecommerce.inventoryservice.camel;

import com.ecommerce.inventoryservice.event.OrderEvent;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventRoute extends RouteBuilder {

    private final InventoryRepository repository;

    public InventoryEventRoute(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void configure() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat(OrderEvent.class);
        jacksonDataFormat.setObjectMapper(mapper);

        from("kafka:order-events?brokers={{kafka.bootstrap-servers}}&groupId=inventory-group")
                .log("Received message from order-events: ${body}")
                .unmarshal(jacksonDataFormat)
                .process(exchange -> {
                    OrderEvent event = exchange.getIn().getBody(OrderEvent.class);
                    exchange.getContext().createProducerTemplate().sendBody("log:inventory-check?level=INFO",
                            "Checking inventory for product: " + event.getProductName() + " with quantity: " + event.getQuantity());

                    repository.findByProductName(event.getProductName())
                            .ifPresentOrElse(inventory -> {
                                if (inventory.getStock() >= event.getQuantity()) {
                                    inventory.setStock(inventory.getStock() - event.getQuantity());
                                    repository.save(inventory);
                                    event.setStatus("INVENTORY_RESERVED");
                                    exchange.getContext().createProducerTemplate().sendBody("log:inventory-check?level=INFO",
                                            "Inventory reserved for product: " + event.getProductName() + ", Remaining stock: " + inventory.getStock());
                                } else {
                                    event.setStatus("OUT_OF_STOCK");
                                    exchange.getContext().createProducerTemplate().sendBody("log:inventory-check?level=INFO",
                                            "Not enough stock for product: " + event.getProductName());
                                }
                                event.setAvailableStock(inventory.getStock());
                                exchange.getIn().setBody(event);
                            }, () -> {
                                event.setStatus("PRODUCT_NOT_FOUND");
                                exchange.getContext().createProducerTemplate().sendBody("log:inventory-check?level=INFO",
                                        "Product not found: " + event.getProductName());
                                exchange.getIn().setBody(event);
                            });
                })
                .log("Sending result to inventory-events: ${body}")
                .marshal(jacksonDataFormat)
                .to("kafka:inventory-events?brokers={{kafka.bootstrap-servers}}");
    }
}
