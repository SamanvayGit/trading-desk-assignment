package com.example.tradingdesk.service;

import com.example.tradingdesk.api.dto.AddPositionRequest;
import com.example.tradingdesk.api.dto.OrderRequest;
import com.example.tradingdesk.api.dto.OrderResponse;
import com.example.tradingdesk.api.dto.PortfolioResponse;
import com.example.tradingdesk.domain.OrderSide;
import com.example.tradingdesk.domain.OrderStatus;
import com.example.tradingdesk.exception.BusinessRuleViolationException;
import com.example.tradingdesk.exception.InvalidOrderStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PortfolioService portfolioService;

    @Test
    void buyFillIncreasesHoldings() {
        OrderResponse order = orderService.placeOrder(new OrderRequest("BUY-FILL", "aapl", "tech", 50, OrderSide.BUY));

        OrderResponse filled = orderService.fillOrder(order.orderId());
        PortfolioResponse portfolio = portfolioService.getPortfolio("BUY-FILL");

        assertThat(filled.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(portfolio.positions()).containsEntry("AAPL", 50);
        assertThat(portfolio.sectorBreakdown()).containsEntry("TECH", 50);
    }

    @Test
    void sellPlaceFailsWhenHoldingsAreInsufficient() {
        assertThatThrownBy(() -> orderService.placeOrder(new OrderRequest("NO-HOLDINGS", "TSLA", "TECH", 1, OrderSide.SELL)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient available holdings");
    }

    @Test
    void sellFillDecreasesHoldings() {
        portfolioService.addToPortfolio("SELL-FILL", new AddPositionRequest("NVDA", "TECH", 100));
        OrderResponse order = orderService.placeOrder(new OrderRequest("SELL-FILL", "NVDA", "TECH", 40, OrderSide.SELL));

        orderService.fillOrder(order.orderId());
        PortfolioResponse portfolio = portfolioService.getPortfolio("SELL-FILL");

        assertThat(portfolio.positions()).containsEntry("NVDA", 60);
    }

    @Test
    void cannotCancelFilledOrder() {
        OrderResponse order = orderService.placeOrder(new OrderRequest("CANCEL-FILLED", "MSFT", "TECH", 10, OrderSide.BUY));
        orderService.fillOrder(order.orderId());

        assertThatThrownBy(() -> orderService.cancelOrder(order.orderId()))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Cannot cancel");
    }

    @Test
    void pendingOrderLimitIsThree() {
        orderService.placeOrder(new OrderRequest("LIMIT-THREE", "AAPL", "TECH", 1, OrderSide.BUY));
        orderService.placeOrder(new OrderRequest("LIMIT-THREE", "MSFT", "TECH", 1, OrderSide.BUY));
        orderService.placeOrder(new OrderRequest("LIMIT-THREE", "GOOGL", "TECH", 1, OrderSide.BUY));

        assertThatThrownBy(() -> orderService.placeOrder(new OrderRequest("LIMIT-THREE", "TSLA", "TECH", 1, OrderSide.BUY)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has 3 pending orders");
    }
}
