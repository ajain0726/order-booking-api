package com.fynxt.trading.service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transaction templates used by the services.
 *
 * <p>READ COMMITTED is set explicitly because MySQL/InnoDB defaults to
 * REPEATABLE READ, where a plain SELECT reads from a snapshot taken at the
 * transaction's first read. Fill/cancel read the order's trader id before
 * taking the trader lock; under REPEATABLE READ the order re-read after the
 * lock would still come from that earlier snapshot and could show PENDING
 * for an order another request has just filled. With READ COMMITTED every
 * statement sees the latest committed data, so reads done while holding
 * the lock are current.
 */
final class Transactions {

    private Transactions() {
    }

    static TransactionTemplate readCommitted(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return template;
    }

    static TransactionTemplate readOnly(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = readCommitted(transactionManager);
        template.setReadOnly(true);
        return template;
    }
}
