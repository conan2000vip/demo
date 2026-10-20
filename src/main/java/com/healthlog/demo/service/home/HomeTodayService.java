package com.healthlog.demo.service.home;

import java.util.Map;

public interface HomeTodayService {
    Map<String, Object> buildToday(Long profileId, Long currentUserId);
}