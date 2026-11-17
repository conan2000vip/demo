package com.healthlog.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.healthlog.demo.constant.SessionConstants;

class ProfilePinAuthenticationInterceptorTest {

    private final ProfilePinAuthenticationInterceptor interceptor = new ProfilePinAuthenticationInterceptor();

    @Test
    void protectedPageRedirectsToCurrentProfilePinScreenAndRemembersRequestedPage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/profile-manage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 30L);
        request.setSession(session);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals("/profile/30/select", response.getRedirectedUrl());
        assertEquals("/profile/profile-manage", session.getAttribute(SessionConstants.PIN_RETURN_TO));
    }

    @Test
    void protectedProfileUrlPromptsForTheProfileInTheRequestedUrl() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/30/sleep");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, 45L);
        request.setSession(session);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals("/profile/30/select", response.getRedirectedUrl());
        assertEquals("/profile/30/sleep", session.getAttribute(SessionConstants.PIN_RETURN_TO));
    }

    @Test
    void profileSelectionAndPinVerificationRemainAccessibleWithoutProfileAuthentication() throws Exception {
        MockHttpServletRequest selectionRequest = new MockHttpServletRequest("GET", "/profile/30/select");
        MockHttpServletRequest verifyRequest = new MockHttpServletRequest("POST", "/profile/30/verify-pin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        selectionRequest.setSession(session);
        verifyRequest.setSession(session);

        assertTrue(interceptor.preHandle(selectionRequest, response, new Object()));
        assertTrue(interceptor.preHandle(verifyRequest, response, new Object()));
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void authenticatedProfileCanAccessProtectedPage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/30/sleep");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, 45L);
        request.setSession(session);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(response.getRedirectedUrl());
    }

    @Test
    void jsonRequestWithoutProfileAuthenticationReceivesUnauthorizedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile/30/sleep/chart-data");
        request.addHeader("Accept", "application/json");
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("PIN認証が必要です"));
    }
}
