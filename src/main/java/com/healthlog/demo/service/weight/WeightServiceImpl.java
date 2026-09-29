package com.healthlog.demo.service.weight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.WeightSummary;
import com.healthlog.demo.dto.chartdata.ChartDataResponse;
import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.dto.common.PageResponse;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Weight;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.WeightRepository;
import com.healthlog.demo.service.base.BaseLogService;
import com.healthlog.demo.service.helper.ChartDataBuilder;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.weight.WeightFeedback.BmiStatus;

@Service
@Transactional
public class WeightServiceImpl extends BaseLogService<Weight, Weight> implements WeightService {

	private static final int PAGE_SIZE = 10;

	private final WeightRepository weightRepository;
	private final BmiCalculator bmiCalculator;
	private final WeightFeedback weightFeedback;
	private final ChartDataBuilder chartDataBuilder;

	public WeightServiceImpl(
			WeightRepository weightRepository,
			ProfileAccessValidation profileAccessValidation,
			BmiCalculator bmiCalculator,
			WeightFeedback weightFeedback,
			ChartDataBuilder chartDataBuilder) {
		super(weightRepository, profileAccessValidation);
		this.weightRepository = weightRepository;
		this.bmiCalculator = bmiCalculator;
		this.weightFeedback = weightFeedback;
		this.chartDataBuilder = chartDataBuilder;
	}

	@Override
	protected Weight mapToDto(Weight entity) {
		return entity;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> chartData(Long profileId, Long currentUserId, DateRangerFilter dateRange) {
		validateDateRange(dateRange);
		List<Weight> logs = getLogs(profileId, currentUserId, dateRange);
		@SuppressWarnings("null")
        ChartDataResponse chartData = chartDataBuilder.build(logs, dateRange, Weight::getWeight);

		Map<String, Object> result = new HashMap<>();
		result.put("labels", chartData.labels());
		result.put("values", chartData.values());
		result.put("chartMode", chartData.chartMode());
		return result;
	}

	@SuppressWarnings("null")
	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter dateRange, int page) {
		validateDateRange(dateRange);
		if (page < 0) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "ページ番号が正しくありません。");
		}

		LocalDate from = dateRange != null ? dateRange.getFrom() : null;
		LocalDate to = dateRange != null ? dateRange.getTo() : null;
		Profile profile = validateAndGetProfile(profileId, currentUserId);
		List<Weight> allLogs = getLogs(profileId, currentUserId, dateRange);
		Weight minLog = allLogs.stream().min(Comparator.comparing(Weight::getWeight)).orElse(null);
		Weight maxLog = allLogs.stream().max(Comparator.comparing(Weight::getWeight)).orElse(null);
		Weight latestLog = allLogs.stream()
				.max(Comparator.comparing(Weight::getRecordedDate).thenComparing(Weight::getId))
				.orElse(null);

		BigDecimal latest = latestLog != null ? latestLog.getWeight() : null;
		BigDecimal min = minLog != null ? minLog.getWeight() : null;
		BigDecimal max = maxLog != null ? maxLog.getWeight() : null;
		BigDecimal bmi = latestLog != null ? bmiCalculator.calculateBMI(latest, resolveHeight(profile, latestLog)) : null;
		BmiStatus overallStatus = weightFeedback.statusOf(bmi);

		ChartDataResponse chartData = chartDataBuilder.build(allLogs, dateRange, Weight::getWeight);
		Page<Weight> logPage = getLogsPaged(profileId, currentUserId, dateRange, PageRequest.of(page, PAGE_SIZE));
		PageResponse<WeightSummary> pageResponse = PageResponse.from(logPage.map(log -> toSummary(profile, log)));

		Map<String, Object> stats = new HashMap<>();
		stats.put("latest", latest);
		stats.put("min", min);
		stats.put("max", max);
		stats.put("bmi", bmi);
		stats.put("bmiStatus", overallStatus != null ? overallStatus.label() : null);
		stats.put("bmiStatusCode", overallStatus != null ? overallStatus.code() : null);
		stats.put("latestDate", latestLog != null ? latestLog.getRecordedDate() : null);
		stats.put("minDate", minLog != null ? minLog.getRecordedDate() : null);
		stats.put("maxDate", maxLog != null ? maxLog.getRecordedDate() : null);

		Map<String, Object> result = new HashMap<>();
		result.put("currentProfile", profile);
		result.put("logs", pageResponse.content());
		result.put("stats", stats);
		result.put("labels", chartData.labels());
		result.put("values", chartData.values());
		result.put("hasAnyLog", weightRepository.existsByProfile_Id(profileId));
		result.put("currentPage", pageResponse.currentPage());
		result.put("totalPages", pageResponse.totalPages());
		result.put("hasNext", pageResponse.hasNext());
		result.put("hasPrevious", pageResponse.hasPrevious());
		result.put("chartMode", chartData.chartMode());
		result.put("chartFrom", from);
		result.put("chartTo", to);
		return result;
	}

	@Override
	public Weight create(Long profileId, Long currentUserId, Weight weight) {
		Profile profile = validateAndGetProfile(profileId, currentUserId);
		validateWeightInput(weight);
		if (weight.getHeight() == null) {
			weight.setHeight(profile.getHeight());
		}
		weight.setProfile(profile);
		return weightRepository.save(weight);
	}

	@Override
	public Weight update(Long profileId, Long currentUserId, Long logId, Weight input) {
		validateAndGetProfile(profileId, currentUserId);
		Weight log = findEntityByIdAndProfile(logId, profileId);
		validateWeightInput(input);

		if (input.getHeight() == null) {
			input.setHeight(log.getProfile().getHeight());
		}
		log.setRecordedDate(input.getRecordedDate());
		log.setMeasuredAt(input.getMeasuredAt());
		log.setWeight(input.getWeight());
		log.setHeight(input.getHeight());
		log.setMemo(input.getMemo());
		return weightRepository.save(log);
	}

	@Override
	public void delete(Long profileId, Long currentUserId, Long logId) {
		deleteLog(logId, profileId, currentUserId);
	}

	private WeightSummary toSummary(Profile profile, Weight log) {
		BigDecimal bmi = bmiCalculator.calculateBMI(log.getWeight(), resolveHeight(profile, log));
		return WeightSummary.from(log, bmi, weightFeedback.statusOf(bmi));
	}

	private BigDecimal resolveHeight(Profile profile, Weight weight) {
		return weight.getHeight() != null ? weight.getHeight() : profile.getHeight();
	}

	private void validateDateRange(DateRangerFilter dateRange) {
		if (dateRange != null && dateRange.getFrom() != null && dateRange.getTo() != null
				&& dateRange.getFrom().isAfter(dateRange.getTo())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"開始日が終了日より後になっているため、期間指定が正しくありません。");
		}
	}

	private void validateWeightInput(Weight weight) {
		if (weight.getRecordedDate() == null) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "記録日を入力してください");
		}
		if (weight.getRecordedDate().isAfter(LocalDate.now())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "未来の日付は指定できません");
		}
		if (weight.getWeight() == null
				|| weight.getWeight().compareTo(BigDecimal.ONE) < 0
				|| weight.getWeight().compareTo(BigDecimal.valueOf(700)) > 0) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "体重は1〜700kgの範囲で入力してください");
		}
		if (weight.getHeight() != null
				&& (weight.getHeight().compareTo(BigDecimal.TEN) < 0
						|| weight.getHeight().compareTo(BigDecimal.valueOf(350)) > 0)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "身長は10〜350cmの範囲で入力してください");
		}
		if (weight.getMemo() != null && weight.getMemo().length() > 500) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "500文字以内で入力してください");
		}
	}

}
