package com.ecommerce.notificationservice.camel;

import com.ecommerce.notificationservice.event.InventoryEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InventoryEventRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat(InventoryEvent.class);
        jacksonDataFormat.setObjectMapper(mapper);

        /**
         * 	•	รับข้อความจาก Kafka Topic inventory-events
         * 	•	แปลง JSON เป็น InventoryEvent ด้วย DataFormat ที่เตรียมไว้
         * 	•	ตรวจสอบสถานะ
         * 	•	ถ้าสินค้า OUT_OF_STOCK → ไป direct:outOfStockNotification
         * 	•	ถ้า สถานะอื่น → ไป direct:sendEmailToCustomer
         */
        from("kafka:inventory-events?brokers=kafka:9093")
                .unmarshal(jacksonDataFormat)
                .choice()
                .when(simple("${body.status} == 'OUT_OF_STOCK'"))
                .to("direct:outOfStockNotification")
                .otherwise()
                .to("direct:sendEmailToCustomer");


        /**
         * 	•	Log ข้อมูลสินค้า
         * 	•	ส่งต่อไปยัง
         * 	•	ส่งอีเมล (จริงๆ แค่ log ไว้ใน console)
         * 	•	ส่งแจ้งเตือนผ่าน Line Notify
         */
        from("direct:outOfStockNotification")
                .log("สินค้าไม่พอ: ${body.productName}")
                .to("direct:sendEmailToCustomer")
                .to("direct:sendLineNotification");

        /**
         * 	•	จำลองการส่งอีเมลด้วยการ log
         * 	•	แสดงรายละเอียดของ Event ทั้งหมด
         */
        from("direct:sendEmailToCustomer")
                .process(exchange -> {
                    InventoryEvent event = exchange.getIn().getBody(InventoryEvent.class);
                    String content = "Email >>> เรียนลูกค้ารายการสั่งซื้อของท่าน มีรายละเอียดดังนี้\n Order :  "
                            + event.getOrderId() + "\n สินค้า : "
                            + event.getProductName() + "\n มีสถานะ : "
                            + event.getStatus();
                    log.info(content);
                    log.info(event.toString());
                });

        /**
         * 	•	เตรียม HTTP Request สำหรับส่งข้อความไปยัง Line
         * 	•	กำหนด Method เป็น POST
         * 	•	ใส่ Header Authorization ด้วย Access Token
         * 	•	เตรียม Body ที่จะส่งข้อความไปยัง User
         */
        from("direct:sendLineNotification")
                .process(exchange -> {
                    InventoryEvent event = exchange.getIn().getBody(InventoryEvent.class);
                    String message = String.format("ด่วน!!!! สินค้า %s ไม่เพียงพอ\nลูกค้าต้องการ: %d\nแต่มีสินค้าแค่ : %d\nกรุณาเติมสินค้า",
                            event.getProductName(),
                            event.getQuantity(),
                            event.getAvailableStock()
                    );

                    Map<String, Object> payload = new HashMap<>();
                    List<Map<String, String>> messages = new ArrayList<>();
                    Map<String, String> msg = new HashMap<>();
                    msg.put("type", "text");
                    msg.put("text", message);
                    messages.add(msg);
                    payload.put("messages", messages);

                    // แปลง Map เป็น JSON String
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonPayload = objectMapper.writeValueAsString(payload);

                    exchange.getIn().setBody(jsonPayload);
                })
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json; charset=UTF-8"))
                .setHeader("Authorization", constant("Bearer ???????????"))
                .to("https://api.line.me/v2/bot/message/broadcast");
    }
}
