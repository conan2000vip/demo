package com.healthlog.demo.dto.profile;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @DecimalMin(value = "0.1", message = "身長は0より大きい値を入力してください")
    @DecimalMax(value = "300.0", message = "身長の値が正しくありません")
    private BigDecimal height;

    @DecimalMin(value = "0.1", message = "目標体重は0より大きい値を入力してください")
    private BigDecimal targetWeight;

    @Min(value = 0, message = "水分目標は0以上の値を入力してください")
    private Integer waterGoalMl;

    @Min(value = 0, message = "歩数目標は0以上の値を入力してください")
    private Integer stepGoal;

    @DecimalMin(value = "0.0", message = "睡眠目標は0〜24の範囲で入力してください")
    @DecimalMax(value = "24.0", message = "睡眠目標は0〜24の範囲で入力してください")
    private BigDecimal dailySleepGoal;

    private String avatar = "avatar_01";

    private boolean isPrimary = false;
}