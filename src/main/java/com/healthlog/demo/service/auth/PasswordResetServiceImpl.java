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
import com.healthlog.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final AuthToken.TokenType TOKEN_TYPE_OTP = AuthToken.TokenType.PASSWORD_RESET_OTP;
    private static final AuthToken.TokenType TOKEN_TYPE_RESET = AuthToken.TokenType.PASSWORD_RESET;
    private static final long OTP_EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
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

        // メールアドレスが登録されていない場合でも、セキュリティ上の理由から成功として返す
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // 既存の未使用OTPを無効化
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
    public void resetPassword(String resetToken, PasswordResetConfirmRequest request) {
        // パスワードと確認用パスワードが一致するかを確認
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "パスワードと確認用パスワードが一致しません");
        }

        // resetTokenが有効かどうかを確認
        if (!StringUtils.hasText(resetToken)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "有効なリクエストではありません。最初からやり直してください");
        }

        AuthToken authToken = authTokenRepository.findByTokenAndTokenType(resetToken, TOKEN_TYPE_RESET)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "有効なリクエストではありません。もう一度お試しください。"));

        if (authToken.isUsedFlg()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "この確認コードはすでに使用されています。再度リクエストしてください");
        }
        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "確認コードの有効期限が切れています。新しいコードを再発行してください");
        }

        // 3. 新しいパスワードを更新
        User user = authToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 4. トークンを無効化
        authToken.setUsedFlg(true);
        authTokenRepository.save(authToken);
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(1_000_000);
        return String.format("%06d", otp);
    }
}