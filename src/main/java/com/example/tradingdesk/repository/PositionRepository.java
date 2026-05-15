package com.example.tradingdesk.repository;

import com.example.tradingdesk.domain.Position;
import com.example.tradingdesk.domain.Trader;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByTrader(Trader trader);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Position p where p.trader = :trader and p.stock = :stock")
    Optional<Position> findByTraderAndStockForUpdate(@Param("trader") Trader trader, @Param("stock") String stock);
}
