package com.healthlog.demo.controller;

import java.time.LocalDate;
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

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.helper.CurrentUserResolver;
import com.healthlog.demo.service.feedbackservice.FeedbackService;
import com.healthlog.demo.service.step.StepService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile/{profileId}/step")
@RequiredArgsConstructor
public class StepController {
    private final StepService stepService;
    private final FeedbackService feedbackService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public String page(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        Map<String, Object> result = stepService.list(profileId, user.getId(), new DateRangerFilter(startDate, endDate),
                page);
        result.forEach(model::addAttribute);
        model.addAttribute("profileId", profileId);
        model.addAttribute("feedbackList", feedbackService.getStepFeedback(profileId));
        return "step";
    }

    @GetMapping("/chart-data")
    @ResponseBody
    public Map<String, Object> chartData(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        User user = requireUser(session);
        return stepService.chartData(profileId, user.getId(), new DateRangerFilter(startDate, endDate));
    }

    @PostMapping("/save")
    public String save(@PathVariable Long profileId, @RequestParam(required = false) Long recordId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordedDate,
            @RequestParam int steps, @RequestParam(required = false) String memo,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        Step input = new Step();
        input.setRecordedDate(recordedDate);
        input.setSteps(steps);
        input.setMemo(memo);
        try {
            if (recordId == null) {
                stepService.create(profileId, user.getId(), input);
                flash.addFlashAttribute("message", "歩数記録を保存しました。");
            } else {
                stepService.update(profileId, user.getId(), recordId, input);
                flash.addFlashAttribute("message", "歩数記録を更新しました。");
            }
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/step";
    }

    @PostMapping("/{logId}/delete")
    public String delete(@PathVariable Long profileId, @PathVariable Long logId,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        try {
            stepService.delete(profileId, user.getId(), logId);
            flash.addFlashAttribute("message", "歩数記録を削除しました。");
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/step";
    }

    private User requireUser(HttpSession session) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "ログインしてください。");
        return user;
    }
}
