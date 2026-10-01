package com.healthlog.demo.service.home;

import java.time.LocalDate;

public interface HomeStreakService {
    int getCurrentStreak(Long profileId, LocalDate today);
}