package com.healthlog.demo.service.home;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.service.sleep.SleepService;
import com.healthlog.demo.service.step.StepService;
import com.healthlog.demo.service.water.WaterService;
import com.healthlog.demo.service.weight.WeightService;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeSummaryChartServiceImpl implements HomeSummaryChartService {

    private final WeightService weightService;
    private final SleepService sleepService;
    private final WaterService waterService;
    private final StepService stepService;
    private final ProfileAccessValidation profileAccessValidation;

    @Override
    public Map<String, Object> getSummaryChartData(Long profileId, Long currentUserId, LocalDate from, LocalDate to) {
        DateRangerFilter range = new DateRangerFilter(from, to);

        Map<String, Object> weight = chartOrEmpty(profileId, currentUserId, range, Category.weight);
        Map<String, Object> sleep = chartOrEmpty(profileId, currentUserId, range, Category.sleep);
        Map<String, Object> water = chartOrEmpty(profileId, currentUserId, range, Category.water);
        Map<String, Object> step = chartOrEmpty(profileId, currentUserId, range, Category.step);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        List<Map<String, Object>> charts = List.of(weight, sleep, water, step);
        result.put("chartMode", firstAvailable("chartMode", charts));
        result.put("labels", firstAvailable("labels", charts));
        result.put("weight", series(weight));
        result.put("sleep", series(sleep));
        result.put("water", series(water));
        result.put("step", series(step));
        return result;
    }

    private Map<String, Object> chartOrEmpty(Long profileId, Long currentUserId, DateRangerFilter range,
            Category category) {
        if (!profileAccessValidation.hasReadAccess(profileId, currentUserId, category)) {
            return Map.of("labels", List.of(), "values", List.of(), "chartMode", "EMPTY");
        }
        return switch (category) {
        case weight -> weightService.chartData(profileId, currentUserId, range);
        case sleep -> sleepService.chartData(profileId, currentUserId, range);
        case water -> waterService.chartData(profileId, currentUserId, range);
        case step -> stepService.chartData(profileId, currentUserId, range);
        case memo -> Map.of("labels", List.of(), "values", List.of(), "chartMode", "EMPTY");
        };
    }

    private Object firstAvailable(String key, List<Map<String, Object>> charts) {
        for (Map<String, Object> chart : charts) {
            Object value = chart.get(key);
            if (value != null && (!(value instanceof List<?> list) || !list.isEmpty())) {
                return value;
            }
        }
        return charts.isEmpty() ? null : charts.get(0).get(key);
    }

    /** values: そのまま / first・last: 値のある最初と最後 / diff: last - first */
    @SuppressWarnings("null")
    private Map<String, Object> series(Map<String, Object> chartData) {
        List<?> values = (List<?>) chartData.get("values");
        BigDecimal first = null;
        BigDecimal last = null;
        if (values != null) {
            for (Object v : values) {
                if (v instanceof BigDecimal b) {
                    if (first == null)
                        first = b;
                    last = b;
                }
            }
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("values", values);
        s.put("first", first);
        s.put("last", last);
        s.put("diff", first != null ? last.subtract(first) : null);
        return s;
    }
}