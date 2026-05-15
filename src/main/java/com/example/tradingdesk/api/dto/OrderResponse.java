package com.example.tradingdesk.api.dto;

import com.example.tradingdesk.domain.OrderSide;
import com.example.tradingdesk.domain.OrderStatus;

public record OrderResponse(
        Long orderId,
        String traderId,
        String stock,
        String sector,
        int quantity,
        OrderSide side,
        OrderStatus status
) {
}
