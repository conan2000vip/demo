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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeSummaryChartServiceImpl implements HomeSummaryChartService {

    private final WeightService weightService;
    private final SleepService sleepService;
    private final WaterService waterService;
    private final StepService stepService;

    @Override
    public Map<String, Object> getSummaryChartData(Long profileId, Long currentUserId, LocalDate from, LocalDate to) {
        DateRangerFilter range = new DateRangerFilter(from, to);

        Map<String, Object> weight = weightService.chartData(profileId, currentUserId, range);
        Map<String, Object> sleep = sleepService.chartData(profileId, currentUserId, range);
        Map<String, Object> water = waterService.chartData(profileId, currentUserId, range);
        Map<String, Object> step = stepService.chartData(profileId, currentUserId, range);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("chartMode", weight.get("chartMode"));
        result.put("labels", weight.get("labels"));
        result.put("weight", series(weight));
        result.put("sleep", series(sleep));
        result.put("water", series(water));
        result.put("step", series(step));
        return result;
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