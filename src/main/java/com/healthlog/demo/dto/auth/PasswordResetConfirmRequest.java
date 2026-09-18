package com.healthlog.demo.dto.auth;

import com.healthlog.demo.validation.ValidPassword;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {

    @NotBlank(message = "新しいパスワードを入力してください")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "確認用パスワードを入力してください")
    private String confirmPassword;
}