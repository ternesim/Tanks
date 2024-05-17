package com.game.tanks.repositories;

import java.util.Optional;
import java.time.LocalDateTime;

import com.game.tanks.models.Statistic;

import com.game.tanks.repositories.*;

public interface StatisticsRepository {
    public void save(Statistic entity);
    public void update(Statistic entity);
    public boolean existsByName(String name);
}