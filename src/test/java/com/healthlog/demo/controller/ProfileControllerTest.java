package com.healthlog.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.util.SecurityContextUtil;

class ProfileControllerTest {

    @Test
    void profileManagementRoutesUnauthenticatedProfileToPinSelection() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 30L);
        ConcurrentModel model = new ConcurrentModel();

        String result = controller.manageProfile(session, model);

        assertEquals("redirect:/profile/30/select", result);
        verify(profileService, never()).getProfiles(user.getId());
        verify(profileService, never()).hasAnyProfile(user.getId());
    }

    @Test
    void profileManagementWithoutAnySelectedProfileRoutesToProfileSelection() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);

        String result = controller.manageProfile(session, new ConcurrentModel());

        assertEquals("redirect:/profile/select", result);
        verify(profileService, never()).getProfiles(user.getId());
    }

    @Test
    void legacyProfileManagementUrlRedirectsToProfileScopedUrl() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 30L);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, 30L);

        assertEquals("redirect:/profile/30/profile-manage",
                controller.manageProfile(session, new ConcurrentModel()));
    }

    @Test
    void switchingProfileFromManagementKeepsManagementPageWithoutManagementPermissions() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, 10L);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 30L);
        Profile authenticatedProfile = new Profile();
        authenticatedProfile.setPrimary(true);
        Profile currentProfile = new Profile();
        currentProfile.setId(30L);
        when(profileService.getProfile(user.getId(), 10L)).thenReturn(authenticatedProfile);
        when(profileService.hasAnyProfile(user.getId())).thenReturn(true);
        when(profileService.getProfiles(user.getId())).thenReturn(List.of(currentProfile));
        when(profileService.resolveCurrentProfile(session, user.getId())).thenReturn(currentProfile);
        when(profileService.isProfileLimitReached(user.getId())).thenReturn(false);
        ConcurrentModel model = new ConcurrentModel();

        String result = controller.manageProfile(30L, session, model);

        assertEquals("profile/profile-manage", result);
        assertEquals(false, model.getAttribute("canManageAllProfiles"));
        assertEquals(false, model.getAttribute("canManageCurrentProfile"));
        assertEquals("プロファイルを管理するには、認証済みプロファイルに切り替えてください。",
                model.getAttribute("error"));
    }

    @Test
    void successfulPinVerificationReturnsToOriginallyRequestedProfilePage() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.PIN_RETURN_TO, "/profile/profile-manage");

        String result = controller.verifyPin(30L, "1234", session, new ConcurrentModel());

        assertEquals("redirect:/profile/30/profile-manage", result);
        assertEquals(30L, session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID));
        verify(profileService).verifyPin(user.getId(), 30L, "1234");
    }

    @Test
    void switchingProfilePreservesCurrentSectionAndFilters() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        Profile profile = new Profile();
        profile.setName("Family member");
        org.mockito.Mockito.when(profileService.getProfile(user.getId(), 30L)).thenReturn(profile);

        String result = controller.switchProfile(
                30L,
                session,
                "http://localhost:8081/profile/45/home",
                "/profile/45/sleep?startDate=2026-11-01&endDate=2026-11-12",
                new RedirectAttributesModelMap());

        assertEquals("redirect:/profile/30/sleep?startDate=2026-11-01&endDate=2026-11-12", result);
        verify(profileService).switchProfile(session, user.getId(), 30L);
    }

    @Test
    void switchingProfileFromManagementPreservesManagementPage() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        Profile profile = new Profile();
        profile.setName("Family member");
        org.mockito.Mockito.when(profileService.getProfile(user.getId(), 30L)).thenReturn(profile);

        String result = controller.switchProfile(
                30L,
                session,
                "http://localhost:8081/profile/45/profile-manage",
                "/profile/45/profile-manage",
                new RedirectAttributesModelMap());

        assertEquals("redirect:/profile/30/profile-manage", result);
        verify(profileService).switchProfile(session, user.getId(), 30L);
    }

    @Test
    void switchingProfileFromHomeUpdatesCurrentProfileForThisTab() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileController controller = new ProfileController(
                profileService, mock(SecurityContextUtil.class), mock(UserRepository.class));
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 10L);
        Profile profile = new Profile();
        profile.setName("Family member");
        org.mockito.Mockito.when(profileService.getProfile(user.getId(), 30L)).thenReturn(profile);
        String result = controller.switchProfile(
                30L,
                session,
                "http://localhost:8081/profile/10/home?startDate=2026-11-01",
                "/profile/10/home?startDate=2026-11-01",
                new RedirectAttributesModelMap());

        assertEquals("redirect:/profile/30/home?startDate=2026-11-01", result);
        assertEquals(10L, session.getAttribute(SessionConstants.CURRENT_PROFILE_ID));
        verify(profileService).switchProfile(session, user.getId(), 30L);
    }
}
