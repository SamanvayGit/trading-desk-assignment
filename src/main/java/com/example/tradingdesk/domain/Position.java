package com.example.tradingdesk.domain;

import com.example.tradingdesk.exception.BusinessRuleViolationException;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "positions",
        uniqueConstraints = @UniqueConstraint(name = "uk_positions_trader_stock", columnNames = {"trader_id", "stock"})
)
public class Position {

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

    @Version
    @Column(nullable = false)
    private long version;

    protected Position() {
    }

    public Position(Trader trader, String stock, String sector, int quantity) {
        this.trader = trader;
        this.stock = stock;
        this.sector = sector;
        this.quantity = quantity;
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

    public void add(int amount, String sector) {
        this.quantity += amount;
        this.sector = sector;
    }

    public void subtract(int amount) {
        if (quantity < amount) {
            throw new BusinessRuleViolationException("Insufficient holdings for " + stock + ": required " + amount + ", available " + quantity);
        }
        this.quantity -= amount;
    }
}
