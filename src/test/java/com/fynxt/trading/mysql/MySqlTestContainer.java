package com.fynxt.trading.mysql;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * One MySQL 8.4 container shared by every MySQL test class; Spring Boot
 * points the datasource at it via {@link ServiceConnection}.
 */
interface MySqlTestContainer {

    @Container
    @ServiceConnection
    MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
}
