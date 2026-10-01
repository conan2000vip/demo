package com.healthlog.demo.service.home;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeStreakServiceImpl implements HomeStreakService {

    private static final int MAX_DAYS = 366;

    private final WeightRepository weightRepository;
    private final SleepRepository sleepRepository;
    private final WaterRepository waterRepository;
    private final StepRepository stepRepository;

    @Override
    public int getCurrentStreak(Long profileId, LocalDate today) {
        LocalDate start = hasAll(profileId, today) ? today
                : hasAll(profileId, today.minusDays(1)) ? today.minusDays(1) : null;
        if (start == null) {
            return 0;
        }
        int streak = 0;
        for (LocalDate d = start; streak < MAX_DAYS && hasAll(profileId, d); d = d.minusDays(1)) {
            streak++;
        }
        return streak;
    }

    private boolean hasAll(Long profileId, LocalDate date) {
        return weightRepository.existsByProfile_IdAndRecordedDate(profileId, date)
                && sleepRepository.existsByProfile_IdAndRecordedDate(profileId, date)
                && waterRepository.existsByProfile_IdAndRecordedDate(profileId, date)
                && stepRepository.existsByProfile_IdAndRecordedDate(profileId, date);
    }
}