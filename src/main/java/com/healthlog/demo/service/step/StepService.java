package com.healthlog.demo.service.step;

import java.util.Map;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Step;

public interface StepService {
    Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page);

    Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange);

    Step create(Long profileId, Long currentUserId, Step input);

    Step update(Long profileId, Long currentUserId, Long logId, Step input);

    void delete(Long profileId, Long currentUserId, Long logId);
}