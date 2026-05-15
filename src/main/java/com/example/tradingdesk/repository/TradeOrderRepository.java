package com.example.tradingdesk.repository;

import com.example.tradingdesk.domain.OrderSide;
import com.example.tradingdesk.domain.OrderStatus;
import com.example.tradingdesk.domain.TradeOrder;
import com.example.tradingdesk.domain.Trader;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    long countByTraderAndStatus(Trader trader, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from TradeOrder o join fetch o.trader where o.id = :id")
    Optional<TradeOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select coalesce(sum(o.quantity), 0)
            from TradeOrder o
            where o.trader = :trader
              and o.stock = :stock
              and o.status = :status
              and o.side = :side
            """)
    long sumQuantityByTraderStockStatusAndSide(
            @Param("trader") Trader trader,
            @Param("stock") String stock,
            @Param("status") OrderStatus status,
            @Param("side") OrderSide side
    );
}
