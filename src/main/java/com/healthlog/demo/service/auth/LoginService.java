package com.healthlog.demo.service.auth;
import com.healthlog.demo.dto.auth.LoginRequest;
import com.healthlog.demo.entity.User;

public interface LoginService {
    User login(LoginRequest request);
}