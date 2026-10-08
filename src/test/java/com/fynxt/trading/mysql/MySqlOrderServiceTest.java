package com.fynxt.trading.mysql;

import com.fynxt.trading.service.OrderServiceTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Re-runs every {@link OrderServiceTest} case against a real MySQL server
 * (InnoDB row locks, CHECK constraints). Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@ImportTestcontainers(MySqlTestContainer.class)
class MySqlOrderServiceTest extends OrderServiceTest {
}
