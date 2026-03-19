package com.financeapp.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        String configuredUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                "jdbc:sqlite:finance.db"
        );
        String jdbcUrl = normalizeJdbcUrl(configuredUrl);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName(resolveDriverClassName(jdbcUrl, environment.getProperty("SPRING_DATASOURCE_DRIVER_CLASS_NAME")));
        return dataSource;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(Environment environment) {
        return properties -> properties.put("hibernate.dialect", resolveDialect(environment));
    }

    private String resolveDialect(Environment environment) {
        String explicitDialect = environment.getProperty("SPRING_JPA_DATABASE_PLATFORM");
        if (explicitDialect != null && !explicitDialect.isBlank()) {
            return explicitDialect;
        }

        String configuredUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                "jdbc:sqlite:finance.db"
        );

        return normalizeJdbcUrl(configuredUrl).startsWith("jdbc:postgresql:")
                ? "org.hibernate.dialect.PostgreSQLDialect"
                : "org.hibernate.community.dialect.SQLiteDialect";
    }

    private String resolveDriverClassName(String jdbcUrl, String explicitDriverClassName) {
        if (explicitDriverClassName != null && !explicitDriverClassName.isBlank()) {
            return explicitDriverClassName;
        }

        return jdbcUrl.startsWith("jdbc:postgresql:")
                ? "org.postgresql.Driver"
                : "org.sqlite.JDBC";
    }

    private String normalizeJdbcUrl(String value) {
        if (value.startsWith("postgresql://")) {
            return "jdbc:" + value;
        }
        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException("Nenhuma URL de banco configurada.");
    }
}
