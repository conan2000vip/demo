package com.healthlog.demo.service.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.healthlog.demo.constant.SessionConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class TabProfileContext {

    private static final String TAB_STATES = "TAB_PROFILE_STATES";
    private static final String TAB_ID_PARAMETER = "tabId";

    private TabProfileContext() {
    }

    public static String getTabId(HttpServletRequest request) {
        return validTabId(request.getParameter(TAB_ID_PARAMETER));
    }

    public static String getCurrentTabId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return getTabId(attributes.getRequest());
    }

    public static String ensureTabId(HttpServletRequest request) {
        String tabId = getTabId(request);
        return tabId != null ? tabId : UUID.randomUUID().toString();
    }

    public static void initializeFromLegacy(HttpSession session, String tabId) {
        synchronized (session) {
            if (session.getAttribute(TAB_STATES) instanceof Map<?, ?> states && states.containsKey(tabId)) {
                return;
            }
            Map<String, Object> initialState = new HashMap<>();
            copySessionValue(session, initialState, SessionConstants.CURRENT_PROFILE_ID);
            copySessionValue(session, initialState, SessionConstants.AUTHENTICATED_PROFILE_ID);
            copySessionValue(session, initialState, SessionConstants.PIN_RETURN_TO);
            saveTabState(session, tabId, initialState);
        }
    }

    public static Object getAttribute(HttpSession session, String name) {
        String tabId = getCurrentTabId();
        if (tabId == null) {
            return session.getAttribute(name);
        }
        synchronized (session) {
            return getTabState(session, tabId).get(name);
        }
    }

    public static void setAttribute(HttpSession session, String name, Object value) {
        String tabId = getCurrentTabId();
        if (tabId == null) {
            session.setAttribute(name, value);
            return;
        }
        synchronized (session) {
            Map<String, Object> state = getTabState(session, tabId);
            state.put(name, value);
            saveTabState(session, tabId, state);
        }
    }

    public static void removeAttribute(HttpSession session, String name) {
        String tabId = getCurrentTabId();
        if (tabId == null) {
            session.removeAttribute(name);
            return;
        }
        synchronized (session) {
            Map<String, Object> state = getTabState(session, tabId);
            state.remove(name);
            saveTabState(session, tabId, state);
        }
    }

    public static String appendTabId(HttpServletRequest request, String path) {
        return appendTabId(getTabId(request), path);
    }

    public static String appendCurrentTabId(String path) {
        return appendTabId(getCurrentTabId(), path);
    }

    private static String appendTabId(String tabId, String path) {
        if (tabId == null || path == null || path.isBlank()) {
            return path;
        }
        if (path.matches(".*(?:\\?|&)tabId=[^&]*.*")) {
            return path;
        }
        String separator = path.contains("?") ? "&" : "?";
        return path + separator + TAB_ID_PARAMETER + "=" + tabId;
    }

    private static String validTabId(String tabId) {
        if (tabId == null || tabId.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(tabId).toString().equalsIgnoreCase(tabId) ? tabId : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Map<String, Object> getTabState(HttpSession session, String tabId) {
        Object value = session.getAttribute(TAB_STATES);
        if (value instanceof Map<?, ?> states) {
            Object tabState = states.get(tabId);
            if (tabState instanceof Map<?, ?> state) {
                Map<String, Object> result = new HashMap<>();
                state.forEach((key, item) -> result.put((String) key, item));
                return result;
            }
        }
        return new HashMap<>();
    }

    private static void saveTabState(HttpSession session, String tabId, Map<String, Object> tabState) {
        Map<String, Map<String, Object>> states = new HashMap<>();
        Object value = session.getAttribute(TAB_STATES);
        if (value instanceof Map<?, ?> existingStates) {
            existingStates.forEach((key, state) -> {
                if (key instanceof String stateTabId && state instanceof Map<?, ?> values) {
                    Map<String, Object> copiedState = new HashMap<>();
                    values.forEach((stateKey, stateValue) -> copiedState.put((String) stateKey, stateValue));
                    states.put(stateTabId, copiedState);
                }
            });
        }
        states.put(tabId, new HashMap<>(tabState));
        session.setAttribute(TAB_STATES, states);
    }

    private static void copySessionValue(HttpSession session, Map<String, Object> target, String name) {
        Object value = session.getAttribute(name);
        if (value != null) {
            target.put(name, value);
        }
    }
}
