package com.healthlog.demo.controller;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
import com.healthlog.demo.entity.User;
import com.healthlog.demo.entity.Water;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.helper.CurrentUserResolver;
import com.healthlog.demo.service.feedbackservice.FeedbackService;
import com.healthlog.demo.service.water.WaterService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile/{profileId}/water")
@RequiredArgsConstructor
public class WaterController {
    private final WaterService waterService;
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
        Map<String, Object> result = waterService.list(profileId, user.getId(),
                new DateRangerFilter(startDate, endDate), page);
        result.forEach(model::addAttribute);
        model.addAttribute("profileId", profileId);
        model.addAttribute("feedbackList", feedbackService.getWaterFeedback(profileId));
        return "water";
    }

    @GetMapping("/chart-data")
    @ResponseBody
    public Map<String, Object> chartData(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        User user = requireUser(session);
        return waterService.chartData(profileId, user.getId(), new DateRangerFilter(startDate, endDate));
    }

    @PostMapping("/save")
    public String save(@PathVariable Long profileId, @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) String recordedDate,
            @RequestParam(required = false) String recordedTime,
            @RequestParam(required = false) String drinkType, @RequestParam(required = false) String amountMl,
            @RequestParam(required = false) String memo,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        try {
            Water input = new Water();
            input.setRecordedDate(parseRecordedDate(recordedDate));
            input.setRecordedTime(parseRecordedTime(recordedTime));
            input.setDrinkType(drinkType);
            input.setAmountMl(parseAmountMl(amountMl));
            input.setMemo(memo);
            if (recordId == null) {
                waterService.create(profileId, user.getId(), input);
                flash.addFlashAttribute("message", "水分記録を保存しました。");
            } else {
                waterService.update(profileId, user.getId(), recordId, input);
                flash.addFlashAttribute("message", "水分記録を更新しました。");
            }
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/water";
    }

    private LocalDate parseRecordedDate(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "日付を選択してください。");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "正しい日付を入力してください。");
        }
    }

    private int parseAmountMl(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "水分量を入力してください。");
        }
        if (!value.matches("-?\\d+")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "水分量は整数で入力してください。");
        }
        BigInteger amount;
        try {
            amount = new BigInteger(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "水分量は整数で入力してください。");
        }
        if (amount.compareTo(BigInteger.ONE) < 0 || amount.compareTo(BigInteger.valueOf(5000)) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "水分量は1〜5000mlの範囲で入力してください。");
        }
        return amount.intValue();
    }

    private LocalTime parseRecordedTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "正しい記録時刻を入力してください。");
        }
    }

    @PostMapping("/{logId}/delete")
    public String delete(@PathVariable Long profileId, @PathVariable Long logId,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        try {
            waterService.delete(profileId, user.getId(), logId);
            flash.addFlashAttribute("message", "水分記録を削除しました。");
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/water";
    }

    private User requireUser(HttpSession session) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "ログインしてください。");
        return user;
    }
}
