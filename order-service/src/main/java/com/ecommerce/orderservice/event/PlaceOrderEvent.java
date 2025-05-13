package com.ecommerce.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlaceOrderEvent(
        Long orderId,
        String customerId,
        String productName,
        int quantity,
        BigDecimal price,
        LocalDateTime createDate
) {}
