package com.healthlog.demo.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.helper.CurrentUserResolver;
import com.healthlog.demo.service.home.HomeService;
import com.healthlog.demo.service.home.HomeSummaryChartService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile/{profileId}/home")
@RequiredArgsConstructor
public class HomeController {

    private final CurrentUserResolver currentUserResolver;
    private final HomeService homeService;
    private final HomeSummaryChartService homeSummaryChartService;

    @GetMapping
    public String showHomePage(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session, Model model) {

        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";

        homeService.getHomeData(profileId, user.getId()).forEach(model::addAttribute);
        model.addAttribute("profileId", profileId);
        model.addAttribute("filterStartDate", startDate);
        model.addAttribute("filterEndDate", endDate);

        LocalDate to = endDate != null ? endDate : LocalDate.now();
        LocalDate from = startDate != null ? startDate : to.minusDays(6);
        model.addAttribute("chartFrom", from);
        model.addAttribute("chartTo", to);
        return "home";
    }

    @GetMapping("/summary-chart")
    @ResponseBody
    public Map<String, Object> summaryChart(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        User user = currentUserResolver.resolve(session);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "ログインしてください");
        }
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        LocalDate from = startDate != null ? startDate : to.minusDays(6);
        return homeSummaryChartService.getSummaryChartData(profileId, user.getId(), from, to);
    }
}