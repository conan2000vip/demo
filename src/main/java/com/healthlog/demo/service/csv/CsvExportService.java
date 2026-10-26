package com.healthlog.demo.service.csv;

import java.time.LocalDate;
import java.util.List;

public interface CsvExportService {
    // CSV 1ファイル分のデータ（列見出しと行）
    record CsvTable(List<String> columns, List<List<String>> rows) {
    }

    // 閲覧権限と期間を検証し、指定したデータ項目（weight / sleep / water / steps）の表を作る
    CsvTable build(Long profileId, Long userId, String type, LocalDate start, LocalDate end);

    // 表を UTF-8（BOM付き）の CSV バイト列に変換する
    byte[] toCsvBytes(CsvTable table);
}
