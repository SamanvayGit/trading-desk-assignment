package com.example.tradingdesk.service;

import com.example.tradingdesk.api.dto.OrderRequest;
import com.example.tradingdesk.api.dto.OrderResponse;
import com.example.tradingdesk.domain.OrderSide;
import com.example.tradingdesk.domain.OrderStatus;
import com.example.tradingdesk.domain.Position;
import com.example.tradingdesk.domain.TradeOrder;
import com.example.tradingdesk.domain.Trader;
import com.example.tradingdesk.exception.BusinessRuleViolationException;
import com.example.tradingdesk.exception.ResourceNotFoundException;
import com.example.tradingdesk.repository.PositionRepository;
import com.example.tradingdesk.repository.TradeOrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_PENDING_ORDERS = 3;

    private final TraderService traderService;
    private final TradeOrderRepository orderRepository;
    private final PositionRepository positionRepository;

    public OrderService(
            TraderService traderService,
            TradeOrderRepository orderRepository,
            PositionRepository positionRepository
    ) {
        this.traderService = traderService;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Trader trader = traderService.getOrCreateLocked(request.traderId());
        String stock = SymbolNormalizer.normalize(request.stock());
        String sector = SymbolNormalizer.normalize(request.sector());

        long pendingCount = orderRepository.countByTraderAndStatus(trader, OrderStatus.PENDING);
        if (pendingCount >= MAX_PENDING_ORDERS) {
            throw new BusinessRuleViolationException("Trader " + trader.getTraderId() + " already has 3 pending orders");
        }

        if (request.side() == OrderSide.SELL) {
            int currentHolding = positionRepository.findByTraderAndStockForUpdate(trader, stock)
                    .map(Position::getQuantity)
                    .orElse(0);
            long reservedSellQuantity = orderRepository.sumQuantityByTraderStockStatusAndSide(
                    trader, stock, OrderStatus.PENDING, OrderSide.SELL);
            long available = currentHolding - reservedSellQuantity;
            if (available < request.quantity()) {
                throw new BusinessRuleViolationException(
                        "Insufficient available holdings for " + stock + ": required " + request.quantity() + ", available " + available);
            }
        }

        TradeOrder order = orderRepository.save(new TradeOrder(trader, stock, sector, request.quantity(), request.side()));
        log.info("Placed {} order {} for trader {} stock {} quantity {}", request.side(), order.getId(), trader.getTraderId(), stock, request.quantity());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse fillOrder(Long orderId) {
        TradeOrder snapshot = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        traderService.getOrCreateLocked(snapshot.getTrader().getTraderId());
        TradeOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));

        Position position = positionRepository.findByTraderAndStockForUpdate(order.getTrader(), order.getStock())
                .orElseGet(() -> new Position(order.getTrader(), order.getStock(), order.getSector(), 0));

        if (order.getSide() == OrderSide.BUY) {
            position.add(order.getQuantity(), order.getSector());
        } else {
            position.subtract(order.getQuantity());
        }
        positionRepository.save(position);
        order.fill();
        log.info("Filled order {} for trader {}", order.getId(), order.getTrader().getTraderId());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        TradeOrder snapshot = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        traderService.getOrCreateLocked(snapshot.getTrader().getTraderId());
        TradeOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        order.cancel();
        log.info("Cancelled order {} for trader {}", order.getId(), order.getTrader().getTraderId());
        return toResponse(order);
    }

    private OrderResponse toResponse(TradeOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getTrader().getTraderId(),
                order.getStock(),
                order.getSector(),
                order.getQuantity(),
                order.getSide(),
                order.getStatus()
        );
    }
}
