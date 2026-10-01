package com.healthlog.demo.service.home;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.dto.feedback.FeedbackLevel;
import com.healthlog.demo.dto.feedback.FeedbackType;
import com.healthlog.demo.service.feedbackservice.FeedbackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeFeedbackServiceImpl implements HomeFeedbackService {

    private static final Map<FeedbackType, String> SEVERE_LABELS = Map.of(FeedbackType.WEIGHT_SUDDEN_CHANGE, "体重変化大",
            FeedbackType.WEIGHT_BIG_CHANGE, "体重変化大", FeedbackType.SLEEP_CONTINUOUS_SHORT, "睡眠不足が継続",
            FeedbackType.SLEEP_SHORT, "睡眠不足", FeedbackType.WATER_EXCESS, "水分過剰", FeedbackType.WATER_LOW, "水分不足",
            FeedbackType.STEP_LOW, "歩数不足");

    private final FeedbackService feedbackService;

    @Override
    public List<FeedbackItem> getHomeFeedback(Long profileId) {
        return feedbackService.getHomeFeedback(profileId);
    }

    @Override
    public boolean collectSevereLabels(Long profileId, List<String> severeLabels) {
        List<FeedbackItem> all = new ArrayList<>();
        all.addAll(feedbackService.getWeightFeedback(profileId));
        all.addAll(feedbackService.getSleepFeedback(profileId));
        all.addAll(feedbackService.getWaterFeedback(profileId));
        all.addAll(feedbackService.getStepFeedback(profileId));

        List<FeedbackItem> severe = all.stream()
                .filter(i -> i.getLevel() != null && i.getLevel().getPriority() >= FeedbackLevel.LV3.getPriority())
                .sorted(Comparator.comparingInt((FeedbackItem i) -> i.getLevel().getPriority()).reversed()).toList();

        boolean hasLv4 = false;
        for (FeedbackItem item : severe) {
            if (item.getLevel() == FeedbackLevel.LV4) {
                hasLv4 = true;
            }
            String label = SEVERE_LABELS.getOrDefault(item.getType(), item.getTitle());
            if (!severeLabels.contains(label)) {
                severeLabels.add(label);
            }
        }
        return hasLv4;
    }
}