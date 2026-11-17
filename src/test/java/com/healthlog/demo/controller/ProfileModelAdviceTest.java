package com.healthlog.demo.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.HandlerMapping;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.util.SecurityContextUtil;

class ProfileModelAdviceTest {

    @Test
    void profileFromRequestUrlOverridesSharedSessionProfile() {
        ProfileService profileService = mock(ProfileService.class);
        SecurityContextUtil securityContextUtil = mock(SecurityContextUtil.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProfileAccessValidation accessValidation = mock(ProfileAccessValidation.class);
        ProfileModelAdvice advice = new ProfileModelAdvice(
                profileService, securityContextUtil, userRepository, accessValidation);

        User user = new User();
        user.setId(100L);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 10L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("profileId", "20"));
        Profile requestedProfile = new Profile();
        requestedProfile.setId(20L);
        when(profileService.getProfiles(user.getId())).thenReturn(List.of(requestedProfile));
        when(accessValidation.validateCanViewProfile(20L, user.getId())).thenReturn(requestedProfile);
        ConcurrentModel model = new ConcurrentModel();

        advice.addProfileMenuAttributes(request, session, model);

        assertSame(requestedProfile, model.getAttribute("currentProfile"));
        verify(accessValidation).validateCanViewProfile(20L, user.getId());
        verify(profileService, never()).resolveCurrentProfile(session, user.getId());
        verify(accessValidation).hasReadAccess(20L, user.getId(), Category.weight);
    }
}
