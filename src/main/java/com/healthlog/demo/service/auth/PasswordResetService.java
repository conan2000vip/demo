package com.healthlog.demo.service.auth;

import com.healthlog.demo.dto.auth.PasswordResetConfirmRequest;

public interface PasswordResetService {
    void sendPasswordResetEmail(String email);
    void resetPassword(String email, PasswordResetConfirmRequest request);
}