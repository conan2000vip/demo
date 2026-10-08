package com.healthlog.demo.service.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.healthlog.demo.dto.chartdata.ChartDataResponse;
import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.BaseLog;

@Component
public class ChartDataBuilder {

    public <T extends BaseLog> ChartDataResponse build(List<T> logs, DateRangerFilter dateRange,
            Function<T, BigDecimal> valueExtractor) {
        LocalDate from = dateRange != null ? dateRange.getFrom() : null;
        LocalDate to = dateRange != null ? dateRange.getTo() : null;
        String chartMode = determineChartMode(from, to);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        switch (chartMode) {
        case "HOUR" -> buildHourly(logs, valueExtractor, labels, values);
        case "DAY" -> buildDaily(logs, valueExtractor, from, to, labels, values);
        case "WEEK" -> buildWeekly(logs, valueExtractor, from, to, labels, values);
        case "MONTH" -> buildMonthly(logs, valueExtractor, from, to, labels, values);
        case "YEAR" -> buildYearly(logs, valueExtractor, from, to, labels, values);
        default -> throw new IllegalArgumentException("Unsupported chart mode: " + chartMode);
        }
        return new ChartDataResponse(labels, values, chartMode);
    }

    private String determineChartMode(LocalDate from, LocalDate to) {
        if (from == null || to == null)
            return "DAY";
        if (from.equals(to))
            return "HOUR";
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 7)
            return "DAY";
        if (days <= 31)
            return "WEEK";
        if (days <= 730)
            return "MONTH";
        return "YEAR";
    }

    private <T extends BaseLog> void buildHourly(List<T> logs, Function<T, BigDecimal> valueExtractor,
            List<String> labels, List<BigDecimal> values) {
        Map<Integer, T> hourly = new TreeMap<>();
        for (T log : logs) {
            if (log.getMeasuredAt() == null)
                continue;
            int hour = log.getMeasuredAt().getHour();
            T existing = hourly.get(hour);
            if (existing == null || log.getMeasuredAt().isAfter(existing.getMeasuredAt())) {
                hourly.put(hour, log);
            }
        }
        for (int hour = 0; hour < 24; hour++) {
            labels.add(String.format("%02d:00", hour));
            T log = hourly.get(hour);
            values.add(log != null ? valueExtractor.apply(log) : null);
        }
    }

    private <T extends BaseLog> void buildDaily(List<T> logs, Function<T, BigDecimal> valueExtractor, LocalDate from,
            LocalDate to, List<String> labels, List<BigDecimal> values) {
        Map<LocalDate, T> daily = latestPerDate(logs);
        if (from == null || to == null) {
            daily.forEach((date, log) -> {
                labels.add(date.toString());
                values.add(valueExtractor.apply(log));
            });
            return;
        }
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            labels.add(date.toString());
            T log = daily.get(date);
            values.add(log != null ? valueExtractor.apply(log) : null);
        }
    }

    private <T extends BaseLog> void buildWeekly(List<T> logs, Function<T, BigDecimal> valueExtractor, LocalDate from,
            LocalDate to, List<String> labels, List<BigDecimal> values) {
        Map<LocalDate, T> daily = latestPerDate(logs);
        for (LocalDate start = from; !start.isAfter(to); start = start.plusDays(7)) {
            LocalDate end = start.plusDays(6).isAfter(to) ? to : start.plusDays(6);
            labels.add(start.toString());
            List<BigDecimal> weekValues = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                T log = daily.get(date);
                if (log != null)
                    weekValues.add(valueExtractor.apply(log));
            }
            values.add(average(weekValues));
        }
    }

    private <T extends BaseLog> void buildMonthly(List<T> logs, Function<T, BigDecimal> valueExtractor, LocalDate from,
            LocalDate to, List<String> labels, List<BigDecimal> values) {
        Map<String, List<T>> monthly = new TreeMap<>();
        for (T log : latestPerDate(logs).values()) {
            String key = String.format("%04d-%02d", log.getRecordedDate().getYear(),
                    log.getRecordedDate().getMonthValue());
            monthly.computeIfAbsent(key, ignored -> new ArrayList<>()).add(log);
        }
        for (LocalDate month = from.withDayOfMonth(1); !month.isAfter(to); month = month.plusMonths(1)) {
            String key = String.format("%04d-%02d", month.getYear(), month.getMonthValue());
            labels.add(key);
            values.add(average(monthly.getOrDefault(key, List.of()).stream().map(valueExtractor).toList()));
        }
    }

    private <T extends BaseLog> void buildYearly(List<T> logs, Function<T, BigDecimal> valueExtractor, LocalDate from,
            LocalDate to, List<String> labels, List<BigDecimal> values) {
        Map<Integer, List<T>> yearly = new TreeMap<>();
        for (T log : latestPerDate(logs).values()) {
            yearly.computeIfAbsent(log.getRecordedDate().getYear(), ignored -> new ArrayList<>()).add(log);
        }
        for (LocalDate year = from.withDayOfYear(1); !year.isAfter(to); year = year.plusYears(1)) {
            int key = year.getYear();
            labels.add(String.valueOf(key));
            values.add(average(yearly.getOrDefault(key, List.of()).stream().map(valueExtractor).toList()));
        }
    }

    private <T extends BaseLog> Map<LocalDate, T> latestPerDate(List<T> logs) {
        Map<LocalDate, T> latestByDate = new TreeMap<>();
        for (T log : logs) {
            T existing = latestByDate.get(log.getRecordedDate());
            if (existing == null || log.getId() > existing.getId()) {
                latestByDate.put(log.getRecordedDate(), log);
            }
        }
        return latestByDate;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty())
            return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }
}
