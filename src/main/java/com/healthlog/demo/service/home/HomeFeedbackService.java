package com.healthlog.demo.service.home;

import java.util.List;
import java.util.Set;

import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.entity.ProfileShareSetting.Category;

public interface HomeFeedbackService {
    List<FeedbackItem> getHomeFeedback(Long profileId);

    boolean collectSevereLabels(Long profileId, List<String> severeLabels, Set<Category> visibleCategories);
}