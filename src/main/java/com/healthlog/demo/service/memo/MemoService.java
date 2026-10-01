package com.healthlog.demo.service.memo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Memo;

public interface MemoService {
    Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page);

    Memo create(Long profileId, Long currentUserId, Memo input);

    Memo update(Long profileId, Long currentUserId, Long logId, Memo input);

    void delete(Long profileId, Long currentUserId, Long logId);

    List<Memo> getRecentThreeDays(Long profileId, Long currentUserId);

    List<Memo> getByDate(Long profileId, Long currentUserId, LocalDate recordedDate);
}
