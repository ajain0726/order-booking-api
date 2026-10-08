package com.fynxt.trading.repository;

import com.fynxt.trading.domain.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    @Query("select h from Holding h where h.traderId = :traderId and h.stock.symbol = :symbol")
    Optional<Holding> findByTraderAndStock(@Param("traderId") String traderId, @Param("symbol") String symbol);

    @Query("select h from Holding h join fetch h.stock where h.traderId = :traderId and h.quantity > 0")
    List<Holding> findOpenPositions(@Param("traderId") String traderId);
}
