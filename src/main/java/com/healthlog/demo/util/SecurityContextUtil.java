package com.healthlog.demo.util;

import java.util.Collections;
import java.util.List;

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

import com.healthlog.demo.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityContextUtil {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    // 1. メールアドレスを使ってログインセッションを有効化する（ROLE_USERを設定）。
    public void authenticateUser(String email) {
        authenticateUserWithRole(email, "ROLE_USER");
    }

    // 2. Userエンティティを使ってログインセッションを有効化する（ROLE_USERを設定）。
    public void authenticateUser(User user) {
        if (user != null) {
            authenticateUserWithRole(user.getEmail(), "ROLE_USER");
        }
    }

    private void authenticateUserWithRole(String email, String roleName) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleName));

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        if (response != null) {
            securityContextRepository.saveContext(context, request, response);
        }
    }

    // 3. Spring Security Contextからログイン中のユーザーのメールアドレスを取得する。
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    // 4. ログアウト時にログインセッションを完全に削除する。
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}