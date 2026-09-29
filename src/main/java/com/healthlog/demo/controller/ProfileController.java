package com.healthlog.demo.controller;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.dto.profile.ProfileFormDto;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.util.SecurityContextUtil;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private static final List<String> AVATARS = List.of(
            "avatar_01", "avatar_02", "avatar_03", "avatar_04", "avatar_05",
            "avatar_06", "avatar_07", "avatar_08", "avatar_09", "avatar_10", "avatar_11", "avatar_12");

    private final ProfileService profileService;
    private final SecurityContextUtil securityContextUtil;
    private final UserRepository userRepository;

    private User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (user != null) {
            return user;
        }
        String email = securityContextUtil.getCurrentUserEmail();
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                session.setAttribute(SessionConstants.LOGIN_USER, user);
            }
        }
        return user;
    }

    // === 1. プロファイル選択画面 ===
    @GetMapping("/select")
    public String showSelectProfileScreen(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("profiles", profileService.getProfiles(user.getId()));
        return "profile/select-profile";
    }

    @GetMapping("/{id}/select")
    public String selectProfile(@PathVariable("id") Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }
        profileService.switchProfile(session, user.getId(), id);
        return "redirect:/profile/" + id + "/home";
    }

    // === 2. プロファイル管理画面 ===
    @GetMapping("/profile-manage")
    public String manageProfile(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("profiles", profileService.getProfiles(user.getId()));
        model.addAttribute("currentProfile", profileService.resolveCurrentProfile(session, user.getId()));
        return "profile/profile-manage";
    }

    // === 3. ヘッダーからプロファイルを切り替える処理 ===
    @PostMapping("/switch/{id}")
    public String switchProfile(@PathVariable("id") Long id, HttpSession session,
            @RequestHeader(value = "Referer", required = false) String referer, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            Profile profile = profileService.getProfile(user.getId(), id);
            profileService.switchProfile(session, user.getId(), id);
            redirectAttributes.addFlashAttribute("message", profile.getName() + " プロファイルを切り替えました");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + buildRedirectUrl(referer, id);
    }

    // === 4. ProfileFormDtoを使用した新規プロファイル作成処理 ===
    @GetMapping("/new")
    public String newProfileForm(HttpSession session, Model model) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("profile", new ProfileFormDto());
        model.addAttribute("isFirstProfile", !profileService.hasAnyProfile(user.getId()));
        model.addAttribute("isPrimary", false);
        model.addAttribute("avatars", AVATARS);
        return "profile/profile-form";
    }

    @PostMapping("/new")
    public String createProfile(@Valid @ModelAttribute("profile") ProfileFormDto formDto, BindingResult bindingResult,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        boolean isFirstProfile = !profileService.hasAnyProfile(user.getId());

        if (bindingResult.hasErrors()) {
            model.addAttribute("isFirstProfile", isFirstProfile);
            model.addAttribute("isPrimary", false);
            model.addAttribute("avatars", AVATARS);
            return "profile/profile-form";
        }

        try {
            Profile createdProfile = profileService.create(user.getId(), formDto);
            session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, createdProfile.getId());
            redirectAttributes.addFlashAttribute("message", createdProfile.getName() + " のプロファイルを作成しました");
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isFirstProfile", isFirstProfile);
            model.addAttribute("isPrimary", false);
            model.addAttribute("avatars", AVATARS);
            return "profile/profile-form";
        }

        return "redirect:/profile/profile-manage";
    }

    // === 5. ProfileFormDtoを使用したプロファイル編集処理 ===
    @GetMapping("/{id}/edit")
    public String editProfileForm(@PathVariable("id") Long id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (!canManageProfile(user.getId(), id, session)) {
            redirectAttributes.addFlashAttribute("error", "このプロファイルを編集する権限がありません");
            return "redirect:/profile/profile-manage";
        }

        try {
            ProfileFormDto formDto = profileService.getProfileFormDto(user.getId(), id);
            model.addAttribute("profile", formDto);
            model.addAttribute("isPrimary", formDto.isPrimary());
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("profiles", profileService.getProfiles(user.getId()));
            return "profile/profile-manage";
        }

        model.addAttribute("isFirstProfile", false);
        model.addAttribute("avatars", AVATARS);
        return "profile/profile-form";
    }

    @PostMapping("/{id}/edit")
    public String updateProfile(@PathVariable("id") Long id, @Valid @ModelAttribute("profile") ProfileFormDto formDto,
            BindingResult bindingResult, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (!canManageProfile(user.getId(), id, session)) {
            redirectAttributes.addFlashAttribute("error", "このプロファイルを編集する権限がありません");
            return "redirect:/profile/profile-manage";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("isFirstProfile", false);
            model.addAttribute("isPrimary", formDto.isPrimary());
            model.addAttribute("avatars", AVATARS);
            return "profile/profile-form";
        }

        try {
            formDto.setId(id);
            Profile updatedProfile = profileService.update(user.getId(), formDto);
            redirectAttributes.addFlashAttribute("message", updatedProfile.getName() + " のプロファイルを更新しました");
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isFirstProfile", false);
            model.addAttribute("isPrimary", formDto.isPrimary());
            model.addAttribute("avatars", AVATARS);
            return "profile/profile-form";
        }

        return "redirect:/profile/profile-manage";
    }

    // === 6. プロファイル削除処理 ===
    @PostMapping("/delete")
    public String deleteProfile(@RequestParam("profileId") Long profileId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (!canManageProfile(user.getId(), profileId, session)) {
            redirectAttributes.addFlashAttribute("error", "このプロファイルを削除する権限がありません");
            return "redirect:/profile/profile-manage";
        }

        try {
            profileService.delete(user.getId(), profileId, session);
            redirectAttributes.addFlashAttribute("message", "プロファイルを削除しました");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/profile/profile-manage";
    }

    private boolean canManageProfile(Long userId, Long targetProfileId, HttpSession session) {
        Profile currentProfile = profileService.resolveCurrentProfile(session, userId);
        return currentProfile != null && (currentProfile.isPrimary() || currentProfile.getId().equals(targetProfileId));
    }

    // リダイレクト先URLを作成する補助処理。
    private String buildRedirectUrl(String referer, Long newProfileId) {
        String path = extractSafePath(referer);
        if (path == null) {
            return "/profile/" + newProfileId + "/home";
        }

        Matcher matcher = Pattern.compile("^/profile/\\d+(/.*)?$").matcher(path);
        if (matcher.matches()) {
            String suffix = matcher.group(1);
            if (suffix == null || suffix.isBlank() || suffix.equals("/")) {
                suffix = "/home";
            }
            return "/profile/" + newProfileId + suffix;
        }

        return path;
    }

    private String extractSafePath(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String query = uri.getRawQuery();
            return query != null ? path + "?" + query : path;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}