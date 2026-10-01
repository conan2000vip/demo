package com.healthlog.demo.service.sleep;

import java.util.List;

import com.healthlog.demo.dto.feedback.FeedbackItem;

public interface SleepFeedback {
    List<FeedbackItem> evaluate(Long profileId);
}
