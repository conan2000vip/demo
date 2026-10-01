package com.healthlog.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.healthlog.demo.entity.Weight;
import com.healthlog.demo.service.weight.BmiCalculator.BmiStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeightSummary {

	private Long id;
	private LocalDate recordedDate;
	private LocalDateTime measuredAt;
	private BigDecimal weight;
	private BigDecimal height;
	private String memo;
	private BigDecimal bmi;
	private String bmiStatus;
	private String bmiStatusCode;

	public static WeightSummary from(Weight weight, BigDecimal bmi, BmiStatus status) {
		return new WeightSummary(
				weight.getId(),
				weight.getRecordedDate(),
				weight.getMeasuredAt(),
				weight.getWeight(),
				weight.getHeight(),
				weight.getMemo(),
				bmi,
				status != null ? status.label() : null,
				status != null ? status.code() : null);
	}
}
