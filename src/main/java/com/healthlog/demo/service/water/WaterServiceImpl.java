package com.healthlog.demo.service.water;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.WaterSummary;
import com.healthlog.demo.dto.chartdata.ChartDataResponse;
import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.BaseLog;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Water;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.Water.DrinkType;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.service.base.BaseLogService;
import com.healthlog.demo.service.helper.ChartDataBuilder;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

@Service
@Transactional
public class WaterServiceImpl extends BaseLogService<Water, Water> implements WaterService {
    private static final int PAGE_SIZE = 10;
    private final WaterRepository waterRepository;
    private final ChartDataBuilder chartDataBuilder;

    public WaterServiceImpl(WaterRepository waterRepository, ProfileAccessValidation profileAccessValidation,
            ChartDataBuilder chartDataBuilder) {
        super(waterRepository, profileAccessValidation);
        this.waterRepository = waterRepository;
        this.chartDataBuilder = chartDataBuilder;
    }

    @Override
    protected Water mapToDto(Water entity) {
        return entity;
    }

    @Override
    protected Category category() {
        return Category.water;
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page) {
        validatePageAndRange(dateRange, page);
        Profile profile = validateAndGetProfile(profileId, currentUserId);
        List<Water> filteredLogs = getLogs(profileId, currentUserId, dateRange);
        Page<Water> waterPage = getLogsPaged(profileId, currentUserId, dateRange, PageRequest.of(page, PAGE_SIZE));
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<Water> todayLogs = waterRepository.findByProfile_IdAndRecordedDateOrderByIdAsc(profileId, today);
        Integer todayTotal = todayLogs.isEmpty() ? null : todayLogs.stream().mapToInt(Water::getAmountMl).sum();
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<Water> monthLogs = waterRepository.findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDesc(
                profileId, monthStart, monthEnd);
        Map<LocalDate, Integer> monthDailyTotals = new TreeMap<>();
        monthLogs.forEach(log -> monthDailyTotals.merge(log.getRecordedDate(), log.getAmountMl(), Integer::sum));
        Integer monthAverage = monthDailyTotals.isEmpty() ? null
                : (int) Math.round(monthDailyTotals.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        Integer goal = profile.getWaterGoalMl();
        Integer goalRate = todayTotal != null && goal != null && goal > 0
                ? (int) Math.round(todayTotal * 100.0 / goal)
                : null;
        Water latest = waterRepository.findByProfile_IdOrderByRecordedDateDesc(profileId).stream()
                .max(Comparator.comparing(Water::getRecordedDate)
                        .thenComparing(Water::getRecordedTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Water::getId))
                .orElse(null);
        ChartDataResponse chart = chartDataBuilder.build(aggregateForChart(filteredLogs, dateRange), dateRange,
                WaterChartPoint::getChartAmount);

        Map<String, Object> stats = new HashMap<>();
        stats.put("latest", latest != null ? latest.getAmountMl() : null);
        stats.put("latestDate", latest != null ? latest.getRecordedDate() : null);
        stats.put("todayTotal", todayTotal);
        stats.put("todayDate", today);
        stats.put("monthAverage", monthAverage);
        stats.put("goalRate", goalRate);
        stats.put("todayGoal", goal);

        Map<String, Object> result = new HashMap<>();
        result.put("currentProfile", profile);
        result.put("logs", waterPage.getContent().stream().map(WaterSummary::from).toList());
        result.put("stats", stats);
        result.put("hasAnyLog", waterRepository.existsByProfile_Id(profileId));
        result.put("currentPage", waterPage.getNumber());
        result.put("totalPages", waterPage.getTotalPages());
        result.put("hasPrevious", waterPage.hasPrevious());
        result.put("hasNext", waterPage.hasNext());
        result.put("labels", chart.labels());
        result.put("values", chart.values());
        result.put("chartMode", chart.chartMode());
        result.put("chartFrom", dateRange != null ? dateRange.getFrom() : null);
        result.put("chartTo", dateRange != null ? dateRange.getTo() : null);
        result.put("filterStartDate", dateRange != null ? dateRange.getFrom() : null);
        result.put("filterEndDate", dateRange != null ? dateRange.getTo() : null);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange) {
        validateDateRange(dateRange);
        List<Water> logs = getLogs(profileId, currentUserId, dateRange);
        @SuppressWarnings("null")
        ChartDataResponse chart = chartDataBuilder.build(aggregateForChart(logs, dateRange), dateRange,
                WaterChartPoint::getChartAmount);
        return Map.of("labels", chart.labels(), "values", chart.values(), "chartMode", chart.chartMode());
    }

    @Override
    public Water create(Long profileId, Long currentUserId, Water input) {
        Profile profile = validateAndGetEditableProfile(profileId, currentUserId);
        validateWater(input);
        input.setProfile(profile);
        if (input.getRecordedTime() == null)
            input.setRecordedTime(LocalTime.now());
        input.setMeasuredAt(input.getRecordedDate().atTime(input.getRecordedTime()));
        return waterRepository.save(input);
    }

    @Override
    public Water update(Long profileId, Long currentUserId, Long logId, Water input) {
        validateAndGetEditableProfile(profileId, currentUserId);
        Water existing = findEntityByIdAndProfile(logId, profileId);
        validateWater(input);
        existing.setRecordedDate(input.getRecordedDate());
        existing.setRecordedTime(
                input.getRecordedTime() != null ? input.getRecordedTime() : existing.getRecordedTime());
        existing.setDrinkType(input.getDrinkType());
        existing.setAmountMl(input.getAmountMl());
        existing.setMemo(input.getMemo());
        existing.setMeasuredAt(existing.getRecordedDate().atTime(existing.getRecordedTime()));
        return waterRepository.save(existing);
    }

    @Override
    public void delete(Long profileId, Long currentUserId, Long logId) {
        deleteLog(logId, profileId, currentUserId);
    }

    private void validateWater(Water water) {
        if (water.getRecordedDate() == null || water.getRecordedDate().isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "日付を確認してください。未来の日付は指定できません。");
        }
        if (water.getAmountMl() < 1 || water.getAmountMl() > 5000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "水分量は1〜5000mlの範囲で入力してください。");
        }
        try {
            water.setDrinkType(DrinkType.valueOf(water.getDrinkType()).name());
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "飲み物を選択してください。");
        }
        if (water.getMemo() != null && water.getMemo().length() > 500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メモは500文字以内で入力してください。");
        }
    }

    private List<WaterChartPoint> aggregateForChart(List<Water> logs, DateRangerFilter range) {
        boolean hourly = range != null && range.getFrom() != null && range.getFrom().equals(range.getTo());
        Map<String, WaterChartPoint> grouped = new TreeMap<>();
        for (Water water : logs) {
            String key = water.getRecordedDate().toString();
            if (hourly) {
                LocalTime time = water.getRecordedTime() != null ? water.getRecordedTime()
                        : water.getMeasuredAt() != null ? water.getMeasuredAt().toLocalTime() : LocalTime.MIDNIGHT;
                key += String.format("T%02d", time.getHour());
            }
            WaterChartPoint point = grouped.computeIfAbsent(key, ignored -> {
                WaterChartPoint created = new WaterChartPoint();
                created.setId(water.getId());
                created.setRecordedDate(water.getRecordedDate());
                LocalTime measured = water.getRecordedTime() != null ? water.getRecordedTime()
                        : water.getMeasuredAt() != null ? water.getMeasuredAt().toLocalTime() : LocalTime.MIDNIGHT;
                created.setMeasuredAt(
                        water.getRecordedDate().atTime(hourly ? measured.withMinute(0).withSecond(0) : LocalTime.NOON));
                return created;
            });
            point.setChartAmount(point.getChartAmount().add(BigDecimal.valueOf(water.getAmountMl())));
            if (water.getId() != null && (point.getId() == null || water.getId() > point.getId()))
                point.setId(water.getId());
        }
        return new ArrayList<>(grouped.values());
    }

    private void validatePageAndRange(DateRangerFilter range, int page) {
        if (page < 0)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ページ番号が正しくありません。");
        validateDateRange(range);
    }

    private void validateDateRange(DateRangerFilter range) {
        if (range != null && range.getFrom() != null && range.getTo() != null
                && range.getFrom().isAfter(range.getTo())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "開始日が終了日より後になっています。");
        }
    }

    private static class WaterChartPoint extends BaseLog {
        private BigDecimal chartAmount = BigDecimal.ZERO;

        public BigDecimal getChartAmount() {
            return chartAmount;
        }

        public void setChartAmount(BigDecimal chartAmount) {
            this.chartAmount = chartAmount;
        }
    }
}
