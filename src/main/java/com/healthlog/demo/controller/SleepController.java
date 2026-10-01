package com.healthlog.demo.controller;

import java.time.LocalDate;
import java.time.LocalTime;
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
import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.entity.Sleep.SleepType;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.helper.CurrentUserResolver;
import com.healthlog.demo.service.sleep.SleepService;
import com.healthlog.demo.service.feedbackservice.FeedbackService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile/{profileId}/sleep")
@RequiredArgsConstructor
public class SleepController {
    private final SleepService sleepService;
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
        Map<String, Object> result = sleepService.list(profileId, user.getId(),
                new DateRangerFilter(startDate, endDate), page);
        result.forEach(model::addAttribute);
        model.addAttribute("profileId", profileId);
        model.addAttribute("feedbackList", feedbackService.getSleepFeedback(profileId));
        return "sleep";
    }

    @GetMapping("/chart-data")
    @ResponseBody
    public Map<String, Object> chartData(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        User user = requireUser(session);
        return sleepService.chartData(profileId, user.getId(), new DateRangerFilter(startDate, endDate));
    }

    @PostMapping("/save")
    public String save(@PathVariable Long profileId, @RequestParam(required = false) Long recordId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordedDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(defaultValue = "NIGHT") SleepType sleepType,
            @RequestParam(required = false) String memo, HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        Sleep input = new Sleep();
        input.setRecordedDate(recordedDate);
        input.setStartTime(startTime);
        input.setEndTime(endTime);
        input.setSleepType(sleepType);
        input.setMemo(memo);
        try {
            if (recordId == null) {
                sleepService.create(profileId, user.getId(), input);
                flash.addFlashAttribute("message", "睡眠記録を保存しました。");
            } else {
                sleepService.update(profileId, user.getId(), recordId, input);
                flash.addFlashAttribute("message", "睡眠記録を更新しました。");
            }
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/sleep";
    }

    @PostMapping("/{logId}/delete")
    public String delete(@PathVariable Long profileId, @PathVariable Long logId,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        try {
            sleepService.delete(profileId, user.getId(), logId);
            flash.addFlashAttribute("message", "睡眠記録を削除しました。");
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/sleep";
    }

    private User requireUser(HttpSession session) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "ログインしてください。");
        return user;
    }
}
