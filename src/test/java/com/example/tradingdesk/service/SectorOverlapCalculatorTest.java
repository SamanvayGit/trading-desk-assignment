package com.example.tradingdesk.service;

import com.example.tradingdesk.domain.RiskFlag;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SectorOverlapCalculatorTest {

    private final SectorOverlapCalculator calculator = new SectorOverlapCalculator();

    @Test
    void calculatesWorkedExample() {
        SectorOverlapCalculator.Analysis analysis = calculator.analyze(Set.of("AAPL", "TSLA", "NVDA"));

        assertThat(analysis.dominantBasket()).isEqualTo("TECH_HEAVY");
        assertThat(analysis.riskFlag()).isEqualTo(RiskFlag.HIGH);
        assertThat(analysis.overlaps())
                .extracting(SectorOverlapCalculator.BasketOverlap::basket, SectorOverlapCalculator.BasketOverlap::overlap)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("TECH_HEAVY", new BigDecimal("75.00")),
                        org.assertj.core.groups.Tuple.tuple("BALANCED", new BigDecimal("50.00")),
                        org.assertj.core.groups.Tuple.tuple("FINANCE_HEAVY", new BigDecimal("0.00"))
                );
    }

    @Test
    void emptyPortfolioHasLowRisk() {
        SectorOverlapCalculator.Analysis analysis = calculator.analyze(Set.of());

        assertThat(analysis.riskFlag()).isEqualTo(RiskFlag.LOW);
        assertThat(analysis.overlaps())
                .allSatisfy(overlap -> assertThat(overlap.overlap()).isEqualByComparingTo("0.00"));
    }
}
