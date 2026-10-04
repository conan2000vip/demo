package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.util.Optional;

public interface HomeStreakService {
    int getCurrentStreak(Long profileId, LocalDate today);

    Optional<StreakInfo> getCurrentStreakInfo(Long profileId, LocalDate today);

    boolean hasPreviousStreak(Long profileId, LocalDate today);

    Optional<StreakBreak> getBrokenStreak(Long profileId, LocalDate today);

    record StreakInfo(int days, LocalDate startDate, LocalDate lastCompletedDate) {
    }

    record StreakBreak(int days, LocalDate startDate, LocalDate lastCompletedDate) {
    }
}