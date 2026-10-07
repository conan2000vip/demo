package com.healthlog.demo.service.weight;

import java.util.Map;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Weight;

public interface WeightService {

    Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange);

    Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page);

    Weight create(Long profileId, Long currentUserId, Weight weight);

    Weight update(Long profileId, Long currentUserId, Long logId, Weight input);

    void delete(Long profileId, Long currentUserId, Long logId);
}
