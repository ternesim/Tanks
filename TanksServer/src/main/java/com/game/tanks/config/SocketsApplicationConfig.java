package com.game.tanks.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import com.game.tanks.server.Server;

import javax.sql.DataSource;

@Configuration
@ComponentScan("com.game.tanks")
@PropertySource("classpath:db.properties")
public class SocketsApplicationConfig {
    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.user}")
    private String DB_USER;
    @Value("${db.password}")
    private String DB_PASSWD;
    @Value("${db.driver.name}")
    private String DB_DRIVER_NAME;

    @Bean(name = "HikariDataSource")
    @Scope("singleton")
    public DataSource initHikariDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(DB_URL);
        hikariConfig.setUsername(DB_USER);
        hikariConfig.setPassword(DB_PASSWD);
        hikariConfig.setDriverClassName(DB_DRIVER_NAME);
        DataSource dataSource = new HikariDataSource(hikariConfig);
        // String sql_del = "DROP TABLE IF EXISTS users";
        // String sql_create_user = "CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, name VARCHAR(255), password VARCHAR(255), statistics_id INT)";
        // String sql_del = "DROP TABLE IF EXISTS statistics";
        String sql_create_statistics = "CREATE TABLE IF NOT EXISTS statistics (id SERIAL PRIMARY KEY, name VARCHAR(255), shots INT DEFAULT 0, hits INT DEFAULT 0, misses INT DEFAULT 0)";
        try {
            // dataSource.getConnection().createStatement().execute(sql_create_user);
            dataSource.getConnection().createStatement().execute(sql_create_statistics);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataSource;
    }
}