package com.healthlog.demo.service.helper;

import org.springframework.stereotype.Component;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.util.SecurityContextUtil;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {
    private final SecurityContextUtil securityContextUtil;
    private final UserRepository userRepository;

    public User resolve(HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (user != null)
            return user;

        String email = securityContextUtil.getCurrentUserEmail();
        if (email == null)
            return null;
        user = userRepository.findByEmail(email).orElse(null);
        if (user != null)
            session.setAttribute(SessionConstants.LOGIN_USER, user);
        return user;
    }
}