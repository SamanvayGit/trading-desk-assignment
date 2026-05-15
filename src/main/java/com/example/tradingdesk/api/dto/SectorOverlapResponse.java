package com.example.tradingdesk.api.dto;

import java.util.List;

public record SectorOverlapResponse(
        List<SectorOverlapItem> overlaps,
        String dominantBasket,
        String riskFlag
) {
}
