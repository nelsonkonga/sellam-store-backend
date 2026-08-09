package com.sellam.store.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseCleanupRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupRunner.class);

    @Bean
    public CommandLineRunner runDatabaseCleanup(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                int rows = jdbcTemplate.update("UPDATE sales SET status = 'CONFIRMED' WHERE status IS NULL;");
                log.info("DatabaseCleanupRunner: {} sales rows updated to CONFIRMED.", rows);
            } catch (Exception e) {
                log.error("DatabaseCleanupRunner: Error executing cleanup script.", e);
            }
        };
    }
}
