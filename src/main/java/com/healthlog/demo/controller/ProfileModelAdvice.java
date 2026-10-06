package com.healthlog.demo.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.util.SecurityContextUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class ProfileModelAdvice {
    private final ProfileService profileService;
    private final SecurityContextUtil securityContextUtil;
    private final UserRepository userRepository;
    private final ProfileAccessValidation profileAccessValidation;

    @ModelAttribute
    public void addProfileMenuAttributes(HttpSession session, Model model) {
        User user = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (user == null) {
            String email = securityContextUtil.getCurrentUserEmail();
            if (email != null) {
                user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    session.setAttribute(SessionConstants.LOGIN_USER, user);
                }
            }
        }

        if (user == null) return;

        model.addAttribute("profileList", profileService.getProfiles(user.getId()));
        Profile currentProfile = profileService.resolveCurrentProfile(session, user.getId());
        model.addAttribute("currentProfile", currentProfile);
        model.addAttribute("canViewWeight",
                currentProfile != null && profileAccessValidation.hasReadAccess(
                        currentProfile.getId(), user.getId(), Category.weight));
        model.addAttribute("canViewSleep",
                currentProfile != null && profileAccessValidation.hasReadAccess(
                        currentProfile.getId(), user.getId(), Category.sleep));
        model.addAttribute("canViewWater",
                currentProfile != null && profileAccessValidation.hasReadAccess(
                        currentProfile.getId(), user.getId(), Category.water));
        model.addAttribute("canViewStep",
                currentProfile != null && profileAccessValidation.hasReadAccess(
                        currentProfile.getId(), user.getId(), Category.step));
        model.addAttribute("canViewMemo",
                currentProfile != null && profileAccessValidation.hasReadAccess(
                        currentProfile.getId(), user.getId(), Category.memo));
    }
}