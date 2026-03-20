package com.financeapp.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        String configuredUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                "jdbc:sqlite:finance.db"
        );
        DatabaseSettings settings = resolveDatabaseSettings(configuredUrl, environment);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(settings.jdbcUrl());
        dataSource.setDriverClassName(resolveDriverClassName(settings.jdbcUrl(), environment.getProperty("SPRING_DATASOURCE_DRIVER_CLASS_NAME")));
        if (settings.username() != null && !settings.username().isBlank()) {
            dataSource.setUsername(settings.username());
        }
        if (settings.password() != null && !settings.password().isBlank()) {
            dataSource.setPassword(settings.password());
        }
        return dataSource;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(Environment environment) {
        return properties -> {
            String dialect = resolveDialect(environment);
            if (dialect == null) {
                properties.remove("hibernate.dialect");
                return;
            }
            properties.put("hibernate.dialect", dialect);
        };
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

        return resolveDatabaseSettings(configuredUrl, environment).jdbcUrl().startsWith("jdbc:postgresql:")
                ? null
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

    private DatabaseSettings resolveDatabaseSettings(String configuredUrl, Environment environment) {
        String explicitUsername = environment.getProperty("SPRING_DATASOURCE_USERNAME");
        String explicitPassword = environment.getProperty("SPRING_DATASOURCE_PASSWORD");

        if (configuredUrl.startsWith("postgresql://")) {
            URI uri = URI.create(configuredUrl);
            String[] credentials = parseCredentials(uri.getUserInfo());
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "") + uri.getPath();
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl += "?" + uri.getQuery();
            }

            String username = explicitUsername != null && !explicitUsername.isBlank() ? explicitUsername : credentials[0];
            String password = explicitPassword != null && !explicitPassword.isBlank() ? explicitPassword : credentials[1];
            return new DatabaseSettings(jdbcUrl, username, password);
        }

        return new DatabaseSettings(configuredUrl, explicitUsername, explicitPassword);
    }

    private String[] parseCredentials(String userInfo) {
        if (userInfo == null || userInfo.isBlank()) {
            return new String[] { null, null };
        }

        String[] rawParts = userInfo.split(":", 2);
        String username = decode(rawParts[0]);
        String password = rawParts.length > 1 ? decode(rawParts[1]) : null;
        return new String[] { username, password };
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException("Nenhuma URL de banco configurada.");
    }

    private record DatabaseSettings(String jdbcUrl, String username, String password) {}
}
