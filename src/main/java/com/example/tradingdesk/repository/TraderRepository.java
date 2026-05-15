package com.example.tradingdesk.repository;

import com.example.tradingdesk.domain.Trader;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TraderRepository extends JpaRepository<Trader, Long> {

    Optional<Trader> findByTraderId(String traderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trader t where t.traderId = :traderId")
    Optional<Trader> findByTraderIdForUpdate(@Param("traderId") String traderId);
}
