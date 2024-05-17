package com.game.tanks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import com.game.tanks.models.*;

@Component("StatisticsRepositoryImpl")
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private final DataSource dataSource;
    
    @Autowired
    public StatisticsRepositoryImpl(@Qualifier("HikariDataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Statistic entity) {
        try {
            String sql = "INSERT INTO statistics (name, shots, hits, misses) VALUES (?, ?, ?, ?)";
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.update(sql, entity.getName(), entity.getShots(), entity.getHits(), entity.getMisses());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Statistic entity) {
        try {
            String sql = "UPDATE statistics SET shots = ?, hits = ?, misses = ? WHERE name = ?";
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.update(sql, entity.getShots(), entity.getHits(), entity.getMisses(), entity.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean existsByName(String name) {
        try {
            String sql = "SELECT * FROM statistics WHERE name = ?";
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            return jdbcTemplate.queryForList(sql, name).size() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}