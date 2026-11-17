package com.healthlog.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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

class HomeControllerTest {

    private static final Long USER_ID = 100L;
    private static final Long AUTHENTICATED_PROFILE_ID = 10L;
    private static final Long QUICK_VIEW_PROFILE_ID = 20L;

    @Test
    @DisplayName("URLの対象プロフィールを表示しても認証済みプロフィールを変更しない")
    void authenticatedProfileCanBeViewedWithoutUsingSessionCurrentProfile() {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        HomeService homeService = mock(HomeService.class);
        HomeSummaryChartService homeSummaryChartService = mock(HomeSummaryChartService.class);
        HomeController controller = new HomeController(
                currentUserResolver, homeService, homeSummaryChartService);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, QUICK_VIEW_PROFILE_ID);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        when(currentUserResolver.resolve(session)).thenReturn(user);
        when(homeService.getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID)).thenReturn(Map.of());

        String result = controller.showHomePage(
                QUICK_VIEW_PROFILE_ID, null, null, session, new ConcurrentModel());

        assertEquals("home", result);
        assertEquals(QUICK_VIEW_PROFILE_ID,
                session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID));
        assertEquals(AUTHENTICATED_PROFILE_ID, session.getAttribute(SessionConstants.CURRENT_PROFILE_ID));
        org.mockito.Mockito.verify(homeService).getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID);
    }

    @Test
    @DisplayName("他のPIN付きプロフィールを表示しても認証済みプロフィールを維持する")
    void viewingAnotherPinnedProfileDoesNotChangeAuthentication() {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        HomeService homeService = mock(HomeService.class);
        HomeSummaryChartService homeSummaryChartService = mock(HomeSummaryChartService.class);
        HomeController controller = new HomeController(
                currentUserResolver, homeService, homeSummaryChartService);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        when(currentUserResolver.resolve(session)).thenReturn(user);
        when(homeService.getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID)).thenReturn(Map.of());

        String result = controller.showHomePage(
                QUICK_VIEW_PROFILE_ID, null, null, session, new ConcurrentModel());

        assertEquals("home", result);
        assertEquals(AUTHENTICATED_PROFILE_ID,
                session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID));
        assertEquals(AUTHENTICATED_PROFILE_ID, session.getAttribute(SessionConstants.CURRENT_PROFILE_ID));
        org.mockito.Mockito.verify(homeService).getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID);
    }

    @Test
    @DisplayName("URLで指定された別プロフィールもPINを再要求せず表示する")
    void directAccessToOtherPinnedProfileUsesUrlProfileAfterAuthentication() {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
        HomeService homeService = mock(HomeService.class);
        HomeSummaryChartService homeSummaryChartService = mock(HomeSummaryChartService.class);
        HomeController controller = new HomeController(
                currentUserResolver, homeService, homeSummaryChartService);
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(USER_ID);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, AUTHENTICATED_PROFILE_ID);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, QUICK_VIEW_PROFILE_ID);
        when(currentUserResolver.resolve(session)).thenReturn(user);
        when(homeService.getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID)).thenReturn(Map.of());

        String result = controller.showHomePage(
                QUICK_VIEW_PROFILE_ID, null, null, session, new ConcurrentModel());

        assertEquals("home", result);
        verify(homeService).getHomeData(QUICK_VIEW_PROFILE_ID, USER_ID);
    }
}
