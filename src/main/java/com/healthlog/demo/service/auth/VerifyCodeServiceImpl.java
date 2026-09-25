package com.healthlog.demo.service.auth;

import java.security.SecureRandom;
import java.time.Duration;
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
    private static final int OTP_EXPIRY_MINUTES = 30;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void verifyCode(VerifyCodeRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "確認コードを入力してください。");
        }
        String email = request.getEmail().trim().toLowerCase();
        String inputCode = request.getCode().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "確認コードが正しくないか、有効期限が切れています。"));
        List<AuthToken> otpTokens = new java.util.ArrayList<>(authTokenRepository
                .findByUser_IdAndTokenTypeAndUsedFlgFalse(user.getId(), AuthToken.TokenType.EMAIL_VERIFICATION));
        otpTokens.addAll(authTokenRepository.findByUser_IdAndTokenTypeAndUsedFlgFalse(user.getId(), TOKEN_TYPE_OTP));
        @SuppressWarnings("null")
        AuthToken otpToken = otpTokens.stream()
                .max(Comparator.comparing(AuthToken::getCreatedAt))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                        "確認コードが正しくないか、すでに有効期限が切れています。新しいコードを再発行してください。"));
        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "確認コードの有効期限が切れています。新しいコードを再発行してください。");
        }
        if (!otpToken.getToken().trim().equals(inputCode)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "確認コードが正しくありません。もう一度入力してください。");
        }
        otpToken.setUsedFlg(true);
        authTokenRepository.save(otpToken);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void resendCode(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "メールアドレスが見つかりません。"));

        AuthToken.TokenType tokenType = (user.getEmailVerifiedAt() == null)
                ? AuthToken.TokenType.EMAIL_VERIFICATION
                : TOKEN_TYPE_OTP;

        List<AuthToken> existingTokens = authTokenRepository
                .findByUser_IdAndTokenTypeAndUsedFlgFalse(user.getId(), tokenType);

        existingTokens.stream()
                .max(Comparator.comparing(AuthToken::getCreatedAt))
                .ifPresent(lastToken -> {
                    long seconds = Duration.between(lastToken.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    if (seconds < RESEND_COOLDOWN_SECONDS) {
                        throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "しばらく時間をおいてから再度お試しください。");
                    }
                });

        existingTokens.forEach(token -> {
            token.setUsedFlg(true);
            authTokenRepository.save(token);
        });

        String newCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        AuthToken newToken = new AuthToken(user, tokenType, newCode,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        authTokenRepository.save(newToken);

        if (tokenType == AuthToken.TokenType.EMAIL_VERIFICATION) {
            emailService.sendRegistrationOtpEmail(user.getEmail(), newCode);
        } else {
            emailService.sendPasswordResetOtpEmail(user.getEmail(), newCode);
        }
    }
}