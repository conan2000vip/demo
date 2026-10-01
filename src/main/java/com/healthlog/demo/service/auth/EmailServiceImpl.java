package com.healthlog.demo.service.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = buildOtpMessage(
                toEmail,
                "【HealthLog】パスワード再設定の認証コード",
                "パスワード再設定のため、以下の認証コードをご入力ください。",
                otp);
        mailSender.send(message);
        log.info("パスワード再設定用の認証コードを送信しました");
    }

    @Override
    public void sendRegistrationOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = buildOtpMessage(
                toEmail,
                "【HealthLog】会員登録の確認コード",
                "会員登録を完了するため、以下の認証コードをご入力ください。",
                otp);
        mailSender.send(message);
        log.info("会員登録用の認証コードを送信しました");
    }

    private SimpleMailMessage buildOtpMessage(String toEmail, String subject, String intro, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom("no-reply@healthlog.com");
        message.setSubject(subject);
        message.setText(
                intro + "\n\n" +
                        "確認コード: " + otp + "\n\n" +
                        "このコードの有効期限は30分間です。\n" +
                        "本メールに心当たりがない場合は、破棄していただいて問題ございません。");
        return message;
    }
}