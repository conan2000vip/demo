package com.healthlog.demo.service.weight;

import java.math.BigDecimal;

public interface WeightFeedback {

	BmiStatus statusOf(BigDecimal bmi);

	record BmiStatus(String label, String code) {
	}
}
