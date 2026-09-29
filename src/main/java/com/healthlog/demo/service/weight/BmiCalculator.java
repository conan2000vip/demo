package com.healthlog.demo.service.weight;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class BmiCalculator {

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
}
