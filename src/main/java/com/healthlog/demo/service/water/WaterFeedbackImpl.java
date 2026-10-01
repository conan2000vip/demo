package com.healthlog.demo.service.water;

import java.util.List;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.feedback.FeedbackItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WaterFeedbackImpl implements WaterFeedback {
    private final WaterFeedbackRule waterFeedbackRule;

    @Override
    public List<FeedbackItem> evaluate(Long profileId) {
        return waterFeedbackRule.evaluate(profileId);
    }
}
