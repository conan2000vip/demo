package com.healthlog.demo.service.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.healthlog.demo.constant.SessionConstants;

class TabProfileContextTest {

    private static final String TAB_A = "11111111-1111-4111-8111-111111111111";
    private static final String TAB_B = "22222222-2222-4222-8222-222222222222";

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void profileAndPinStateAreIndependentBetweenTabs() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 10L);
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, 10L);

        bindTab(TAB_A);
        TabProfileContext.initializeFromLegacy(session, TAB_A);
        TabProfileContext.setAttribute(session, SessionConstants.CURRENT_PROFILE_ID, 45L);
        TabProfileContext.setAttribute(session, SessionConstants.AUTHENTICATED_PROFILE_ID, 45L);

        bindTab(TAB_B);
        TabProfileContext.initializeFromLegacy(session, TAB_B);
        assertEquals(10L, TabProfileContext.getAttribute(session, SessionConstants.CURRENT_PROFILE_ID));
        TabProfileContext.setAttribute(session, SessionConstants.CURRENT_PROFILE_ID, 30L);
        TabProfileContext.setAttribute(session, SessionConstants.AUTHENTICATED_PROFILE_ID, 30L);

        bindTab(TAB_A);
        assertEquals(45L, TabProfileContext.getAttribute(session, SessionConstants.CURRENT_PROFILE_ID));
        assertEquals(45L, TabProfileContext.getAttribute(session, SessionConstants.AUTHENTICATED_PROFILE_ID));

        bindTab(TAB_B);
        assertEquals(30L, TabProfileContext.getAttribute(session, SessionConstants.CURRENT_PROFILE_ID));
        assertEquals(30L, TabProfileContext.getAttribute(session, SessionConstants.AUTHENTICATED_PROFILE_ID));
        assertEquals(10L, session.getAttribute(SessionConstants.CURRENT_PROFILE_ID));
    }

    @Test
    void tabIdIsPreservedAcrossProfileNavigationUrls() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("tabId", TAB_A);

        assertEquals("/profile/45/home?tabId=" + TAB_A,
                TabProfileContext.appendTabId(request, "/profile/45/home"));
        assertEquals("/profile/45/home?tabId=" + TAB_A,
                TabProfileContext.appendTabId(request, "/profile/45/home?tabId=" + TAB_A));
    }

    private void bindTab(String tabId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("tabId", tabId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
