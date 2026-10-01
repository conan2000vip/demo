package com.healthlog.demo.service.home;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.service.sleep.SleepService;
import com.healthlog.demo.service.step.StepService;
import com.healthlog.demo.service.water.WaterService;
import com.healthlog.demo.service.weight.WeightService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeTodayServiceImpl implements HomeTodayService {

    private static final int SLEEP_TOLERANCE_MINUTES = 30;
    private static final int STEP_TOLERANCE_COUNT = 300;
    private static final int STEP_TOLERANCE_PERCENT = 95;
    private static final int WATER_TOLERANCE_ML = 100;
    private static final int WATER_TOLERANCE_PERCENT = 95;
    private static final BigDecimal WEIGHT_TOLERANCE_KG = BigDecimal.valueOf(0.5);

    private final ProfileService profileService;
    private final WeightService weightService;
    private final SleepService sleepService;
    private final WaterService waterService;
    private final StepService stepService;

    @Override
    public Map<String, Object> buildToday(Long profileId, Long currentUserId) {
        LocalDate todayDate = LocalDate.now();
        DateRangerFilter range = new DateRangerFilter(todayDate, todayDate);
        Profile profile = profileService.getProfile(currentUserId, profileId);

        Map<String, Object> today = createDefaults();
        applyWeight(today, profile, profileId, currentUserId, range);
        applySleep(today, profile, profileId, currentUserId, range);
        applyWater(today, profile, profileId, currentUserId, range);
        applyStep(today, profile, profileId, currentUserId, range);
        return today;
    }

    private Map<String, Object> createDefaults() {
        Map<String, Object> t = new HashMap<>();
        t.put("weightKg", null);
        t.put("bmi", null);
        t.put("bmiStatus", null);
        t.put("bmiStatusCode", null);
        t.put("targetWeight", null);
        t.put("weightGoalStatus", "NO_RECORD");
        t.put("weightGoalText", "今日の体重は未記録です");
        t.put("sleepHour", null);
        t.put("sleepMinute", null);
        t.put("sleepGoalMinutes", null);
        t.put("sleepGoalStatus", "NO_RECORD");
        t.put("sleepGoalText", "今日の睡眠は未記録です");
        t.put("waterMl", null);
        t.put("waterPercent", 0);
        t.put("waterGoalStatus", "NO_RECORD");
        t.put("waterGoalText", "今日の水分は未記録です");
        t.put("stepCount", null);
        t.put("stepPercent", 0);
        t.put("stepGoalStatus", "NO_RECORD");
        t.put("stepGoalText", "今日の歩数は未記録です");
        return t;
    }

    @SuppressWarnings({ "unchecked", "null" })
    private void applyWeight(Map<String, Object> today, Profile profile, Long profileId, Long userId,
            DateRangerFilter range) {
        try {
            Map<String, Object> result = weightService.list(profileId, userId, range, 0);
            Map<String, Object> stats = (Map<String, Object>) result.get("stats");
            BigDecimal latest = stats != null ? (BigDecimal) stats.get("latest") : null;
            if (latest == null) {
                return;
            }
            today.put("weightKg", latest);
            today.put("bmi", stats.get("bmi"));
            today.put("bmiStatus", stats.get("bmiStatus"));
            today.put("bmiStatusCode", stats.get("bmiStatusCode"));

            BigDecimal target = profile.getTargetWeight();
            if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
                today.put("weightGoalStatus", "NO_GOAL");
                today.put("weightGoalText", "目標体重が設定されていません");
                return;
            }
            today.put("targetWeight", target);

            BigDecimal diff = latest.subtract(target);
            if (diff.abs().compareTo(WEIGHT_TOLERANCE_KG) <= 0) {
                today.put("weightGoalStatus", "ACHIEVED");
                today.put("weightGoalText", "目標体重を達成しました");
                return;
            }
            today.put("weightGoalStatus", diff.signum() > 0 ? "LOSE" : "GAIN");
            today.put("weightGoalText", "目標まであと" + diff.abs().setScale(1, RoundingMode.HALF_UP) + " kg");
        } catch (BusinessException e) {
            // 既定値(未記録)のまま
        }
    }

    private void applySleep(Map<String, Object> today, Profile profile, Long profileId, Long userId,
            DateRangerFilter range) {
        try {
            Map<String, Object> result = sleepService.list(profileId, userId, range, 0);
            List<?> logs = (List<?>) result.get("logs");
            if (logs == null || logs.isEmpty()) {
                return;
            }
            int total = 0;
            boolean valid = false;
            for (Object o : logs) {
                if (o instanceof Sleep s && s.getSleepMinutes() != null) {
                    total += s.getSleepMinutes();
                    valid = true;
                }
            }
            if (!valid) {
                return;
            }
            today.put("sleepHour", total / 60);
            today.put("sleepMinute", total % 60);

            BigDecimal goalHours = profile.getDailySleepGoal();
            if (goalHours == null || goalHours.compareTo(BigDecimal.ZERO) <= 0) {
                today.put("sleepGoalStatus", "NO_GOAL");
                today.put("sleepGoalText", "睡眠の目標が設定されていません");
                return;
            }
            int goalMinutes = goalHours.multiply(BigDecimal.valueOf(60)).intValue();
            today.put("sleepGoalMinutes", goalMinutes);

            if (total >= goalMinutes - SLEEP_TOLERANCE_MINUTES) {
                today.put("sleepGoalStatus", "ACHIEVED");
                today.put("sleepGoalText", "睡眠目標を達成しました");
                return;
            }
            int remain = goalMinutes - total;
            today.put("sleepGoalStatus", "NOT_ACHIEVED");
            today.put("sleepGoalText", remain / 60 > 0 ? "目標まであと" + (remain / 60) + "時間" + (remain % 60) + "分"
                    : "目標まであと" + (remain % 60) + "分");
        } catch (BusinessException e) {
            // 既定値(未記録)のまま
        }
    }

    @SuppressWarnings("unchecked")
    private void applyWater(Map<String, Object> today, Profile profile, Long profileId, Long userId,
            DateRangerFilter range) {
        try {
            Map<String, Object> result = waterService.list(profileId, userId, range, 0);
            Map<String, Object> stats = (Map<String, Object>) result.get("stats");
            Integer total = stats != null ? (Integer) stats.get("todayTotal") : null;
            if (total == null) {
                return;
            }
            today.put("waterMl", total);

            Integer goal = profile.getWaterGoalMl();
            if (goal == null || goal <= 0) {
                today.put("waterGoalStatus", "NO_GOAL");
                today.put("waterGoalText", "水分目標が設定されていません");
                return;
            }
            int percent = (int) Math.round(total * 100.0 / goal);
            today.put("waterPercent", percent);

            if (total >= goal || percent >= WATER_TOLERANCE_PERCENT || goal - total <= WATER_TOLERANCE_ML) {
                today.put("waterGoalStatus", "ACHIEVED");
                today.put("waterGoalText", "水分目標を達成しました");
            } else {
                today.put("waterGoalStatus", "NOT_ACHIEVED");
                today.put("waterGoalText", "目標まであと" + (goal - total) + " ml");
            }
        } catch (BusinessException e) {
            // 既定値(未記録)のまま
        }
    }

    @SuppressWarnings("unchecked")
    private void applyStep(Map<String, Object> today, Profile profile, Long profileId, Long userId,
            DateRangerFilter range) {
        try {
            Map<String, Object> result = stepService.list(profileId, userId, range, 0);
            Map<String, Object> stats = (Map<String, Object>) result.get("stats");
            Integer steps = stats != null ? (Integer) stats.get("todaySteps") : null;
            if (steps == null) {
                return;
            }
            today.put("stepCount", steps);

            Integer goal = profile.getStepGoal();
            if (goal == null || goal <= 0) {
                today.put("stepGoalStatus", "NO_GOAL");
                today.put("stepGoalText", "歩数目標が設定されていません");
                return;
            }
            int percent = (int) Math.round(steps * 100.0 / goal);
            today.put("stepPercent", percent);

            if (steps >= goal || percent >= STEP_TOLERANCE_PERCENT || goal - steps <= STEP_TOLERANCE_COUNT) {
                today.put("stepGoalStatus", "ACHIEVED");
                today.put("stepGoalText", "歩数目標を達成しました");
            } else {
                today.put("stepGoalStatus", "NOT_ACHIEVED");
                today.put("stepGoalText", "目標まであと" + (goal - steps) + " 歩");
            }
        } catch (BusinessException e) {
            // 既定値(未記録)のまま
        }
    }
}