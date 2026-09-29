package com.healthlog.demo.dto.chartdata;

import java.math.BigDecimal;
import java.util.List;

public record ChartDataResponse(List<String> labels, List<BigDecimal> values, String chartMode) {
}
