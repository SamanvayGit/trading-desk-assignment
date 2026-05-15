package com.example.tradingdesk.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddPositionRequest(
        @NotBlank String stock,
        @NotBlank String sector,
        @Min(1) int quantity
) {
}
