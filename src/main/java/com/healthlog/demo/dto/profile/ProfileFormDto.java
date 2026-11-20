package com.healthlog.demo.dto.profile;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileFormDto {

    private Long id;

    @NotBlank(message = "名前を入力してください")
    @Size(max = 100, message = "名前は100文字以内で入力してください")
    private String name;

    @NotNull(message = "生年月日を入力してください")
    @PastOrPresent(message = "生年月日には未来の日付を指定できません")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String relationship;

    private String gender;

    @NotNull(message = "身長を入力してください")
    @DecimalMin(value = "10", message = "身長は10〜350cmの範囲で入力してください")
    @DecimalMax(value = "350.0", message = "身長は10〜350cmの範囲で入力してください")
    @Digits(integer = 3, fraction = 1, message = "小数点以下1桁まで入力してください")
    private BigDecimal height;

    @DecimalMin(value = "1", message = "体重目標は1〜700kgの範囲で入力してください")
    @DecimalMax(value = "700.0", message = "体重目標は1〜700kgの範囲で入力してください")
    @Digits(integer = 3, fraction = 1, message = "小数点以下1桁まで入力してください")
    private BigDecimal targetWeight;

    @Min(value = 0, message = "水分目標は0〜10000mlの範囲で入力してください")
    @DecimalMax(value = "10000", message = "水分目標は0〜10000mlの範囲で入力してください")
    @Digits(integer = 5, fraction = 0, message = "水分目標は数値で入力してください（整数のみ）")
    private Integer waterGoalMl;

    @Min(value = 0, message = "歩数目標は0〜200000歩の範囲で入力してください")
    @DecimalMax(value = "200000", message = "歩数目標は0〜200000歩の範囲で入力してください")
    @Digits(integer = 6, fraction = 0, message = "歩数目標は数値で入力してください（整数のみ）")
    private Integer stepGoal;

    @DecimalMin(value = "0", message = "睡眠目標は0〜24の範囲で入力してください")
    @DecimalMax(value = "24", message = "睡眠目標は0〜24の範囲で入力してください")
    @Digits(integer = 2, fraction = 1, message = "小数点以下1桁まで入力してください")
    private BigDecimal dailySleepGoal;

    private String avatar = "avatar_01";

    private boolean isPrimary = false;

    private boolean managedByPrimary = true;

    private boolean pinEnabled;

    private String pin;

    private String pinConfirmation;

    private String currentPin;

    private String accountPassword;
}