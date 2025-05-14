package com.ecommerce.orderservice.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

@Component
public class OrderEventRouter extends RouteBuilder {
    private final JacksonDataFormat jacksonDataFormat;

    public OrderEventRouter(JacksonDataFormat jacksonDataFormat) {
        this.jacksonDataFormat = jacksonDataFormat;
    }

    @Override
    public void configure() {
        from("direct:place-order")
                .routeId("place-order-route")
                .log("Received Event: ${body}")
                .marshal(jacksonDataFormat)  // 👈 ใช้ DataFormat ที่กำหนดเอง
                .to("kafka:order-events?brokers=kafka:9093")
                .log("Message sent to Kafka: ${body}");
    }
}