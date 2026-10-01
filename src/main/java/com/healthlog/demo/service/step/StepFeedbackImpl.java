package com.healthlog.demo.service.step;

import java.util.List;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.feedback.FeedbackItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StepFeedbackImpl implements StepFeedback {
    private final StepFeedbackRule stepFeedbackRule;

    @Override
    public List<FeedbackItem> evaluate(Long profileId) {
        return stepFeedbackRule.evaluate(profileId);
    }
}
