package com.healthlog.demo.service.step;

import java.util.List;

import com.healthlog.demo.dto.feedback.FeedbackItem;

public interface StepFeedback {
    List<FeedbackItem> evaluate(Long profileId);
}
