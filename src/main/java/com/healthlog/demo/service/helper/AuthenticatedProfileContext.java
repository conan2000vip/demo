package com.healthlog.demo.service.helper;

import com.healthlog.demo.constant.SessionConstants;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class AuthenticatedProfileContext {

    private final HttpSession session;

    public Long getProfileId() {
        return (Long) TabProfileContext.getAttribute(session, SessionConstants.AUTHENTICATED_PROFILE_ID);
    }
}
