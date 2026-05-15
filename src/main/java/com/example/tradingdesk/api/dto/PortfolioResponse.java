package com.example.tradingdesk.api.dto;

import java.util.Map;

public record PortfolioResponse(
        String traderId,
        Map<String, Integer> positions,
        Map<String, Integer> sectorBreakdown
) {
}
