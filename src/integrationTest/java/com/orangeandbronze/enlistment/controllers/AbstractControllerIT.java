package com.orangeandbronze.enlistment.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static com.orangeandbronze.enlistment.domain.TestUtils.DEFAULT_DATABASE_NAME;

class AbstractControllerIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MockMvc mockMvc;

    @Container
    private final PostgreSQLContainer container = new PostgreSQLContainer("postgres:14")
            .withDatabaseName(DEFAULT_DATABASE_NAME).withUsername(DEFAULT_DATABASE_NAME).withPassword(DEFAULT_DATABASE_NAME);

    // @DynamicPropertySource to overwrite the application.properties file only for this
    // specific context, integration testing
    @DynamicPropertySource
    private static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:postgresql:14:///" + DEFAULT_DATABASE_NAME);
        registry.add("spring.datasource.password", () -> DEFAULT_DATABASE_NAME);
        registry.add("spring.datasource.username", () -> DEFAULT_DATABASE_NAME);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }
}
