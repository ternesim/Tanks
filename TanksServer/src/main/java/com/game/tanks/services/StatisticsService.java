package com.game.tanks.services;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import com.game.tanks.models.*;

import java.util.List;

public interface StatisticsService {
    void saveStatistic(String name, int shots, int hits, int misses);
}
