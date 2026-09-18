package com.healthlog.demo.util;

import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityContextUtil {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    // ログインセッションを初期化し、ユーザーのメールアドレスでSPRING_SESSIONデータベースに保存する。
    public void authenticateUser(String email) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        if (response != null) {
            securityContextRepository.saveContext(context, request, response);
        }
    }

    /**
     * Xóa sạch Security Context khi đăng xuất.
     */
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
