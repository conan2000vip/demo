package com.healthlog.demo.controller;

import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.helper.TabProfileContext;
import com.healthlog.demo.util.SecurityContextUtil;

import jakarta.servlet.http.HttpServletRequest;
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
    public void addProfileMenuAttributes(HttpServletRequest request, HttpSession session, Model model) {
        String tabId = TabProfileContext.ensureTabId(request);
        TabProfileContext.initializeFromLegacy(session, tabId);
        model.addAttribute("tabId", tabId);
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

        if (user == null)
            return;

        model.addAttribute("profileList", profileService.getProfiles(user.getId()));
        Profile currentProfile = resolveCurrentProfile(request, session, user.getId());
        model.addAttribute("currentProfile", currentProfile);
        model.addAttribute("canViewWeight", currentProfile != null
                && profileAccessValidation.hasReadAccess(currentProfile.getId(), user.getId(), Category.weight));
        model.addAttribute("canViewSleep", currentProfile != null
                && profileAccessValidation.hasReadAccess(currentProfile.getId(), user.getId(), Category.sleep));
        model.addAttribute("canViewWater", currentProfile != null
                && profileAccessValidation.hasReadAccess(currentProfile.getId(), user.getId(), Category.water));
        model.addAttribute("canViewStep", currentProfile != null
                && profileAccessValidation.hasReadAccess(currentProfile.getId(), user.getId(), Category.step));
        model.addAttribute("canViewMemo", currentProfile != null
                && profileAccessValidation.hasReadAccess(currentProfile.getId(), user.getId(), Category.memo));
        model.addAttribute("canEditWeight", currentProfile != null
                && profileAccessValidation.hasEditAccess(currentProfile.getId(), user.getId(), Category.weight));
        model.addAttribute("canEditSleep", currentProfile != null
                && profileAccessValidation.hasEditAccess(currentProfile.getId(), user.getId(), Category.sleep));
        model.addAttribute("canEditWater", currentProfile != null
                && profileAccessValidation.hasEditAccess(currentProfile.getId(), user.getId(), Category.water));
        model.addAttribute("canEditStep", currentProfile != null
                && profileAccessValidation.hasEditAccess(currentProfile.getId(), user.getId(), Category.step));
        model.addAttribute("canEditMemo", currentProfile != null
                && profileAccessValidation.hasEditAccess(currentProfile.getId(), user.getId(), Category.memo));
    }

    private Profile resolveCurrentProfile(HttpServletRequest request, HttpSession session, Long userId) {
        Object pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables instanceof Map<?, ?> variables) {
            Object profileId = variables.containsKey("profileId")
                    ? variables.get("profileId")
                    : variables.get("id");
            if (profileId != null) {
                return profileAccessValidation.validateCanViewProfile(Long.valueOf(profileId.toString()), userId);
            }
        }
        return profileService.resolveCurrentProfile(session, userId);
    }
}