package com.healthlog.demo.service.feedbackservice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.dto.feedback.FeedbackLevel;
import com.healthlog.demo.dto.feedback.FeedbackType;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;
import com.healthlog.demo.service.sleep.SleepFeedback;
import com.healthlog.demo.service.step.StepFeedback;
import com.healthlog.demo.service.water.WaterFeedback;
import com.healthlog.demo.service.weight.WeightFeedback;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private static final int[] HEALTH_STREAK_MILESTONES = { 7, 14, 30, 60, 90, 180, 365 };
    private final WeightFeedback weightFeedback;
    private final SleepFeedback sleepFeedback;
    private final WaterFeedback waterFeedback;
    private final StepFeedback stepFeedback;
    private final WeightRepository weightRepository;
    private final SleepRepository sleepRepository;
    private final WaterRepository waterRepository;
    private final StepRepository stepRepository;

    // ===== Màn hình riêng: mỗi loại chỉ 1 thẻ có mức cao nhất =====
    public List<FeedbackItem> getWeightFeedback(Long profileId) {
        return pickTop(weightFeedback.evaluate(profileId));
    }

    public List<FeedbackItem> getSleepFeedback(Long profileId) {
        return pickTop(sleepFeedback.evaluate(profileId));
    }

    public List<FeedbackItem> getWaterFeedback(Long profileId) {
        return pickTop(waterFeedback.evaluate(profileId));
    }

    public List<FeedbackItem> getStepFeedback(Long profileId) {
        return pickTop(stepFeedback.evaluate(profileId));
    }

    // ===== Home: gộp thẻ của 4 màn hình + streak, sắp xếp giảm dần =====
    public List<FeedbackItem> getHomeFeedback(Long profileId) {
        List<FeedbackItem> items = new ArrayList<>();
        items.addAll(getWeightFeedback(profileId));
        items.addAll(getSleepFeedback(profileId));
        items.addAll(getWaterFeedback(profileId));
        items.addAll(getStepFeedback(profileId));
        FeedbackItem streak = checkHealthRecordStreak(profileId);
        if (streak != null)
            items.add(streak);
        return sortByPriority(items);
    }

    /** Giữ 1 thẻ: mức cao nhất, cùng mức thì mới nhất */
    @SuppressWarnings("null")
    private List<FeedbackItem> pickTop(List<FeedbackItem> items) {
        return items.stream().max(Comparator.comparingInt((FeedbackItem i) -> i.getLevel().getPriority())
                .thenComparing(FeedbackItem::getOccurredAt)).map(List::of).orElse(List.of());
    }

    private int calculateStreak(Long profileId, LocalDate today) {
        int streak = 0;
        for (LocalDate date = today;; date = date.minusDays(1)) {
            boolean hasWeight = weightRepository.existsByProfile_IdAndRecordedDate(profileId, date);
            boolean hasSleep = sleepRepository.existsByProfile_IdAndRecordedDate(profileId, date);
            boolean hasWater = waterRepository.existsByProfile_IdAndRecordedDate(profileId, date);
            boolean hasStep = stepRepository.existsByProfile_IdAndRecordedDate(profileId, date);
            if (!hasWeight || !hasSleep || !hasWater || !hasStep)
                break;
            streak++;
        }
        return streak;
    }

    private FeedbackItem checkHealthRecordStreak(Long profileId) {
        LocalDate today = LocalDate.now();
        int streak = calculateStreak(profileId, today);
        for (int milestone : HEALTH_STREAK_MILESTONES) {
            if (streak == milestone)
                return createStreakFeedback(streak, today);
        }
        return null;
    }

    private FeedbackItem createStreakFeedback(int streak, LocalDate today) {
        String title;
        String message;
        switch (streak) {
        case 7 -> {
            title = "7日間連続で健康記録を続けています！";
            message = "1週間、毎日健康記録を続けることができました！\n素晴らしい習慣の第一歩です。";
        }
        case 14 -> {
            title = "14日間連続で健康記録を続けています！";
            message = "2週間、毎日健康記録を続けています。\n素晴らしい習慣が身についてきました！";
        }
        case 30 -> {
            title = "30日間連続で健康記録を続けています！";
            message = "1か月間、健康記録を続けることができました！\n毎日の積み重ねが素晴らしいです。";
        }
        case 60 -> {
            title = "60日間連続で健康記録を続けています！";
            message = "2か月間、健康記録を続けています。\n継続する力が素晴らしいです！";
        }
        case 90 -> {
            title = "90日間連続で健康記録を続けています！";
            message = "3か月間、健康記録を続けることができました！\n素晴らしい継続です！";
        }
        case 180 -> {
            title = "180日間連続で健康記録を続けています！";
            message = "半年間、健康記録を続けています。\n毎日の積み重ねが大きな習慣になっています！";
        }
        case 365 -> {
            title = "365日間連続で健康記録を続けています！";
            message = "1年間、毎日健康記録を続けることができました！\n本当に素晴らしい継続です！";
        }
        default -> {
            return null;
        }
        }
        return new FeedbackItem(FeedbackType.HEALTH_STREAK, FeedbackLevel.LV1, title, message, today.atStartOfDay(),
                "trophy");
    }

    @SuppressWarnings("null")
    private List<FeedbackItem> sortByPriority(List<FeedbackItem> items) {
        return items.stream().sorted(Comparator.comparingInt((FeedbackItem item) -> item.getLevel().getPriority())
                .reversed().thenComparing(FeedbackItem::getOccurredAt, Comparator.reverseOrder())).toList();
    }
}