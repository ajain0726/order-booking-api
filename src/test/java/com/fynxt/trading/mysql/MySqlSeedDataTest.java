package com.fynxt.trading.mysql;

import com.fynxt.trading.web.SeedDataTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * schema.sql and data.sql load on real MySQL and the API reads them correctly.
 * Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@ImportTestcontainers(MySqlTestContainer.class)
class MySqlSeedDataTest extends SeedDataTest {
}
