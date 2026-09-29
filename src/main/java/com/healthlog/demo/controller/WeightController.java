package com.healthlog.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.entity.Weight;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.weight.WeightFeedbackRule;
import com.healthlog.demo.service.weight.WeightService;
import com.healthlog.demo.util.SecurityContextUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile/{profileId}/weight")
@RequiredArgsConstructor
public class WeightController {

	private final WeightService weightService;
	private final WeightFeedbackRule weightFeedbackRule;
	private final SecurityContextUtil securityContextUtil;
	private final UserRepository userRepository;

	@GetMapping
	public String showWeightPage(
			@PathVariable Long profileId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(defaultValue = "0") int page,
			HttpSession session,
			Model model) {
		User user = getCurrentUser(session);
		if (user == null) return "redirect:/auth/login";

		DateRangerFilter dateRange = new DateRangerFilter(startDate, endDate);
		Map<String, Object> result = weightService.list(profileId, user.getId(), dateRange, page);
		result.forEach(model::addAttribute);
		model.addAttribute("profileId", profileId);
		model.addAttribute("filterStartDate", startDate);
		model.addAttribute("filterEndDate", endDate);
		List<FeedbackItem> feedbackList = weightFeedbackRule.evaluate(profileId);
		model.addAttribute("feedbackList", feedbackList);
		return "weight";
	}

	@GetMapping("/chart-data")
	@ResponseBody
	public Map<String, Object> chartData(
			@PathVariable Long profileId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			HttpSession session) {
		User user = getCurrentUser(session);
		if (user == null) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "ログインしてください");
		}
		return weightService.chartData(profileId, user.getId(), new DateRangerFilter(startDate, endDate));
	}

	@PostMapping("/save")
	public String saveWeight(
			@PathVariable Long profileId,
			@RequestParam(required = false) Long recordId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordedDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime measuredAt,
			@RequestParam BigDecimal weight,
			@RequestParam(required = false) BigDecimal height,
			@RequestParam(required = false) String memo,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User user = getCurrentUser(session);
		if (user == null) return "redirect:/auth/login";

		Weight input = new Weight();
		input.setRecordedDate(recordedDate);
		input.setMeasuredAt(measuredAt != null ? measuredAt : recordedDate.atStartOfDay());
		input.setWeight(weight);
		input.setHeight(height);
		input.setMemo(memo);

		try {
			if (recordId == null) {
				weightService.create(profileId, user.getId(), input);
				redirectAttributes.addFlashAttribute("message", "体重記録を保存しました");
			} else {
				weightService.update(profileId, user.getId(), recordId, input);
				redirectAttributes.addFlashAttribute("message", "体重記録を更新しました");
			}
		} catch (BusinessException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/profile/" + profileId + "/weight";
	}

	@PostMapping("/{logId}/delete")
	public String deleteWeight(
			@PathVariable Long profileId,
			@PathVariable Long logId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User user = getCurrentUser(session);
		if (user == null) return "redirect:/auth/login";

		try {
			weightService.delete(profileId, user.getId(), logId);
			redirectAttributes.addFlashAttribute("message", "体重記録を削除しました");
		} catch (BusinessException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/profile/" + profileId + "/weight";
	}

	private User getCurrentUser(HttpSession session) {
		User user = (User) session.getAttribute(SessionConstants.LOGIN_USER);
		if (user != null) return user;

		String email = securityContextUtil.getCurrentUserEmail();
		if (email == null) return null;

		user = userRepository.findByEmail(email).orElse(null);
		if (user != null) session.setAttribute(SessionConstants.LOGIN_USER, user);
		return user;
	}
}
