package com.healthlog.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.ResponseEntity;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.service.profile.ProfileShareService;

class ProfileShareSettingControllerTest {

    private static final Long USER_ID = 100L;
    private static final Long PRIMARY_ID = 10L;
    private static final Long CHILD_ID = 20L;
    private static final Long OTHER_CHILD_ID = 30L;

    @Test
    @DisplayName("Main profile có thể quản lý chia sẻ của profile con đang được main quản lý")
    void primaryCanManageManagedChildShareSettings() {
        ProfileShareService shareService = org.mockito.Mockito.mock(ProfileShareService.class);
        ProfileService profileService = org.mockito.Mockito.mock(ProfileService.class);
        ProfileShareSettingController controller = new ProfileShareSettingController(shareService, profileService);
        MockHttpSession session = authenticatedSession(PRIMARY_ID);
        Profile primary = profile(PRIMARY_ID, true, true);
        Profile child = profile(CHILD_ID, false, true);
        when(profileService.getProfile(USER_ID, PRIMARY_ID)).thenReturn(primary);
        when(profileService.getProfile(USER_ID, CHILD_ID)).thenReturn(child);
        when(profileService.resolveCurrentProfile(session, USER_ID)).thenReturn(primary);
        when(shareService.getShareSettings(USER_ID, CHILD_ID)).thenReturn(List.of());

        ResponseEntity<Object> response = controller.getShareSettings(CHILD_ID, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(shareService).getShareSettings(USER_ID, CHILD_ID);
    }

    @Test
    @DisplayName("Profile phụ không thể mở cài đặt chia sẻ của profile khác")
    void secondaryCannotManageOtherProfileShareSettings() {
        ProfileShareService shareService = org.mockito.Mockito.mock(ProfileShareService.class);
        ProfileService profileService = org.mockito.Mockito.mock(ProfileService.class);
        ProfileShareSettingController controller = new ProfileShareSettingController(shareService, profileService);
        MockHttpSession session = authenticatedSession(CHILD_ID);
        Profile child = profile(CHILD_ID, false, true);
        Profile otherChild = profile(OTHER_CHILD_ID, false, true);
        when(profileService.getProfile(USER_ID, CHILD_ID)).thenReturn(child);
        when(profileService.getProfile(USER_ID, OTHER_CHILD_ID)).thenReturn(otherChild);
        when(profileService.resolveCurrentProfile(session, USER_ID)).thenReturn(child);

        ResponseEntity<Object> response = controller.getShareSettings(OTHER_CHILD_ID, session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(shareService, never()).getShareSettings(USER_ID, OTHER_CHILD_ID);
    }

    @Test
    @DisplayName("Sau khi main chuyển sang profile con thì không thể quản lý chia sẻ như main")
    void switchedPrimaryCannotManageOtherProfilesFromChildContext() {
        ProfileShareService shareService = org.mockito.Mockito.mock(ProfileShareService.class);
        ProfileService profileService = org.mockito.Mockito.mock(ProfileService.class);
        ProfileShareSettingController controller = new ProfileShareSettingController(shareService, profileService);
        MockHttpSession session = authenticatedSession(PRIMARY_ID);
        Profile primary = profile(PRIMARY_ID, true, true);
        Profile currentChild = profile(CHILD_ID, false, true);
        Profile otherChild = profile(OTHER_CHILD_ID, false, true);
        when(profileService.getProfile(USER_ID, PRIMARY_ID)).thenReturn(primary);
        when(profileService.getProfile(USER_ID, OTHER_CHILD_ID)).thenReturn(otherChild);
        when(profileService.resolveCurrentProfile(session, USER_ID)).thenReturn(currentChild);

        ResponseEntity<Object> response = controller.getShareSettings(OTHER_CHILD_ID, session);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(shareService, never()).getShareSettings(USER_ID, OTHER_CHILD_ID);
    }

    @Test
    @DisplayName("Chưa đăng nhập thì API chia sẻ trả về 401")
    void unauthenticatedRequestIsRejected() {
        ProfileShareService shareService = org.mockito.Mockito.mock(ProfileShareService.class);
        ProfileService profileService = org.mockito.Mockito.mock(ProfileService.class);
        ProfileShareSettingController controller = new ProfileShareSettingController(shareService, profileService);

        ResponseEntity<Object> response = controller.getShareSettings(CHILD_ID, new MockHttpSession());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(shareService, never()).getShareSettings(USER_ID, CHILD_ID);
    }

    private static MockHttpSession authenticatedSession(Long authenticatedProfileId) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.LOGIN_USER, user);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, authenticatedProfileId);
        return session;
    }

    private static Profile profile(Long id, boolean primary, boolean managedByPrimary) {
        User user = new User();
        user.setId(USER_ID);
        Profile profile = new Profile(user, "Profile " + id, "family");
        profile.setId(id);
        profile.setPrimary(primary);
        profile.setManagedByPrimary(managedByPrimary);
        return profile;
    }
}
