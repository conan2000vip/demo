package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.healthlog.demo.repository.WeightRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeStreakServiceImpl implements HomeStreakService {

    private final WeightRepository weightRepository;

    @Override
    public int getCurrentStreak(Long profileId, LocalDate today) {
        Optional<StreakInfo> currentStreak = getCurrentStreakInfo(profileId, today);
        return currentStreak.isPresent() ? currentStreak.get().days() : 0;
    }

    @Override
    public Optional<StreakInfo> getCurrentStreakInfo(Long profileId, LocalDate today) {
        List<LocalDate> completeDays = getCompleteDays(profileId, today.plusDays(1).atStartOfDay());
        Set<LocalDate> daySet = new HashSet<>(completeDays);

        LocalDate lastCompletedDate = null;
        if (daySet.contains(today)) {
            lastCompletedDate = today;
        } else if (daySet.contains(today.minusDays(1))) {
            lastCompletedDate = today.minusDays(1);
        }
        if (lastCompletedDate == null)
            return Optional.empty();

        int days = countConsecutiveDays(daySet, lastCompletedDate);
        return Optional.of(new StreakInfo(days, lastCompletedDate.minusDays((long) days - 1), lastCompletedDate));
    }

    @Override
    public boolean hasPreviousStreak(Long profileId, LocalDate today) {
        return getBrokenStreak(profileId, today).isPresent();
    }

    @Override
    public Optional<StreakBreak> getBrokenStreak(Long profileId, LocalDate today) {
        List<LocalDate> completeDays = getCompleteDays(profileId, today.plusDays(1).atStartOfDay());
        if (completeDays.contains(today) || completeDays.contains(today.minusDays(1))) {
            return Optional.empty();
        }
        LocalDate lastStreakDay = completeDays.stream().filter(date -> !date.isAfter(today.minusDays(2))).findFirst()
                .orElse(null);
        if (lastStreakDay == null)
            return Optional.empty();

        int days = countConsecutiveDays(new HashSet<>(completeDays), lastStreakDay);
        return Optional.of(new StreakBreak(days, lastStreakDay.minusDays((long) days - 1), lastStreakDay));
    }

    private List<LocalDate> getCompleteDays(Long profileId, LocalDateTime beforeDate) {
        List<LocalDateTime> completedAtValues = weightRepository.findCompleteStreakDays(profileId, beforeDate);
        return completedAtValues.stream().filter(Objects::nonNull).map(ChronoLocalDateTime::toLocalDate).distinct()
                .sorted(Comparator.reverseOrder()).toList();
    }

    private int countConsecutiveDays(Set<LocalDate> completeDays, LocalDate startFrom) {
        int count = 0;
        for (LocalDate date = startFrom; completeDays.contains(date); date = date.minusDays(1)) {
            count++;
        }
        return count;
    }
}