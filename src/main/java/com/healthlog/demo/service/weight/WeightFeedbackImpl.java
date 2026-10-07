package com.healthlog.demo.service.weight;

import java.util.List;
import org.springframework.stereotype.Service;
import com.healthlog.demo.dto.feedback.FeedbackItem;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeightFeedbackImpl implements WeightFeedback {

    private final WeightFeedbackRule weightFeedbackRule;

    @Override
    public List<FeedbackItem> evaluate(Long profileId) {
        return weightFeedbackRule.evaluate(profileId);
    }
}