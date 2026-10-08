package com.fynxt.trading.service;

import com.fynxt.trading.repository.TraderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acquires the per-trader row lock. Must be called inside the transaction
 * whose work it protects; the lock is released when that transaction ends.
 */
@Component
class TraderLock {

    private final TraderRepository traderRepository;

    TraderLock(TraderRepository traderRepository) {
        this.traderRepository = traderRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(String traderId) {
        traderRepository.lockById(traderId)
                .orElseThrow(() -> new IllegalStateException("Trader " + traderId + " is not registered"));
    }
}
