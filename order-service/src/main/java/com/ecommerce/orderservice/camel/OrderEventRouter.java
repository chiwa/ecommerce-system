package com.ecommerce.orderservice.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderEventRouter extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:place-order")
                .routeId("place-order-route")
                .log("Received Event: ${body}");
    }
}