package com.healthlog.demo.config;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.healthlog.demo.constant.SessionConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class ProfilePinAuthenticationInterceptor implements HandlerInterceptor {
    private static final Pattern PROFILE_SELECTION_OR_PIN_ROUTE = Pattern.compile(
            "^/profile/(?:select|new|\\d+/select|\\d+/verify-pin(?:/account-password)?)$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID) != null) {
            return true;
        }

        String path = getApplicationPath(request);
        if (PROFILE_SELECTION_OR_PIN_ROUTE.matcher(path).matches()) {
            return true;
        }

        if (isJsonRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"PIN認証が必要です。\"}");
            return false;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String query = request.getQueryString();
            session.setAttribute(SessionConstants.PIN_RETURN_TO, path + (query == null ? "" : "?" + query));
        }

        Long currentProfileId = (Long) session.getAttribute(SessionConstants.CURRENT_PROFILE_ID);
        String redirectPath = currentProfileId == null
                ? "/profile/select"
                : "/profile/" + currentProfileId + "/select";
        response.sendRedirect(request.getContextPath() + redirectPath);
        return false;
    }

    private String getApplicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return requestUri.substring(contextPath.length());
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}
