package com.fynxt.trading.mysql;

import com.fynxt.trading.service.ConcurrencyTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Re-runs every {@link ConcurrencyTest} case against a real MySQL server
 * (InnoDB row locks, CHECK constraints). Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@ImportTestcontainers(MySqlTestContainer.class)
class MySqlConcurrencyTest extends ConcurrencyTest {
}
