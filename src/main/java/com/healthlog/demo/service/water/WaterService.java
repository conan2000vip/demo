package com.healthlog.demo.service.water;

import java.util.Map;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Water;

public interface WaterService {
    Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page);

    Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange);

    Water create(Long profileId, Long currentUserId, Water input);

    Water update(Long profileId, Long currentUserId, Long logId, Water input);

    void delete(Long profileId, Long currentUserId, Long logId);
}
