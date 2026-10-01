package com.healthlog.demo.service.sleep;

import java.util.Map;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Sleep;

public interface SleepService {
    Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page);

    Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange);

    Sleep create(Long profileId, Long currentUserId, Sleep input);

    Sleep update(Long profileId, Long currentUserId, Long logId, Sleep input);

    void delete(Long profileId, Long currentUserId, Long logId);
}
