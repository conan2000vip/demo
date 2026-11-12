package com.healthlog.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.service.helper.CurrentUserResolver;
import com.healthlog.demo.service.home.HomeService;
import com.healthlog.demo.service.home.HomeSummaryChartService;
import com.healthlog.demo.service.profile.ProfileService;

class HomeControllerTest {

    private static final Long USER_ID = 100L;
    private static final Long AUTHENTICATED_PROFILE_ID = 10L;
    private static final Long QUICK_VIEW_PROFILE_ID = 20L;

    @Test
    @DisplayName("Quick view で現在選択中のPIN付きプロフィールを開いてもPIN画面へ戻さない")
    void quickViewDoesNotRequirePinAgain() {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        HomeService homeService = mock(HomeService.class);
        HomeSummaryChartService homeSummaryChartService = mock(HomeSummaryChartService.class);
        ProfileService profileService = mock(ProfileService.class);
        HomeController controller = new HomeController(
                currentUserResolver, homeService, homeSummaryChartService, profileService);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, QUICK_VIEW_PROFILE_ID);
        when(currentUserResolver.resolve(session)).thenReturn(user);
        when(profileService.hasPin(USER_ID, QUICK_VIEW_PROFILE_ID)).thenReturn(true);
        when(homeService.getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID)).thenReturn(Map.of());

        String result = controller.showHomePage(
                QUICK_VIEW_PROFILE_ID, null, null, session, new ConcurrentModel());

        assertEquals("home", result);
        assertEquals(AUTHENTICATED_PROFILE_ID,
                session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID));
        verify(profileService).switchProfile(session, USER_ID, QUICK_VIEW_PROFILE_ID);
    }

    @Test
    @DisplayName("Family card 切り替え後はPIN画面へ戻さず認証済みプロフィールを維持する")
    void familyCardSwitchDoesNotRequirePinAgain() {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        HomeService homeService = mock(HomeService.class);
        HomeSummaryChartService homeSummaryChartService = mock(HomeSummaryChartService.class);
        ProfileService profileService = mock(ProfileService.class);
        HomeController controller = new HomeController(
                currentUserResolver, homeService, homeSummaryChartService, profileService);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, QUICK_VIEW_PROFILE_ID);
        session.setAttribute(SessionConstants.PROFILE_SWITCH_TARGET_ID, QUICK_VIEW_PROFILE_ID);
        when(currentUserResolver.resolve(session)).thenReturn(user);
        when(profileService.hasPin(USER_ID, QUICK_VIEW_PROFILE_ID)).thenReturn(true);
        when(homeService.getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID)).thenReturn(Map.of());

        String result = controller.showHomePage(
                QUICK_VIEW_PROFILE_ID, null, null, session, new ConcurrentModel());

        assertEquals("home", result);
        assertEquals(AUTHENTICATED_PROFILE_ID,
                session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID));
        org.junit.jupiter.api.Assertions.assertNull(
                session.getAttribute(SessionConstants.PROFILE_SWITCH_TARGET_ID));
        verify(profileService).switchProfile(session, USER_ID, QUICK_VIEW_PROFILE_ID);
    }

    @Test
    @DisplayName("別プロフィールへ直接アクセスした場合はPIN選択画面を維持する")
    void directAccessToOtherPinnedProfileStillRequiresPin() {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        HomeService homeService = mock(HomeService.class);
        HomeSummaryChartService homeSummaryChartService = mock(HomeSummaryChartService.class);
        ProfileService profileService = mock(ProfileService.class);
        HomeController controller = new HomeController(
                currentUserResolver, homeService, homeSummaryChartService, profileService);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        when(currentUserResolver.resolve(session)).thenReturn(user);
        when(profileService.hasPin(USER_ID, QUICK_VIEW_PROFILE_ID)).thenReturn(true);

        String result = controller.showHomePage(
                QUICK_VIEW_PROFILE_ID, null, null, session, new ConcurrentModel());

        assertEquals("redirect:/profile/20/select", result);
        verify(profileService, never()).switchProfile(session, USER_ID, QUICK_VIEW_PROFILE_ID);
    }
}
