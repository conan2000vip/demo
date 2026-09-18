package com.healthlog.demo.service.auth;

public interface EmailService {
    void sendPasswordResetOtpEmail(String toEmail, String otp);
    void sendRegistrationOtpEmail(String toEmail, String otp);
}
