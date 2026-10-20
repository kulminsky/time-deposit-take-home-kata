package org.ikigaidigital;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Postgres in Docker for integration tests. Spring Boot points the datasource at it (@ServiceConnection).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Same image as compose.yaml.
    public static final String POSTGRES_IMAGE = "postgres:18.6-alpine";

    // One container for all integration tests. Each test class has its own Spring context, and each would
    // otherwise start its own Postgres. The contexts stay cached until the JVM exits, so it is only stopped then.
    // The tests share the database, so each one starts by emptying the tables (db/clean.sql or @BeforeEach).
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
