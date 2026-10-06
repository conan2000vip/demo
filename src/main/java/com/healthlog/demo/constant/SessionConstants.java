package com.healthlog.demo.constant;

public final class SessionConstants {

    private SessionConstants() {
    }

    public static final String CURRENT_PROFILE_ID = "CURRENT_PROFILE_ID";
    /**
     * セッションを認証したプロファイル。
     * 現在表示中のプロファイルを表すCURRENT_PROFILE_IDとは分離して管理する。
     */
    public static final String AUTHENTICATED_PROFILE_ID = "AUTHENTICATED_PROFILE_ID";
    public static final String LOGIN_USER = "LOGIN_USER";
    public static final String RESET_EMAIL = "RESET_EMAIL";
    public static final String IS_RESET_FLOW = "IS_RESET_FLOW";
}