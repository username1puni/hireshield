package com.hireshield.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.hireshield.repository.RuleConfigRepository;
import com.hireshield.repository.UserRepository;

/**
 * Runs the Flyway migrations and Hibernate validation against a real PostgreSQL.
 * Skipped automatically when Docker is not available (it runs in CI).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private RuleConfigRepository ruleConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void migrationsCreateSchemaAndSeedRules() {
        assertThat(ruleConfigRepository.count()).isEqualTo(12);
        assertThat(userRepository.findByEmailIgnoreCase("nobody@example.com")).isEmpty();
    }
}
