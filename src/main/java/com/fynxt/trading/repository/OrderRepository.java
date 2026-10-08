package com.fynxt.trading.repository;

import com.fynxt.trading.domain.Order;
import com.fynxt.trading.domain.OrderSide;
import com.fynxt.trading.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Scalar lookup so the order entity is not cached before the trader lock is held. */
    @Query("select o.traderId from Order o where o.id = :id")
    Optional<String> findTraderIdById(@Param("id") Long id);

    long countByTraderIdAndStatus(String traderId, OrderStatus status);

    @Query("""
            select coalesce(sum(o.quantity), 0) from Order o
            where o.traderId = :traderId and o.stock.symbol = :symbol
              and o.side = :side and o.status = :status
            """)
    long sumQuantity(@Param("traderId") String traderId,
                     @Param("symbol") String symbol,
                     @Param("side") OrderSide side,
                     @Param("status") OrderStatus status);
}
