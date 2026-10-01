package com.healthlog.demo.service.sleep;

import java.util.List;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.feedback.FeedbackItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SleepFeedbackImpl implements SleepFeedback {
    private final SleepFeedbackRule sleepFeedbackRule;

    @Override
    public List<FeedbackItem> evaluate(Long profileId) {
        return sleepFeedbackRule.evaluate(profileId);
    }
}
