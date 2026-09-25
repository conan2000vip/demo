package com.healthlog.demo.service.home;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.entity.Water;
import com.healthlog.demo.entity.Weight;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.weight.WeightService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeSummaryServiceImpl implements HomeSummaryService {@Override
    public Map<String, Object> getSummaryChartData(Long profileId, Long currentUserId, LocalDate from, LocalDate to) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSummaryChartData'");
    }

	// private final WeightService weightService;
	// private final SleepService sleepService;
	// private final WaterService waterService;
	// private final StepService stepService;

	// @Override
	// public Map<String, Object> getSummaryChartData(Long profileId, Long currentUserId, LocalDate from, LocalDate to) {
	// 	Map<LocalDate, BigDecimal> weightMap = collectWeightDaily(profileId, currentUserId, from, to);
	// 	Map<LocalDate, Integer> sleepMap = collectSleepDaily(profileId, currentUserId, from, to);
	// 	Map<LocalDate, Integer> waterMap = collectWaterDaily(profileId, currentUserId, from, to);
	// 	Map<LocalDate, Integer> stepMap = collectStepDaily(profileId, currentUserId, from, to);

	// 	return buildResponse(from, to, weightMap, sleepMap, waterMap, stepMap);
	// }

	// =========================================================
	// 体重: 1日に複数記録がある場合は最終測定時刻のものを採用
	// =========================================================
	// private Map<LocalDate, BigDecimal> collectWeightDaily(Long profileId, Long currentUserId, LocalDate from,
	// 		LocalDate to) {
	// 	Map<LocalDate, BigDecimal> map = new HashMap<>();
	// 	Map<LocalDate, java.time.LocalDateTime> latestTimeMap = new HashMap<>();
	// 	try {
	// 		Map<String, Object> result = weightService.list(profileId, currentUserId, from, to, 0);
	// 		List<?> logs = (List<?>) result.get("logs");
	// 		if (logs != null) {
	// 			for (Object log : logs) {
	// 				if (log instanceof Weight w && w.getRecordedDate() != null && w.getWeight() != null) {
	// 					LocalDate date = w.getRecordedDate();
	// 					java.time.LocalDateTime existingTime = latestTimeMap.get(date);
	// 					if (existingTime == null || (w.getMeasuredAt() != null && w.getMeasuredAt().isAfter(existingTime))) {
	// 						map.put(date, w.getWeight());
	// 						latestTimeMap.put(date, w.getMeasuredAt());
	// 					}
	// 				}
	// 			}
	// 		}
	// 	} catch (BusinessException e) {
	// 		// データなし・権限なし等は空マップのまま返す
	// 	}
	// 	return map;
	// }

	// // =========================================================
	// // 睡眠: 1日の合計分数
	// // =========================================================
	// private Map<LocalDate, Integer> collectSleepDaily(Long profileId, Long currentUserId, LocalDate from,
	// 		LocalDate to) {
	// 	Map<LocalDate, Integer> map = new HashMap<>();
	// 	try {
	// 		Map<String, Object> result = sleepService.list(profileId, currentUserId, from, to, 0);
	// 		List<?> logs = (List<?>) result.get("logs");
	// 		if (logs != null) {
	// 			for (Object log : logs) {
	// 				if (log instanceof Sleep s && s.getRecordedDate() != null && s.getSleepMinutes() != null) {
	// 					map.merge(s.getRecordedDate(), s.getSleepMinutes(), Integer::sum);
	// 				}
	// 			}
	// 		}
	// 	} catch (BusinessException e) {
	// 	}
	// 	return map;
	// }

	// // =========================================================
	// // 水分: 1日の合計ml
	// // =========================================================
	// private Map<LocalDate, Integer> collectWaterDaily(Long profileId, Long currentUserId, LocalDate from,
	// 		LocalDate to) {
	// 	Map<LocalDate, Integer> map = new HashMap<>();
	// 	try {
	// 		Map<String, Object> result = waterService.list(profileId, currentUserId, from, to, 0);
	// 		List<?> logs = (List<?>) result.get("logs");
	// 		if (logs != null) {
	// 			for (Object log : logs) {
	// 				if (log instanceof Water w && w.getRecordedDate() != null && w.getAmountMl() != null) {
	// 					map.merge(w.getRecordedDate(), w.getAmountMl(), Integer::sum);
	// 				}
	// 			}
	// 		}
	// 	} catch (BusinessException e) {
	// 	}
	// 	return map;
	// }

	// // =========================================================
	// // 歩数: 1日1件（schema上UNIQUE制約あり）
	// // =========================================================
	// private Map<LocalDate, Integer> collectStepDaily(Long profileId, Long currentUserId, LocalDate from,
	// 		LocalDate to) {
	// 	Map<LocalDate, Integer> map = new HashMap<>();
	// 	try {
	// 		Map<String, Object> result = stepService.list(profileId, currentUserId, from, to, 0);
	// 		List<?> logs = (List<?>) result.get("logs");
	// 		if (logs != null) {
	// 			for (Object log : logs) {
	// 				if (log instanceof Step s && s.getRecordedDate() != null && s.getSteps() != null) {
	// 					map.put(s.getRecordedDate(), s.getSteps());
	// 				}
	// 			}
	// 		}
	// 	} catch (BusinessException e) {
	// 	}
	// 	return map;
	// }

	// =========================================================
	// 日付軸に沿ってレスポンス形式を組み立てる
	// =========================================================
	// private Map<String, Object> buildResponse(LocalDate from, LocalDate to,
	// 		Map<LocalDate, BigDecimal> weightMap, Map<LocalDate, Integer> sleepMap,
	// 		Map<LocalDate, Integer> waterMap, Map<LocalDate, Integer> stepMap) {

		// List<String> labels = new ArrayList<>();
		// List<BigDecimal> weightValues = new ArrayList<>();
		// List<Integer> sleepValues = new ArrayList<>();
		// List<Integer> waterValues = new ArrayList<>();
		// List<Integer> stepValues = new ArrayList<>();

		// LocalDate cursor = from;
		// while (!cursor.isAfter(to)) {
		// 	labels.add(cursor.toString());
		// 	weightValues.add(weightMap.get(cursor));
		// 	sleepValues.add(sleepMap.get(cursor)); // 分単位のまま返す（時間換算はフロント側）
		// 	waterValues.add(waterMap.get(cursor));
		// 	stepValues.add(stepMap.get(cursor));
		// 	cursor = cursor.plusDays(1);
		// }

	// 	Map<String, Object> response = new HashMap<>();
	// 	response.put("labels", labels);
	// 	response.put("weight", weightValues);
	// 	response.put("sleep", sleepValues);
	// 	response.put("water", waterValues);
	// 	response.put("step", stepValues);
	// 	return response;
	// }
}