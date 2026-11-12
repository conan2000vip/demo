package com.healthlog.demo.service.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.dto.feedback.FeedbackLevel;
import com.healthlog.demo.dto.feedback.FeedbackType;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.feedbackservice.FeedbackService;

class HomeFeedbackServiceImplTest {

    @Test
    @DisplayName("閲覧可能カテゴリだけから重大feedbackを収集する")
    void collectsSevereFeedbackOnlyFromVisibleCategories() {
        FeedbackService feedbackService = mock(FeedbackService.class);
        HomeFeedbackServiceImpl service = new HomeFeedbackServiceImpl(feedbackService);
        FeedbackItem weightAlert = new FeedbackItem(
                FeedbackType.WEIGHT_SUDDEN_CHANGE,
                FeedbackLevel.LV4,
                "急激な体重変化があります",
                "確認してください",
                LocalDateTime.now(),
                "alert-octagon");
        when(feedbackService.getWeightFeedback(10L)).thenReturn(List.of(weightAlert));
        List<String> labels = new ArrayList<>();

        boolean hasLv4 = service.collectSevereLabels(
                10L, labels, EnumSet.of(Category.weight));

        assertTrue(hasLv4);
        assertEquals(List.of("体重変化大"), labels);
        verify(feedbackService).getWeightFeedback(10L);
        verify(feedbackService, never()).getSleepFeedback(10L);
        verify(feedbackService, never()).getWaterFeedback(10L);
        verify(feedbackService, never()).getStepFeedback(10L);
    }
}
