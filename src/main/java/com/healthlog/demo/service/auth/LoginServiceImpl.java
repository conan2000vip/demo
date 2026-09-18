package com.healthlog.demo.service.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.auth.LoginRequest;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.UserRepository;
import com.healthlog.demo.util.SecurityContextUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextUtil securityContextUtil;

    @Override
    @Transactional(readOnly = true)
    public void login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. DBからユーザーを検索（サービス層ではEntityを使用）
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが正しくありません"));

        // 2. パスワードを確認
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "メールアドレスまたはパスワードが正しくありません");
        }

        // 3. メール認証済みか確認
        if (user.getEmailVerifiedAt() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "メール認証を完了してください");
        }

        // 4. セキュアなセッションを設定
        securityContextUtil.authenticateUser(user.getEmail());
    }
}