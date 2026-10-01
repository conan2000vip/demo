package com.healthlog.demo.service.water;

import java.util.List;

import com.healthlog.demo.dto.feedback.FeedbackItem;

public interface WaterFeedback {
    List<FeedbackItem> evaluate(Long profileId);
}
