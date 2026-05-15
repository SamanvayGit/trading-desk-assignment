package com.example.tradingdesk.service;

import com.example.tradingdesk.api.dto.AddPositionRequest;
import com.example.tradingdesk.api.dto.OrderRequest;
import com.example.tradingdesk.api.dto.OrderResponse;
import com.example.tradingdesk.api.dto.PortfolioResponse;
import com.example.tradingdesk.domain.OrderSide;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentOrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PortfolioService portfolioService;

    @Test
    void concurrentBuyOrdersRespectPendingLimit() throws Exception {
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            int index = i;
            tasks.add(() -> {
                start.await();
                try {
                    orderService.placeOrder(new OrderRequest("CONCURRENT-LIMIT", "STK" + index, "TECH", 1, OrderSide.BUY));
                    return true;
                } catch (RuntimeException exception) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = tasks.stream()
                .map(executor::submit)
                .collect(Collectors.toList());
        start.countDown();

        long successes = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successes++;
            }
        }
        executor.shutdown();

        assertThat(successes).isEqualTo(3);
    }

    @Test
    void concurrentSellOrdersReserveHoldings() throws Exception {
        portfolioService.addToPortfolio("CONCURRENT-SELL", new AddPositionRequest("AAPL", "TECH", 100));
        int attempts = 5;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Callable<OrderResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            tasks.add(() -> {
                start.await();
                try {
                    return orderService.placeOrder(new OrderRequest("CONCURRENT-SELL", "AAPL", "TECH", 40, OrderSide.SELL));
                } catch (RuntimeException exception) {
                    return null;
                }
            });
        }

        List<Future<OrderResponse>> futures = tasks.stream()
                .map(executor::submit)
                .collect(Collectors.toList());
        start.countDown();

        List<OrderResponse> accepted = new ArrayList<>();
        for (Future<OrderResponse> future : futures) {
            OrderResponse order = future.get();
            if (order != null) {
                accepted.add(order);
            }
        }
        executor.shutdown();

        assertThat(accepted).hasSize(2);
        accepted.forEach(order -> orderService.fillOrder(order.orderId()));
        PortfolioResponse portfolio = portfolioService.getPortfolio("CONCURRENT-SELL");
        assertThat(portfolio.positions()).containsEntry("AAPL", 20);
    }
}
