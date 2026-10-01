package com.healthlog.demo.service.weight;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class BmiCalculator {

    private static final BigDecimal UNDERWEIGHT_LIMIT = new BigDecimal("18.5");
    private static final BigDecimal NORMAL_LIMIT = new BigDecimal("25.0");
    private static final BigDecimal OBESE_LIMIT = new BigDecimal("30.0");

    public record BmiStatus(String label, String code) {
    }

    public BigDecimal calculateBMI(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal heightMeter = height.divide(
                BigDecimal.valueOf(100),
                4,
                RoundingMode.HALF_UP);

        return weight.divide(
                heightMeter.multiply(heightMeter),
                2,
                RoundingMode.HALF_UP);
    }

    public BmiStatus statusOf(BigDecimal bmi) {
        if (bmi == null) {
            return null;
        }
        if (bmi.compareTo(UNDERWEIGHT_LIMIT) < 0) {
            return new BmiStatus("低体重", "underweight");
        }
        if (bmi.compareTo(NORMAL_LIMIT) < 0) {
            return new BmiStatus("普通体重", "normal");
        }
        if (bmi.compareTo(OBESE_LIMIT) < 0) {
            return new BmiStatus("肥満(1度)", "warning");
        }
        return new BmiStatus("肥満(2度以上)", "obese");
    }
}