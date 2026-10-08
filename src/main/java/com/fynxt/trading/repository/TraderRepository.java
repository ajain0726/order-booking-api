package com.fynxt.trading.repository;

import com.fynxt.trading.domain.Trader;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TraderRepository extends JpaRepository<Trader, String> {

    /**
     * SELECT ... FOR UPDATE on the trader row. Every write for a trader takes
     * this lock first, which serialises that trader's operations while letting
     * different traders proceed in parallel.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trader t where t.id = :id")
    Optional<Trader> lockById(@Param("id") String id);
}
