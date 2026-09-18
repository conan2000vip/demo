package com.healthlog.demo.dto.auth;

import com.healthlog.demo.validation.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "メールアドレスを入力してください")
    @Email(message = "メールアドレスの形式が正しくありません")
    private String email;

    @NotBlank(message = "パスワードを入力してください")
    @ValidPassword
    private String password;

    @NotBlank(message = "パスワード（確認）を入力してください")
    private String confirmPassword;
}