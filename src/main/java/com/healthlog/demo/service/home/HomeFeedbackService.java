package com.healthlog.demo.service.home;

import java.util.List;

import com.healthlog.demo.dto.feedback.FeedbackItem;

public interface HomeFeedbackService {
    List<FeedbackItem> getHomeFeedback(Long profileId);

    boolean collectSevereLabels(Long profileId, List<String> severeLabels);
}