package com.example.tradingdesk.api;

import com.example.tradingdesk.api.dto.AddPositionRequest;
import com.example.tradingdesk.api.dto.PortfolioResponse;
import com.example.tradingdesk.api.dto.SectorOverlapResponse;
import com.example.tradingdesk.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traders/{traderId}/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping
    public PortfolioResponse addToPortfolio(
            @PathVariable String traderId,
            @Valid @RequestBody AddPositionRequest request
    ) {
        return portfolioService.addToPortfolio(traderId, request);
    }

    @GetMapping
    public PortfolioResponse getPortfolio(@PathVariable String traderId) {
        return portfolioService.getPortfolio(traderId);
    }

    @GetMapping("/sector-overlap")
    public SectorOverlapResponse getSectorOverlap(@PathVariable String traderId) {
        return portfolioService.getSectorOverlap(traderId);
    }
}
