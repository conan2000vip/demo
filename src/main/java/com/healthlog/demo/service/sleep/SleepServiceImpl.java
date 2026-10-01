package com.healthlog.demo.service.sleep;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.chartdata.ChartDataResponse;
import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.entity.Sleep.SleepType;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.service.base.BaseLogService;
import com.healthlog.demo.service.helper.ChartDataBuilder;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

@Service
@Transactional
public class SleepServiceImpl extends BaseLogService<Sleep, Sleep> implements SleepService {
    private static final int PAGE_SIZE = 10;
    private final SleepRepository sleepRepository;
    private final ChartDataBuilder chartDataBuilder;

    public SleepServiceImpl(SleepRepository sleepRepository, ProfileAccessValidation profileAccessValidation,
            ChartDataBuilder chartDataBuilder) {
        super(sleepRepository, profileAccessValidation);
        this.sleepRepository = sleepRepository;
        this.chartDataBuilder = chartDataBuilder;
    }

    @Override
    protected Sleep mapToDto(Sleep entity) {
        return entity;
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page) {
        validatePageAndRange(dateRange, page);
        Profile profile = validateAndGetProfile(profileId, currentUserId);
        List<Sleep> filteredLogs = getLogs(profileId, currentUserId, dateRange);
        Page<Sleep> sleepPage = getLogsPaged(profileId, currentUserId, dateRange, PageRequest.of(page, PAGE_SIZE));

        boolean customRange = dateRange != null && (dateRange.getFrom() != null || dateRange.getTo() != null);
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        DateRangerFilter summaryRange = customRange ? dateRange
                : new DateRangerFilter(monthStart, monthEnd);
        List<Sleep> summaryLogs = customRange ? filteredLogs
                : getLogs(profileId, currentUserId, summaryRange);
        Map<LocalDate, Sleep> dailyFiltered = aggregateDaily(filteredLogs);
        Map<LocalDate, Sleep> dailySummary = aggregateDaily(summaryLogs);
        Sleep latestLog = sleepRepository.findTopByProfile_IdOrderByRecordedDateDesc(profileId).orElse(null);
        Sleep shortestLog = dailySummary.values().stream()
                .min(Comparator.comparing(Sleep::getSleepMinutes)).orElse(null);
        Sleep longestLog = dailySummary.values().stream()
                .max(Comparator.comparing(Sleep::getSleepMinutes)).orElse(null);

        int totalMinutes = dailySummary.values().stream().mapToInt(Sleep::getSleepMinutes).sum();
        int dayCount = dailySummary.size();
        Integer averageMinutes = dayCount == 0 ? null
                : BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(dayCount), 0, RoundingMode.HALF_UP)
                        .intValue();
        List<Sleep> chartLogs = isSingleDay(dateRange)
                ? aggregateHourly(filteredLogs, dateRange.getFrom())
                : new ArrayList<>(dailyFiltered.values());
        ChartDataResponse chart = chartDataBuilder.build(chartLogs, dateRange,
                log -> BigDecimal.valueOf(log.getSleepMinutes()).divide(BigDecimal.valueOf(60), 2,
                        RoundingMode.HALF_UP));

        Map<String, Object> stats = new HashMap<>();
        stats.put("latest", latestLog != null ? latestLog.getSleepMinutes() : null);
        stats.put("latestDate", latestLog != null ? latestLog.getRecordedDate() : null);
        stats.put("monthlyAverage", averageMinutes);
        stats.put("shortest", shortestLog != null ? shortestLog.getSleepMinutes() : 0);
        stats.put("shortestDate", shortestLog != null ? shortestLog.getRecordedDate() : null);
        stats.put("longest", longestLog != null ? longestLog.getSleepMinutes() : 0);
        stats.put("longestDate", longestLog != null ? longestLog.getRecordedDate() : null);
        stats.put("isCustomRange", customRange);

        Map<String, Object> result = new HashMap<>();
        result.put("currentProfile", profile);
        result.put("logs", sleepPage.getContent());
        result.put("stats", stats);
        result.put("hasAnyLog", sleepRepository.existsByProfile_Id(profileId));
        result.put("currentPage", sleepPage.getNumber());
        result.put("totalPages", sleepPage.getTotalPages());
        result.put("hasPrevious", sleepPage.hasPrevious());
        result.put("hasNext", sleepPage.hasNext());
        result.put("labels", chart.labels());
        result.put("values", chart.values());
        result.put("chartMode", chart.chartMode());
        result.put("chartFrom", dateRange != null ? dateRange.getFrom() : null);
        result.put("chartTo", dateRange != null ? dateRange.getTo() : null);
        result.put("filterStartDate", dateRange != null ? dateRange.getFrom() : null);
        result.put("filterEndDate", dateRange != null ? dateRange.getTo() : null);
        return result;
    }

    private Map<LocalDate, Sleep> aggregateDaily(List<Sleep> logs) {
        Map<LocalDate, Sleep> daily = new LinkedHashMap<>();
        for (Sleep log : logs) {
            if (log.getSleepMinutes() == null)
                continue;
            Sleep total = daily.get(log.getRecordedDate());
            if (total == null) {
                total = new Sleep();
                total.setId(log.getId());
                total.setProfile(log.getProfile());
                total.setRecordedDate(log.getRecordedDate());
                total.setMeasuredAt(log.getMeasuredAt());
                total.setSleepMinutes(log.getSleepMinutes());
                daily.put(log.getRecordedDate(), total);
            } else {
                total.setSleepMinutes(total.getSleepMinutes() + log.getSleepMinutes());
                if (log.getId() > total.getId()) {
                    total.setId(log.getId());
                    total.setMeasuredAt(log.getMeasuredAt());
                }
            }
        }
        return daily;
    }

    private boolean isSingleDay(DateRangerFilter range) {
        return range != null && range.getFrom() != null && range.getFrom().equals(range.getTo());
    }

    private List<Sleep> aggregateHourly(List<Sleep> logs, LocalDate date) {
        Map<Integer, Sleep> hourly = new java.util.TreeMap<>();
        for (Sleep log : logs) {
            if (log.getStartTime() == null || log.getSleepMinutes() == null)
                continue;
            int hour = log.getStartTime().getHour();
            Sleep bucket = hourly.get(hour);
            if (bucket == null) {
                bucket = new Sleep();
                bucket.setId(log.getId());
                bucket.setRecordedDate(date);
                bucket.setMeasuredAt(date.atTime(hour, 0));
                bucket.setSleepMinutes(log.getSleepMinutes());
                hourly.put(hour, bucket);
            } else {
                bucket.setSleepMinutes(bucket.getSleepMinutes() + log.getSleepMinutes());
                if (log.getId() > bucket.getId()) {
                    bucket.setId(log.getId());
                }
            }
        }
        return new ArrayList<>(hourly.values());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange) {
        validateDateRange(dateRange);
        List<Sleep> logs = getLogs(profileId, currentUserId, dateRange);
        List<Sleep> chartLogs = isSingleDay(dateRange) ? aggregateHourly(logs, dateRange.getFrom())
                : new ArrayList<>(aggregateDaily(logs).values());
        ChartDataResponse chart = chartDataBuilder.build(chartLogs, dateRange,
                log -> BigDecimal.valueOf(log.getSleepMinutes()).divide(BigDecimal.valueOf(60), 2,
                        RoundingMode.HALF_UP));
        return Map.of("labels", chart.labels(), "values", chart.values(), "chartMode", chart.chartMode());
    }

    @Override
    public Sleep create(Long profileId, Long currentUserId, Sleep input) {
        Profile profile = validateAndGetProfile(profileId, currentUserId);
        prepareAndValidate(input);
        Optional<Sleep> duplicate = sleepRepository.findFirstByProfile_IdAndRecordedDateAndSleepType(
                profileId, input.getRecordedDate(), input.getSleepType());
        if (duplicate.isPresent()) {
            throw duplicateSleepTypeException(input.getSleepType());
        }
        input.setProfile(profile);
        return sleepRepository.save(input);
    }

    @Override
    public Sleep update(Long profileId, Long currentUserId, Long logId, Sleep input) {
        validateAndGetProfile(profileId, currentUserId);
        Sleep existing = findEntityByIdAndProfile(logId, profileId);
        prepareAndValidate(input);
        Optional<Sleep> duplicate = sleepRepository.findFirstByProfile_IdAndRecordedDateAndSleepType(
                profileId, input.getRecordedDate(), input.getSleepType());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(logId)) {
            throw duplicateSleepTypeException(input.getSleepType());
        }
        existing.setRecordedDate(input.getRecordedDate());
        existing.setStartTime(input.getStartTime());
        existing.setEndTime(input.getEndTime());
        existing.setSleepType(input.getSleepType());
        existing.setSleepMinutes(input.getSleepMinutes());
        existing.setMemo(input.getMemo());
        existing.setMeasuredAt(input.getMeasuredAt());
        return sleepRepository.save(existing);
    }

    private BusinessException duplicateSleepTypeException(SleepType sleepType) {
        String message = sleepType == SleepType.NIGHT
                ? "この日の夜間睡眠は既に登録されています"
                : "この日の昼寝は既に登録されています";
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    @Override
    public void delete(Long profileId, Long currentUserId, Long logId) {
        deleteLog(logId, profileId, currentUserId);
    }

    private void prepareAndValidate(Sleep sleep) {
        if (sleep.getRecordedDate() == null || sleep.getRecordedDate().isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未来の日付は指定できません。日付を確認してください。");
        }
        if (sleep.getStartTime() == null || sleep.getEndTime() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "就寝時刻と起床時刻を入力してください。");
        }
        if (sleep.getSleepType() == null)
            sleep.setSleepType(SleepType.NIGHT);
        int start = sleep.getStartTime().getHour() * 60 + sleep.getStartTime().getMinute();
        int end = sleep.getEndTime().getHour() * 60 + sleep.getEndTime().getMinute();
        if (end <= start)
            end += 24 * 60;
        int duration = end - start;
        int maximum = sleep.getSleepType() == SleepType.NAP ? 300 : 960;
        if (duration <= 0 || duration > maximum) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    sleep.getSleepType() == SleepType.NAP ? "昼寝は5時間以内で入力してください。" : "夜間睡眠は16時間以内で入力してください。");
        }
        sleep.setSleepMinutes(duration);
        sleep.setMeasuredAt(sleep.getRecordedDate().atTime(sleep.getStartTime()));
        if (sleep.getMemo() != null && sleep.getMemo().length() > 500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メモは500文字以内で入力してください。");
        }
        if (sleep.getMemo() != null)
            sleep.setMemo(sleep.getMemo().trim());
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
}
