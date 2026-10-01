package com.healthlog.demo.service.step;

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
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.StepRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StepFeedbackRule {
    private static final int ALMOST_MIN_PERCENT = 80;
    private static final int LOW_THRESHOLD_PERCENT = 50;
    private static final long NO_RECORD_DAYS_THRESHOLD = 3;
    private static final int LV4_SUDDEN_HIGH_STEPS = 60000;
    private static final int LV4_SUDDEN_STEP_DIFF = 40000;
    private final StepRepository stepRepository;
    private final ProfileRepository profileRepository;

    public List<FeedbackItem> evaluate(Long profileId) {
        Profile profile = profileRepository.findById(profileId).orElseThrow();
        LocalDate today = LocalDate.now();
        List<FeedbackItem> result = new ArrayList<>();
        Optional<Step> lastEver = stepRepository.findTopByProfile_IdOrderByRecordedDateDescIdDesc(profileId);
        if (lastEver.isEmpty()) {
            result.add(buildNoRecordReminder(today, "歩数記録がありません", "まだ歩数データが記録されていません。記録を始めてみましょう。"));
            return result;
        }

        long daysSince = ChronoUnit.DAYS.between(lastEver.get().getRecordedDate(), today);
        if (daysSince >= NO_RECORD_DAYS_THRESHOLD) {
            result.add(new FeedbackItem(FeedbackType.STEP_NO_RECORD, FeedbackLevel.LV2,
                    "最近、歩数記録がありません", "最後の記録から" + daysSince + "日経っています。今日の歩数を記録してみましょう。",
                    lastEver.get().getRecordedDate().atStartOfDay(), "lightbulb"));
            return result;
        }

        LocalDate yesterday = today.minusDays(1);
        List<Step> recentLogs = stepRepository
                .findByProfile_IdAndRecordedDateGreaterThanEqualOrderByRecordedDateDesc(profileId, yesterday);
        Map<LocalDate, Integer> dailyTotals = computeDailyTotals(recentLogs);
        Step todayLog = stepRepository.findFirstByProfile_IdAndRecordedDate(profileId, today).orElse(null);
        if (todayLog == null) {
            result.add(buildNoRecordReminder(today, "歩数記録がありません",
                    "今日の歩数データがまだ記録されていません。記録すると、あなたに合ったフィードバックが受け取れます。"));
            return result;
        }

        Optional<Step> previous = stepRepository
                .findTopByProfile_IdAndRecordedDateLessThanOrderByRecordedDateDesc(profileId, today);
        if (previous.isPresent()) {
            long gap = ChronoUnit.DAYS.between(previous.get().getRecordedDate(), today);
            if (gap > 1) {
                result.add(new FeedbackItem(FeedbackType.STEP_RESUMED, FeedbackLevel.LV0,
                        "記録を再開しました", "前回の記録から" + (gap - 1)
                                + "日空きましたが、今日また記録できました。この調子で続けましょう。",
                        today.atStartOfDay(), "calendar-check"));
            }
        }

        List<FeedbackItem> main = new ArrayList<>();
        checkSuddenStepChange(dailyTotals, today, yesterday, main);
        if (main.isEmpty()) {
            Integer goal = profile.getStepGoal();
            if (goal == null || goal <= 0) {
                main.add(new FeedbackItem(FeedbackType.STEP_NO_GOAL, FeedbackLevel.LV0,
                        "歩数の目標が設定されていません", "目標を設定すると、より詳しいフィードバックが受け取れます。",
                        today.atStartOfDay(), "target"));
            } else {
                checkLowSteps(dailyTotals, today, goal, main);
                if (main.isEmpty())
                    checkAlmostGoal(dailyTotals, today, goal, main);
                if (main.isEmpty())
                    checkComplete(dailyTotals, today, goal, main);
            }
        }
        result.addAll(main);
        return result;
    }

    private void checkSuddenStepChange(Map<LocalDate, Integer> totals, LocalDate today, LocalDate yesterday,
            List<FeedbackItem> items) {
        Integer todaySteps = totals.get(today);
        if (todaySteps == null)
            return;
        if (todaySteps >= LV4_SUDDEN_HIGH_STEPS) {
            items.add(new FeedbackItem(FeedbackType.STEP_SUDDEN_CHANGE, FeedbackLevel.LV4,
                    "急激な歩数変化があります", "本日の歩数（" + todaySteps
                            + "歩）が非常に大きいです。入力内容を確認してください。",
                    today.atStartOfDay(), "alert-octagon"));
            return;
        }
        Integer yesterdaySteps = totals.get(yesterday);
        if (yesterdaySteps != null && Math.abs(todaySteps - yesterdaySteps) >= LV4_SUDDEN_STEP_DIFF) {
            items.add(new FeedbackItem(FeedbackType.STEP_SUDDEN_CHANGE, FeedbackLevel.LV4,
                    "急激な歩数変化があります", "前日比で " + Math.abs(todaySteps - yesterdaySteps)
                            + "歩の大きな変化がありました。入力内容を確認してください。",
                    today.atStartOfDay(), "alert-octagon"));
        }
    }

    private void checkLowSteps(Map<LocalDate, Integer> totals, LocalDate today, int goal, List<FeedbackItem> items) {
        Integer total = totals.get(today);
        if (total == null)
            return;
        int rate = (int) Math.round(total * 100.0 / goal);
        if (rate < LOW_THRESHOLD_PERCENT) {
            items.add(new FeedbackItem(FeedbackType.STEP_LOW, FeedbackLevel.LV3, "歩数が少ないです",
                    "現在の歩数は目標の" + rate + "%です。目標まであと" + (goal - total) + "歩です。",
                    today.atStartOfDay(), "alert-triangle"));
        }
    }

    private void checkAlmostGoal(Map<LocalDate, Integer> totals, LocalDate today, int goal, List<FeedbackItem> items) {
        Integer total = totals.get(today);
        if (total == null)
            return;
        int rate = (int) Math.round(total * 100.0 / goal);
        if (rate < LOW_THRESHOLD_PERCENT || rate >= 100)
            return;
        String title = rate < ALMOST_MIN_PERCENT ? "目標達成に向けて順調です" : "もう少しで目標達成です";
        items.add(new FeedbackItem(FeedbackType.STEP_ALMOST, FeedbackLevel.LV2, title,
                "現在 " + rate + "% 達成しています。目標まであと" + (goal - total) + "歩です。",
                today.atStartOfDay(), "lightbulb"));
    }

    private void checkComplete(Map<LocalDate, Integer> totals, LocalDate today, int goal, List<FeedbackItem> items) {
        Integer total = totals.get(today);
        if (total == null || Math.round(total * 100.0 / goal) < 100)
            return;
        items.add(new FeedbackItem(FeedbackType.STEP_COMPLETE, FeedbackLevel.LV1, "歩数目標を達成しました",
                "本日の歩数は" + total + "歩です。設定した歩数目標を達成しました！",
                today.atStartOfDay(), "check-circle"));
    }

    private FeedbackItem buildNoRecordReminder(LocalDate today, String title, String message) {
        return new FeedbackItem(FeedbackType.STEP_NO_RECORD, FeedbackLevel.LV0,
                title, message, today.atStartOfDay(), "calendar-x");
    }

    @SuppressWarnings("null")
    private Map<LocalDate, Integer> computeDailyTotals(List<Step> logs) {
        Map<LocalDate, Integer> totals = new HashMap<>();
        for (Step log : logs)
            totals.merge(log.getRecordedDate(), log.getSteps(), Integer::sum);
        return totals;
    }
}