package com.healthlog.demo.service.auth;

import com.healthlog.demo.dto.auth.VerifyCodeRequest;

public interface VerifyCodeService {
    void verifyCode(VerifyCodeRequest request);
}