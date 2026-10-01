package com.healthlog.demo.service.sleep;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.dto.feedback.FeedbackLevel;
import com.healthlog.demo.dto.feedback.FeedbackType;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.SleepRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SleepFeedbackRule {
    private static final int LV3_LOOKBACK_DAYS = 3;
    private static final int SHORT_SLEEP_MINUTES_THRESHOLD = 5 * 60;
    private static final int LV4_CONSECUTIVE_DAYS = 5;
    private static final int LV4_DAILY_MINUTES_THRESHOLD = 6 * 60;
    private static final long NO_RECORD_DAYS_THRESHOLD = 3;
    private final SleepRepository sleepRepository;
    private final ProfileRepository profileRepository;

    public List<FeedbackItem> evaluate(Long profileId) {
        Profile profile = profileRepository.findById(profileId).orElseThrow();
        LocalDate today = LocalDate.now();
        List<FeedbackItem> result = new ArrayList<>();
        Optional<Sleep> lastEver = sleepRepository.findTopByProfile_IdOrderByRecordedDateDesc(profileId);
        if (lastEver.isEmpty()) {
            result.add(buildReminder(today, "睡眠記録がありません", "まだ睡眠データが記録されていません。記録を始めてみましょう。"));
            return result;
        }

        LocalDate lastRecordedDate = lastEver.get().getRecordedDate();
        long daysSinceLast = ChronoUnit.DAYS.between(lastRecordedDate, today);
        LocalDate lookbackFrom = lastRecordedDate.minusDays(LV4_CONSECUTIVE_DAYS - 1L);
        List<Sleep> recentLogs = sleepRepository
                .findByProfile_IdAndRecordedDateGreaterThanEqualOrderByRecordedDateDesc(profileId, lookbackFrom);
        Map<LocalDate, Integer> dailyMinutes = aggregateDaily(recentLogs);

        List<FeedbackItem> mainFeedback = new ArrayList<>();
        checkContinuousShortSleep(dailyMinutes, lastRecordedDate, mainFeedback);

        if (daysSinceLast >= NO_RECORD_DAYS_THRESHOLD) {
            result.add(new FeedbackItem(FeedbackType.SLEEP_NO_RECORD, FeedbackLevel.LV2,
                    "最近、睡眠記録がありません", "最後の記録から" + daysSinceLast + "日経っています。今日の睡眠を記録してみましょう。",
                    lastRecordedDate.atStartOfDay(), "lightbulb"));
            result.addAll(mainFeedback);
            return result;
        }

        if (!dailyMinutes.containsKey(today)) {
            result.add(buildReminder(today, "睡眠記録がありません",
                    "今日の睡眠データがまだ記録されていません。記録すると、あなたに合ったフィードバックが受け取れます。"));
            result.addAll(mainFeedback);
            return result;
        }

        Optional<Sleep> previous = sleepRepository
                .findTopByProfile_IdAndRecordedDateLessThanOrderByRecordedDateDesc(profileId, today);
        if (previous.isPresent()) {
            long gap = ChronoUnit.DAYS.between(previous.get().getRecordedDate(), today);
            if (gap > 1) {
                result.add(new FeedbackItem(FeedbackType.SLEEP_RESUMED, FeedbackLevel.LV0,
                        "記録を再開しました", "前回の記録から" + (gap - 1)
                                + "日空きましたが、今日また記録できました。この調子で続けましょう。",
                        today.atStartOfDay(), "calendar-check"));
            }
        }

        if (mainFeedback.isEmpty())
            checkShortAverageSleep(dailyMinutes, today, mainFeedback);
        if (mainFeedback.isEmpty()) {
            int goalMinutes = profile.getDailySleepGoal() != null && profile.getDailySleepGoal().signum() > 0
                    ? profile.getDailySleepGoal().multiply(java.math.BigDecimal.valueOf(60)).intValue()
                    : 0;
            if (goalMinutes <= 0) {
                mainFeedback.add(new FeedbackItem(FeedbackType.SLEEP_NO_GOAL, FeedbackLevel.LV0,
                        "睡眠の目標が設定されていません", "目標を設定すると、あなたに合ったフィードバックが受け取れます。",
                        today.atStartOfDay(), "target"));
            } else {
                checkSleepGoalRate(dailyMinutes, today, goalMinutes, mainFeedback);
            }
        }
        result.addAll(mainFeedback);
        return result;
    }

    private void checkContinuousShortSleep(Map<LocalDate, Integer> dailyTotals, LocalDate referenceDate,
            List<FeedbackItem> items) {
        for (int offset = 0; offset < LV4_CONSECUTIVE_DAYS; offset++) {
            Integer minutes = dailyTotals.get(referenceDate.minusDays(offset));
            if (minutes == null || minutes >= LV4_DAILY_MINUTES_THRESHOLD)
                return;
        }
        items.add(new FeedbackItem(FeedbackType.SLEEP_CONTINUOUS_SHORT, FeedbackLevel.LV4,
                "睡眠不足が続いています", LV4_CONSECUTIVE_DAYS
                        + "日連続で6時間未満の睡眠です。十分な休息を取ることをおすすめします。",
                referenceDate.atStartOfDay(), "alert-octagon"));
    }

    @SuppressWarnings("null")
    private void checkShortAverageSleep(Map<LocalDate, Integer> dailyTotals, LocalDate today,
            List<FeedbackItem> items) {
        List<Integer> recent = new ArrayList<>();
        for (int offset = 0; offset < LV3_LOOKBACK_DAYS; offset++) {
            Integer minutes = dailyTotals.get(today.minusDays(offset));
            if (minutes != null)
                recent.add(minutes);
        }
        if (recent.size() == LV3_LOOKBACK_DAYS
                && recent.stream().mapToInt(Integer::intValue).average().orElse(0) < SHORT_SLEEP_MINUTES_THRESHOLD) {
            items.add(new FeedbackItem(FeedbackType.SLEEP_SHORT, FeedbackLevel.LV3,
                    "睡眠不足が継続しています", "直近" + LV3_LOOKBACK_DAYS
                            + "日間の平均睡眠時間が5時間未満です。休息時間を見直しましょう。",
                    today.atStartOfDay(), "alert-triangle"));
            return;
        }
        Integer todayMinutes = dailyTotals.get(today);
        if (todayMinutes != null && todayMinutes < SHORT_SLEEP_MINUTES_THRESHOLD) {
            items.add(new FeedbackItem(FeedbackType.SLEEP_SHORT, FeedbackLevel.LV3,
                    "今日の睡眠時間が短いです", "今日の睡眠は" + (todayMinutes / 60) + "時間"
                            + (todayMinutes % 60) + "分でした。十分な休息を心がけましょう。",
                    today.atStartOfDay(), "alert-triangle"));
        }
    }

    private void checkSleepGoalRate(Map<LocalDate, Integer> dailyTotals, LocalDate today, int goalMinutes,
            List<FeedbackItem> items) {
        Integer minutes = dailyTotals.get(today);
        if (minutes == null || goalMinutes <= 0)
            return;
        double rate = minutes * 100.0 / goalMinutes;
        int displayRate = (int) Math.round(rate);
        int remaining = goalMinutes - minutes;
        if (rate < 50) {
            items.add(new FeedbackItem(FeedbackType.SLEEP_SHORT, FeedbackLevel.LV2,
                    "睡眠時間が目標よりかなり短いです", "現在の睡眠時間は目標の" + displayRate
                            + "%です。目標まであと" + formatMinutes(remaining) + "です。",
                    today.atStartOfDay(), "lightbulb"));
        } else if (rate < 100) {
            String title = rate >= 80 ? "もう少しで睡眠目標達成です" : "睡眠目標に向けて順調です";
            items.add(new FeedbackItem(FeedbackType.SLEEP_SHORT, FeedbackLevel.LV2, title,
                    "現在 " + displayRate + "% 達成しています。目標まであと" + formatMinutes(remaining) + "です。",
                    today.atStartOfDay(), "lightbulb"));
        } else {
            items.add(new FeedbackItem(FeedbackType.SLEEP_GOOD, FeedbackLevel.LV1,
                    "睡眠目標を達成しました", "今日の睡眠時間は" + (minutes / 60) + "時間" + (minutes % 60)
                            + "分です。設定した睡眠目標を達成しました！",
                    today.atStartOfDay(), "check-circle"));
        }
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours > 0 ? hours + "時間" + minutes + "分" : minutes + "分";
    }

    private FeedbackItem buildReminder(LocalDate date, String title, String message) {
        return new FeedbackItem(FeedbackType.SLEEP_NO_RECORD, FeedbackLevel.LV0,
                title, message, date.atStartOfDay(), "calendar-x");
    }

    @SuppressWarnings("null")
    private Map<LocalDate, Integer> aggregateDaily(List<Sleep> logs) {
        Map<LocalDate, Integer> totals = new HashMap<>();
        for (Sleep log : logs) {
            if (log.getSleepMinutes() != null) {
                totals.merge(log.getRecordedDate(), log.getSleepMinutes(), Integer::sum);
            }
        }
        return totals;
    }

}
