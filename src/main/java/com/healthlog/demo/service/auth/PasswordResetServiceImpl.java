package com.healthlog.demo.service.auth;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.healthlog.demo.dto.auth.PasswordResetConfirmRequest;
import com.healthlog.demo.entity.AuthToken;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.AuthTokenRepository;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final AuthToken.TokenType TOKEN_TYPE_OTP = AuthToken.TokenType.PASSWORD_RESET_OTP;
    private static final long OTP_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    // OTP発行処理（既存の未使用OTPを無効化して新しいOTPを発行）
    public void sendPasswordResetEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メールアドレスを入力してください");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メールアドレスの形式が正しくありません");
        }

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            List<AuthToken> oldTokens = authTokenRepository
                    .findByUser_IdAndTokenTypeAndUsedFlgFalse(user.getId(), TOKEN_TYPE_OTP);
            oldTokens.forEach(t -> t.setUsedFlg(true));
            authTokenRepository.saveAll(oldTokens);

            String otp = generateOtp();
            AuthToken otpToken = new AuthToken();
            otpToken.setUser(user);
            otpToken.setToken(otp);
            otpToken.setTokenType(TOKEN_TYPE_OTP);
            otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            otpToken.setUsedFlg(false);
            authTokenRepository.save(otpToken);
            emailService.sendPasswordResetOtpEmail(user.getEmail(), otp);
        });
    }

    @Override
    @Transactional
    // 再設定トークンの代わりにメールアドレスを使用してパスワードを更新する。
    public void resetPassword(String email, PasswordResetConfirmRequest request, Long pinProfileId) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "有効なリクエストではありません。最初からやり直してください。");
        }

        // 1. パスワードと確認用パスワードが一致するかを確認
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "パスワードと確認用パスワードが一致しません。");
        }

        // 2. メールアドレスからユーザーを取得
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "ユーザーが見つかりません。"));

        // 3. 新しいパスワードを暗号化して更新
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        if (pinProfileId != null) {
            var profile = profileRepository.findByIdAndUser_Id(pinProfileId, user.getId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                            "対象のプロファイルが見つかりません。"));
            profile.setPinHash(null);
            profileRepository.save(profile);
        }

        // 4. このユーザーの未使用OTPトークンをすべて無効化 (クリーンアップ)
        List<AuthToken> activeTokens = authTokenRepository
                .findByUser_IdAndTokenTypeAndUsedFlgFalse(user.getId(), TOKEN_TYPE_OTP);
        activeTokens.forEach(t -> t.setUsedFlg(true));
        authTokenRepository.saveAll(activeTokens);
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(1_000_000);
        return String.format("%06d", otp);
    }
}