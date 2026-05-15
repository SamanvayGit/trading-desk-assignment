package com.example.tradingdesk.service;

import com.example.tradingdesk.domain.RiskFlag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SectorOverlapCalculator {

    private static final Map<String, Set<String>> BASKETS = new LinkedHashMap<>();

    static {
        BASKETS.put("TECH_HEAVY", Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA"));
        BASKETS.put("FINANCE_HEAVY", Set.of("JPM", "GS", "BAC", "MS", "WFC"));
        BASKETS.put("BALANCED", Set.of("AAPL", "JPM", "XOM", "JNJ", "TSLA"));
    }

    public Analysis analyze(Set<String> portfolioStocks) {
        Set<String> normalizedStocks = portfolioStocks.stream()
                .map(SymbolNormalizer::normalize)
                .collect(Collectors.toSet());

        List<BasketOverlap> overlaps = BASKETS.entrySet().stream()
                .map(entry -> calculate(entry.getKey(), normalizedStocks, entry.getValue()))
                .collect(Collectors.toList());

        BasketOverlap dominant = overlaps.stream()
                .max(Comparator.comparing(BasketOverlap::overlap))
                .orElse(new BasketOverlap("NONE", BigDecimal.ZERO));

        return new Analysis(overlaps, dominant.basket(), riskFlag(dominant.overlap()));
    }

    private BasketOverlap calculate(String basket, Set<String> portfolioStocks, Set<String> basketStocks) {
        if (portfolioStocks.isEmpty()) {
            return new BasketOverlap(basket, BigDecimal.ZERO.setScale(2));
        }
        long commonStocks = portfolioStocks.stream()
                .filter(basketStocks::contains)
                .count();
        int denominator = portfolioStocks.size() + basketStocks.size();
        BigDecimal overlap = BigDecimal.valueOf(2L * commonStocks * 100)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        return new BasketOverlap(basket, overlap);
    }

    private RiskFlag riskFlag(BigDecimal overlap) {
        if (overlap.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return RiskFlag.HIGH;
        }
        if (overlap.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return RiskFlag.MEDIUM;
        }
        return RiskFlag.LOW;
    }

    public record Analysis(List<BasketOverlap> overlaps, String dominantBasket, RiskFlag riskFlag) {
    }

    public record BasketOverlap(String basket, BigDecimal overlap) {
    }
}
