package com.healthlog.demo.service.water;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.dto.feedback.FeedbackLevel;
import com.healthlog.demo.dto.feedback.FeedbackType;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Water;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.WaterRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WaterFeedbackRule {
    private static final int ALMOST_MIN_PERCENT = 80;
    private static final int LOW_THRESHOLD_PERCENT = 50;
    private static final int MAX_DAILY_AMOUNT = 4000;
    private static final long NO_RECORD_DAYS_THRESHOLD = 3;
    private final WaterRepository waterRepository;
    private final ProfileRepository profileRepository;

    @SuppressWarnings("null")
    public List<FeedbackItem> evaluate(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new com.healthlog.demo.exception.BusinessException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "プロフィールが見つかりません"));
        LocalDate today = LocalDate.now();
        List<FeedbackItem> result = new ArrayList<>();
        Optional<Water> lastEver = waterRepository.findTopByProfile_IdOrderByRecordedDateDesc(profileId);
        if (lastEver.isEmpty()) {
            result.add(buildNoRecordReminder(today, "水分記録がありません", "まだ水分データが記録されていません。記録を始めてみましょう。"));
            return result;
        }

        long daysSince = ChronoUnit.DAYS.between(lastEver.get().getRecordedDate(), today);
        if (daysSince >= NO_RECORD_DAYS_THRESHOLD) {
            result.add(new FeedbackItem(FeedbackType.WATER_NO_RECORD, FeedbackLevel.LV2,
                    "最近、水分記録がありません", "最後の記録から" + daysSince
                            + "日経っています。今日の水分摂取を記録してみましょう。",
                    lastEver.get().getRecordedDate().atStartOfDay(), "lightbulb"));
            return result;
        }

        List<Water> todayLogs = waterRepository.findByProfile_IdAndRecordedDateOrderByIdAsc(profileId, today);
        if (todayLogs.isEmpty()) {
            result.add(buildNoRecordReminder(today, "水分記録がありません",
                    "今日の水分摂取データがまだ記録されていません。記録すると、あなたに合ったフィードバックが受け取れます。"));
            return result;
        }

        Optional<Water> previous = waterRepository
                .findTopByProfile_IdAndRecordedDateLessThanOrderByRecordedDateDesc(profileId, today);
        if (previous.isPresent()) {
            long gap = ChronoUnit.DAYS.between(previous.get().getRecordedDate(), today);
            if (gap > 1) {
                result.add(new FeedbackItem(FeedbackType.WATER_RESUMED, FeedbackLevel.LV0,
                        "記録を再開しました", "前回の記録から" + (gap - 1)
                                + "日空きましたが、今日また記録できました。この調子で続けましょう。",
                        today.atStartOfDay(), "calendar-check"));
            }
        }

        int todayTotal = todayLogs.stream().mapToInt(Water::getAmountMl).sum();
        List<FeedbackItem> main = new ArrayList<>();
        checkTooMuchWater(todayTotal, today, main);
        if (main.isEmpty()) {
            Integer goal = profile.getWaterGoalMl();
            if (goal == null || goal <= 0) {
                main.add(new FeedbackItem(FeedbackType.WATER_NO_GOAL, FeedbackLevel.LV0,
                        "水分摂取の目標が設定されていません", "目標を設定すると、あなたに合ったフィードバックが受け取れます。",
                        today.atStartOfDay(), "target"));
            } else {
                checkLowWater(todayTotal, today, goal, main);
                if (main.isEmpty())
                    checkAlmostGoal(todayTotal, today, goal, main);
                if (main.isEmpty())
                    checkComplete(todayTotal, today, goal, main);
            }
        }
        result.addAll(main);
        return result;
    }

    private void checkTooMuchWater(int total, LocalDate today, List<FeedbackItem> items) {
        if (total < MAX_DAILY_AMOUNT)
            return;
        items.add(new FeedbackItem(FeedbackType.WATER_EXCESS, FeedbackLevel.LV4,
                "水分を摂りすぎています", "本日の摂取量は" + total + "mlです。必要以上の水分摂取には注意しましょう。",
                today.atStartOfDay(), "alert-octagon"));
    }

    private void checkLowWater(int total, LocalDate today, int goal, List<FeedbackItem> items) {
        int rate = (int) Math.round(total * 100.0 / goal);
        if (rate >= LOW_THRESHOLD_PERCENT)
            return;
        items.add(new FeedbackItem(FeedbackType.WATER_LOW, FeedbackLevel.LV3, "水分摂取が不足しています",
                "現在の摂取量は目標の" + rate + "%です。目標まであと" + (goal - total) + "mlです。",
                today.atStartOfDay(), "alert-triangle"));
    }

    private void checkAlmostGoal(int total, LocalDate today, int goal, List<FeedbackItem> items) {
        int rate = (int) Math.round(total * 100.0 / goal);
        if (rate < LOW_THRESHOLD_PERCENT || rate >= 100)
            return;
        String title = rate >= ALMOST_MIN_PERCENT ? "もう少しで目標達成です" : "目標に向けて順調です";
        items.add(new FeedbackItem(FeedbackType.WATER_ALMOST, FeedbackLevel.LV2, title,
                "現在 " + rate + "% 達成しています。目標まであと" + (goal - total) + "mlです。",
                today.atStartOfDay(), "lightbulb"));
    }

    private void checkComplete(int total, LocalDate today, int goal, List<FeedbackItem> items) {
        if (Math.round(total * 100.0 / goal) < 100)
            return;
        items.add(new FeedbackItem(FeedbackType.WATER_COMPLETE, FeedbackLevel.LV1, "水分目標を達成しました",
                "本日の水分摂取量は" + total + "mlです。設定した水分目標を達成しました！",
                today.atStartOfDay(), "check-circle"));
    }

    private FeedbackItem buildNoRecordReminder(LocalDate today, String title, String message) {
        return new FeedbackItem(FeedbackType.WATER_NO_RECORD, FeedbackLevel.LV0,
                title, message, today.atStartOfDay(), "calendar-x");
    }

}
