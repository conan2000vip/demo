package com.healthlog.demo.service.auth;
import com.healthlog.demo.dto.auth.LoginRequest;

public interface LoginService {
    void login(LoginRequest request);
}