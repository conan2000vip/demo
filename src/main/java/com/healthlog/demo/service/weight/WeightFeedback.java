package com.healthlog.demo.service.weight;

import java.util.List;
import com.healthlog.demo.dto.feedback.FeedbackItem;

public interface WeightFeedback {
    List<FeedbackItem> evaluate(Long profileId);
}