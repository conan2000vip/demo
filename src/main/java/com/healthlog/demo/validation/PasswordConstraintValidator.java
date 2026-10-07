package com.healthlog.demo.validation;

import java.util.List;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 64;

  private static final Pattern LOWERCASE = Pattern.compile(".*[a-z].*");
  private static final Pattern UPPERCASE = Pattern.compile(".*[A-Z].*");
  private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
  private static final Pattern SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
  private static final Pattern WHITESPACE = Pattern.compile(".*\\s.*");
  private static final Pattern REPEATED_CHAR = Pattern.compile("(.)\\1{2,}"); // 同じ文字を3回以上連続して使用

  // よく使われるパスワードの一覧。長くなる場合はDBや設定ファイルで管理する
  private static final List<String> COMMON_PASSWORDS = List.of("12345678", "password", "password123", "qwertyui",
      "11111111", "abc12345");

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.isEmpty()) {
      return true; // @NotBlankに任せ、重複したエラーを避ける
    }

    context.disableDefaultConstraintViolation();
    boolean isValid = true;

    // 条件1: 最小文字数
    if (password.length() < MIN_LENGTH) {
      addViolation(context, "パスワードは8文字以上で入力してください");
      isValid = false;
    }

    // 条件2: 最大文字数
    if (password.length() > MAX_LENGTH) {
      addViolation(context, "パスワードは64文字以内で入力してください");
      isValid = false;
    }

    // 条件3: 小文字
    if (!LOWERCASE.matcher(password).matches()) {
      addViolation(context, "小文字を1文字以上入力してください");
      isValid = false;
    }

    // 条件4: 大文字
    if (!UPPERCASE.matcher(password).matches()) {
      addViolation(context, "大文字を1文字以上入力してください");
      isValid = false;
    }

    // 条件5: 数字
    if (!DIGIT.matcher(password).matches()) {
      addViolation(context, "数字を1文字以上入力してください");
      isValid = false;
    }

    // 条件6: 記号
    if (!SPECIAL.matcher(password).matches()) {
      addViolation(context, "記号を1文字以上入力してください");
      isValid = false;
    }

    // 条件7: 空白を含まない
    if (WHITESPACE.matcher(password).matches()) {
      addViolation(context, "パスワードに空白は使用できません");
      isValid = false;
    }

    // 条件9: 同じ文字を3回以上連続して使用しない
    if (REPEATED_CHAR.matcher(password).find()) {
      addViolation(context, "同じ文字を3回以上連続して使用できません");
      isValid = false;
    }

    // 条件10: よく使われるパスワード一覧に含まれない
    if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
      addViolation(context, "このパスワードは一般的すぎるため使用できません");
      isValid = false;
    }

    return isValid;
  }

  private void addViolation(ConstraintValidatorContext context, String message) {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}