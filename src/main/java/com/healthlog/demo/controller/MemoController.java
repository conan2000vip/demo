package com.healthlog.demo.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Memo;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.service.helper.CurrentUserResolver;
import com.healthlog.demo.service.memo.MemoService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile/{profileId}/memo")
@RequiredArgsConstructor
public class MemoController {
    private final MemoService memoService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public String page(@PathVariable Long profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        Map<String, Object> result = memoService.list(profileId, user.getId(), new DateRangerFilter(startDate, endDate),
                page);
        result.forEach(model::addAttribute);
        model.addAttribute("profileId", profileId);
        return "memo";
    }

    @PostMapping("/save")
    public String save(@PathVariable Long profileId, @RequestParam(required = false) Long recordId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordedDate,
            @RequestParam(required = false) String title, @RequestParam String content,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        Memo input = new Memo();
        input.setRecordedDate(recordedDate);
        input.setTitle(title);
        input.setContent(content);
        try {
            if (recordId == null) {
                memoService.create(profileId, user.getId(), input);
                flash.addFlashAttribute("message", "メモを保存しました。");
            } else {
                memoService.update(profileId, user.getId(), recordId, input);
                flash.addFlashAttribute("message", "メモを更新しました。");
            }
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/memo";
    }

    @PostMapping("/{logId}/delete")
    public String delete(@PathVariable Long profileId, @PathVariable Long logId,
            HttpSession session, RedirectAttributes flash) {
        User user = currentUserResolver.resolve(session);
        if (user == null)
            return "redirect:/auth/login";
        try {
            memoService.delete(profileId, user.getId(), logId);
            flash.addFlashAttribute("message", "メモを削除しました。");
        } catch (BusinessException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/profile/" + profileId + "/memo";
    }
}
