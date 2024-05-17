package com.game.tanks.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.game.tanks.models.*;
import com.game.tanks.repositories.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

@Component("StatisticsServiceImpl")
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    @Qualifier("StatisticsRepositoryImpl")
    private StatisticsRepository statisticsRepository;

    public StatisticsServiceImpl(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    @Override
    public void saveStatistic(String name,int shots, int hits, int misses) {
        if(!statisticsRepository.existsByName(name)) statisticsRepository.save(new Statistic(name, shots, hits, misses));
        else statisticsRepository.update(new Statistic(name, shots, hits, misses));
    }
}
