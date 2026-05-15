package com.example.tradingdesk.service;

import com.example.tradingdesk.api.dto.AddPositionRequest;
import com.example.tradingdesk.api.dto.PortfolioResponse;
import com.example.tradingdesk.api.dto.SectorOverlapItem;
import com.example.tradingdesk.api.dto.SectorOverlapResponse;
import com.example.tradingdesk.domain.Position;
import com.example.tradingdesk.domain.Trader;
import com.example.tradingdesk.repository.PositionRepository;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final TraderService traderService;
    private final PositionRepository positionRepository;
    private final SectorOverlapCalculator sectorOverlapCalculator;

    public PortfolioService(
            TraderService traderService,
            PositionRepository positionRepository,
            SectorOverlapCalculator sectorOverlapCalculator
    ) {
        this.traderService = traderService;
        this.positionRepository = positionRepository;
        this.sectorOverlapCalculator = sectorOverlapCalculator;
    }

    @Transactional
    public PortfolioResponse addToPortfolio(String traderId, AddPositionRequest request) {
        Trader trader = traderService.getOrCreateLocked(traderId);
        String stock = SymbolNormalizer.normalize(request.stock());
        String sector = SymbolNormalizer.normalize(request.sector());
        Position position = positionRepository.findByTraderAndStockForUpdate(trader, stock)
                .orElseGet(() -> new Position(trader, stock, sector, 0));
        position.add(request.quantity(), sector);
        positionRepository.save(position);
        log.info("Added {} {} shares to trader {}", request.quantity(), stock, trader.getTraderId());
        return getPortfolio(trader.getTraderId());
    }

    @Transactional
    public PortfolioResponse getPortfolio(String traderId) {
        Trader trader = traderService.getOrCreateLocked(traderId);
        List<Position> positions = positionRepository.findByTrader(trader);
        return toPortfolioResponse(trader.getTraderId(), positions);
    }

    @Transactional
    public SectorOverlapResponse getSectorOverlap(String traderId) {
        Trader trader = traderService.getOrCreateLocked(traderId);
        Set<String> stocks = positionRepository.findByTrader(trader).stream()
                .filter(position -> position.getQuantity() > 0)
                .map(Position::getStock)
                .collect(Collectors.toSet());
        SectorOverlapCalculator.Analysis analysis = sectorOverlapCalculator.analyze(stocks);
        List<SectorOverlapItem> overlaps = analysis.overlaps().stream()
                .sorted(Comparator.comparing(SectorOverlapCalculator.BasketOverlap::overlap).reversed())
                .map(overlap -> new SectorOverlapItem(overlap.basket(), overlap.overlap().toPlainString() + "%"))
                .collect(Collectors.toList());
        return new SectorOverlapResponse(overlaps, analysis.dominantBasket(), analysis.riskFlag().name());
    }

    private PortfolioResponse toPortfolioResponse(String traderId, List<Position> positions) {
        Map<String, Integer> byStock = positions.stream()
                .filter(position -> position.getQuantity() > 0)
                .sorted(Comparator.comparing(Position::getStock))
                .collect(Collectors.toMap(
                        Position::getStock,
                        Position::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));
        Map<String, Integer> bySector = positions.stream()
                .filter(position -> position.getQuantity() > 0)
                .sorted(Comparator.comparing(Position::getSector))
                .collect(Collectors.toMap(
                        Position::getSector,
                        Position::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));
        return new PortfolioResponse(traderId, byStock, bySector);
    }
}
