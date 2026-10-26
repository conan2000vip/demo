package com.healthlog.demo.service.csv;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvExportServiceImpl implements CsvExportService {

    private static final long MAX_DAYS = 366;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ProfileAccessValidation profileAccessValidation;
    private final WeightRepository weightRepository;
    private final SleepRepository sleepRepository;
    private final WaterRepository waterRepository;
    private final StepRepository stepRepository;

    @Override
    @Transactional(readOnly = true)
    public CsvTable build(Long profileId, Long userId, String type, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "開始日と終了日を指定してください");
        }
        if (start.isAfter(end)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "終了日は開始日以降の日付を指定してください");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "出力できる期間は最大1年です");
        }
        // データ項目ごとに閲覧権限を検証する（"steps" は Category.step に対応）。
        profileAccessValidation.validateAndGetProfile(profileId, userId, toCategory(type));

        CsvTable table = switch (type) {
        case "weight" -> weightTable(profileId, start, end);
        case "sleep" -> sleepTable(profileId, start, end);
        case "water" -> waterTable(profileId, start, end);
        default -> stepTable(profileId, start, end);
        };
        return withNumbers(table);
    }

    // Excel で文字化けしないよう UTF-8 BOM を付ける。
    @Override
    public byte[] toCsvBytes(CsvTable table) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        appendLine(sb, table.columns());
        table.rows().forEach(row -> appendLine(sb, row));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // === 以下は内部処理 ===
    private static CsvTable withNumbers(CsvTable table) {
        List<String> columns = new ArrayList<>();
        columns.add("No");
        columns.addAll(table.columns());
        List<List<String>> rows = new ArrayList<>();
        int no = 1;
        for (List<String> row : table.rows()) {
            List<String> numbered = new ArrayList<>();
            numbered.add(String.valueOf(no++));
            numbered.addAll(row);
            rows.add(numbered);
        }
        return new CsvTable(columns, rows);
    }

    private Category toCategory(String type) {
        if (type == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "データ種別が不正です");
        }
        return switch (type) {
        case "weight" -> Category.weight;
        case "sleep" -> Category.sleep;
        case "water" -> Category.water;
        case "steps" -> Category.step;
        default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "データ種別が不正です");
        };
    }

    private CsvTable weightTable(Long profileId, LocalDate start, LocalDate end) {
        List<List<String>> rows = weightRepository
                .findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(profileId, start, end).stream()
                .map(w -> List.of(str(w.getRecordedDate()),
                        w.getMeasuredAt() == null ? "" : w.getMeasuredAt().format(TIME), str(w.getWeight()),
                        str(w.getHeight()), bmi(w.getWeight(), w.getHeight()), text(w.getMemo())))
                .toList();
        return new CsvTable(List.of("日付", "時間", "体重(kg)", "身長(cm)", "BMI", "メモ"), rows);
    }

    private CsvTable sleepTable(Long profileId, LocalDate start, LocalDate end) {
        List<List<String>> rows = sleepRepository
                .findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(profileId, start, end).stream()
                .map(s -> List.of(str(s.getRecordedDate()),
                        s.getSleepType() != null && "NAP".equals(s.getSleepType().name()) ? "昼寝" : "夜間",
                        str(s.getStartTime()), str(s.getEndTime()), str(s.getSleepMinutes()), text(s.getMemo())))
                .toList();
        return new CsvTable(List.of("日付", "種別", "就寝", "起床", "睡眠時間(分)", "メモ"), rows);
    }

    private CsvTable waterTable(Long profileId, LocalDate start, LocalDate end) {
        List<List<String>> rows = waterRepository
                .findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(profileId, start, end).stream()
                .map(w -> List.of(str(w.getRecordedDate()),
                        w.getRecordedTime() == null ? "" : w.getRecordedTime().format(TIME),
                        text(str(w.getDrinkType())), str(w.getAmountMl()), text(w.getMemo())))
                .toList();
        return new CsvTable(List.of("日付", "時間", "飲み物", "量(ml)", "メモ"), rows);
    }

    private CsvTable stepTable(Long profileId, LocalDate start, LocalDate end) {
        List<List<String>> rows = stepRepository
                .findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateAscIdAsc(profileId, start, end).stream()
                .map(s -> List.of(str(s.getRecordedDate()), str(s.getSteps()), text(s.getMemo()))).toList();
        return new CsvTable(List.of("日付", "歩数", "メモ"), rows);
    }

    private static String bmi(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.signum() <= 0) {
            return "";
        }
        BigDecimal meters = height.divide(BigDecimal.valueOf(100));
        return weight.divide(meters.multiply(meters), 1, RoundingMode.HALF_UP).toPlainString();
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    // CSV インジェクション対策：Excel が数式として解釈する先頭文字には ' を付ける。
    private static String text(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        boolean risky = first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r';
        return risky ? "'" + value : value;
    }

    private static void appendLine(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }

    private static String escape(String value) {
        boolean needsQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        return needsQuote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}