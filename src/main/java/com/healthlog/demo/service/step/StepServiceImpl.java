package com.healthlog.demo.service.step;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.StepSummary;
import com.healthlog.demo.dto.chartdata.ChartDataResponse;
import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.service.base.BaseLogService;
import com.healthlog.demo.service.helper.ChartDataBuilder;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

@Service
@Transactional
public class StepServiceImpl extends BaseLogService<Step, Step> implements StepService {
    private static final int PAGE_SIZE = 10;
    private final StepRepository stepRepository;
    private final ChartDataBuilder chartDataBuilder;

    public StepServiceImpl(StepRepository stepRepository, ProfileAccessValidation accessValidation,
            ChartDataBuilder chartDataBuilder) {
        super(stepRepository, accessValidation);
        this.stepRepository = stepRepository;
        this.chartDataBuilder = chartDataBuilder;
    }

    @Override
    protected Step mapToDto(Step entity) {
        return entity;
    }

    @Override
    protected Category category() {
        return Category.step;
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter range, int page) {
        validatePageAndRange(range, page);
        Profile profile = validateAndGetProfile(profileId, currentUserId);
        List<Step> filtered = getLogs(profileId, currentUserId, range);
        Page<Step> logPage = getLogsPaged(profileId, currentUserId, range, PageRequest.of(page, PAGE_SIZE));
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        Step todayLog = stepRepository.findFirstByProfile_IdAndRecordedDate(profileId, today).orElse(null);
        List<Step> monthLogs = getLogs(profileId, currentUserId, new DateRangerFilter(monthStart, monthEnd));
        Integer monthAverage = monthLogs.isEmpty() ? null
                : (int) Math.round(monthLogs.stream().mapToInt(Step::getSteps).average().orElse(0));
        Integer goal = profile.getStepGoal();
        Integer goalRate = goal != null && goal > 0 && todayLog != null
                ? (int) Math.round(todayLog.getSteps() * 100.0 / goal)
                : null;
        Step latest = stepRepository.findTopByProfile_IdOrderByRecordedDateDescIdDesc(profileId).orElse(null);
        ChartDataResponse chart = chartDataBuilder.build(filtered, range, log -> BigDecimal.valueOf(log.getSteps()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("latest", latest != null ? latest.getSteps() : null);
        stats.put("latestUpdatedAt", latest != null ? latest.getUpdatedAt() : null);
        stats.put("todaySteps", todayLog != null ? todayLog.getSteps() : null);
        stats.put("todayDate", today);
        stats.put("monthAverage", monthAverage);
        stats.put("goalRate", goalRate);

        Map<String, Object> result = new HashMap<>();
        result.put("currentProfile", profile);
        result.put("logs", logPage.getContent().stream().map(log -> toSummary(log, goal)).toList());
        result.put("stats", stats);
        result.put("hasAnyLog", stepRepository.existsByProfile_Id(profileId));
        result.put("currentPage", logPage.getNumber());
        result.put("totalPages", logPage.getTotalPages());
        result.put("hasPrevious", logPage.hasPrevious());
        result.put("hasNext", logPage.hasNext());
        result.put("labels", chart.labels());
        result.put("values", chart.values());
        result.put("chartMode", chart.chartMode());
        result.put("chartFrom", range != null ? range.getFrom() : null);
        result.put("chartTo", range != null ? range.getTo() : null);
        result.put("filterStartDate", range != null ? range.getFrom() : null);
        result.put("filterEndDate", range != null ? range.getTo() : null);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter range) {
        validateDateRange(range);
        List<Step> logs = getLogs(profileId, currentUserId, range);
        ChartDataResponse chart = chartDataBuilder.build(logs, range, log -> BigDecimal.valueOf(log.getSteps()));
        return Map.of("labels", chart.labels(), "values", chart.values(), "chartMode", chart.chartMode());
    }

    @Override
    public Step create(Long profileId, Long currentUserId, Step input) {
        Profile profile = validateAndGetEditableProfile(profileId, currentUserId);
        validateStep(input);
        Step existing = stepRepository.findFirstByProfile_IdAndRecordedDate(profileId, input.getRecordedDate())
                .orElse(null);
        if (existing != null) {
            existing.setSteps(input.getSteps());
            existing.setMemo(input.getMemo());
            existing.setMeasuredAt(input.getRecordedDate().atStartOfDay());
            return stepRepository.save(existing);
        }
        input.setProfile(profile);
        input.setMeasuredAt(input.getRecordedDate().atStartOfDay());
        return stepRepository.save(input);
    }

    @Override
    public Step update(Long profileId, Long currentUserId, Long logId, Step input) {
        validateAndGetEditableProfile(profileId, currentUserId);
        Step existing = findEntityByIdAndProfile(logId, profileId);
        validateStep(input);
        Step duplicate = stepRepository.findFirstByProfile_IdAndRecordedDate(profileId, input.getRecordedDate())
                .orElse(null);
        if (duplicate != null && !duplicate.getId().equals(logId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "同じ日付の歩数記録は1件だけ登録できます。");
        }
        existing.setRecordedDate(input.getRecordedDate());
        existing.setMeasuredAt(input.getRecordedDate().atStartOfDay());
        existing.setSteps(input.getSteps());
        existing.setMemo(input.getMemo());
        return stepRepository.save(existing);
    }

    @Override
    public void delete(Long profileId, Long currentUserId, Long logId) {
        deleteLog(logId, profileId, currentUserId);
    }

    private StepSummary toSummary(Step step, Integer goal) {
        Integer rate = goal != null && goal > 0 ? (int) Math.round(step.getSteps() * 100.0 / goal) : null;
        return new StepSummary(step.getId(), step.getRecordedDate(), step.getSteps(), rate, step.getUpdatedAt(),
                step.getMemo());
    }

    private void validateStep(Step step) {
        if (step.getRecordedDate() == null || step.getRecordedDate().isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未来の日付は指定できません。日付を確認してください。");
        }
        if (step.getSteps() < 0 || step.getSteps() > 200000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "歩数は0〜200000歩の範囲で入力してください。");
        }
        if (step.getMemo() != null && step.getMemo().length() > 500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メモは500文字以内で入力してください。");
        }
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
