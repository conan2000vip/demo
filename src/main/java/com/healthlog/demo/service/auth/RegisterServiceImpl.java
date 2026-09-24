package com.healthlog.demo.service.auth;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.auth.RegisterRequest;
import com.healthlog.demo.entity.AuthToken;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.AuthTokenRepository;
import com.healthlog.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 30;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. パスワードと確認用パスワードが一致するか確認
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "パスワードと確認用パスワードが一致しません");
        }

        String email = request.getEmail().trim().toLowerCase();

        // 2. メールアドレスが既に登録されているか確認
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "登録処理を完了できませんでした。入力内容をご確認ください。");
        }

        // 3. 新しいUserを作成 (emailVerifiedAtはまだ設定されていない)
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // 4. OTPを生成してAuthTokenを作成し、DBに保存
        String otpCode = generateOtp();
        AuthToken verifyToken = new AuthToken(
                user,
                AuthToken.TokenType.EMAIL_VERIFICATION,
                otpCode,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        authTokenRepository.save(verifyToken);

        // 5. 既存のメール送信関数を呼び出す
        emailService.sendRegistrationOtpEmail(user.getEmail(), otpCode);
        log.info("[REGISTER] Sent registration OTP to {}", email);
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(1_000_000);
        return String.format("%06d", otp);
    }
}