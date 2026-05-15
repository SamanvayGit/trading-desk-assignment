package com.example.tradingdesk.api.dto;

import com.example.tradingdesk.domain.OrderSide;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
        @NotBlank String traderId,
        @NotBlank String stock,
        @NotBlank String sector,
        @Min(1) int quantity,
        @NotNull OrderSide side
) {
}
