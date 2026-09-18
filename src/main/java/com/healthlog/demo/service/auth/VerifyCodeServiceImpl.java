package com.healthlog.demo.service.auth;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.auth.VerifyCodeRequest;
import com.healthlog.demo.entity.AuthToken;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.AuthTokenRepository;
import com.healthlog.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerifyCodeServiceImpl implements VerifyCodeService {

    private static final AuthToken.TokenType TOKEN_TYPE_OTP = AuthToken.TokenType.PASSWORD_RESET_OTP;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;

    @Override
    @Transactional
    public void verifyCode(VerifyCodeRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. ユーザーをメールアドレスで検索
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "確認コードが正しくないか、有効期限が切れています。"));

        // 2. 未使用のOTPトークンを取得
        List<AuthToken> otpTokens = authTokenRepository
                .findByUser_IdAndTokenTypeAndUsedFlgFalse(user.getId(), TOKEN_TYPE_OTP);

        @SuppressWarnings("null")
        AuthToken otpToken = otpTokens.stream()
                .max(Comparator.comparing(AuthToken::getCreatedAt))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "確認コードが正しくないか、すでに有効期限が切れています。新しいコードを再発行してください。"));

        // 3. 有効期限とOTPコードを検証
        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "確認コードの有効期限が切れています。新しいコードを再発行してください。");
        }
        if (!otpToken.getToken().equals(request.getCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "確認コードが正しくありません。もう一度入力してください。");
        }

        // 4. 既存の未使用OTPを無効化
        otpToken.setUsedFlg(true);
        authTokenRepository.save(otpToken);
    }
}