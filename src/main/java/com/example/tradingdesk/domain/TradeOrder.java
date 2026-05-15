package com.example.tradingdesk.domain;

import com.example.tradingdesk.exception.InvalidOrderStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "trade_orders")
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trader_id", nullable = false)
    private Trader trader;

    @Column(nullable = false, length = 16)
    private String stock;

    @Column(nullable = false, length = 32)
    private String sector;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status = OrderStatus.PENDING;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TradeOrder() {
    }

    public TradeOrder(Trader trader, String stock, String sector, int quantity, OrderSide side) {
        this.trader = trader;
        this.stock = stock;
        this.sector = sector;
        this.quantity = quantity;
        this.side = side;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Trader getTrader() {
        return trader;
    }

    public String getStock() {
        return stock;
    }

    public String getSector() {
        return sector;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void fill() {
        ensurePending("fill");
        status = OrderStatus.FILLED;
    }

    public void cancel() {
        ensurePending("cancel");
        status = OrderStatus.CANCELLED;
    }

    private void ensurePending(String action) {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Cannot " + action + " order " + id + " because it is " + status);
        }
    }
}
