package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.util.Map;

public interface HomeSummaryService {
	Map<String, Object> getSummaryChartData(Long profileId, Long currentUserId, LocalDate from, LocalDate to);
}