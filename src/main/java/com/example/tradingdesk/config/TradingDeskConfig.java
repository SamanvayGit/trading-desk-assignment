package com.example.tradingdesk.config;

import com.example.tradingdesk.service.SectorOverlapCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradingDeskConfig {

    @Bean
    SectorOverlapCalculator sectorOverlapCalculator() {
        return new SectorOverlapCalculator();
    }
}
