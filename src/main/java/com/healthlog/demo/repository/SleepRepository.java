package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.entity.Sleep.SleepType;

public interface SleepRepository extends BaseLogRepository<Sleep> {

        // 1. 1日の睡眠一覧を取得（昼寝と夜間睡眠の詳細表示用）
    List<Sleep> findByProfile_IdAndRecordedDateOrderByStartTimeAsc(Long profileId, LocalDate recordedDate);

        // 2. 日付と種類を指定して睡眠レコードを1件取得（編集・更新用）
    Optional<Sleep> findFirstByProfile_IdAndRecordedDateAndSleepType(
            Long profileId, LocalDate recordedDate, SleepType sleepType);

        // 3. 1日の合計睡眠時間を分単位で計算（昼寝と夜間睡眠を合算）
//     @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM Sleep s WHERE s.profile.id = :profileId AND s.recordedDate = :recordedDate")
//     Long sumDurationMinutesByProfileIdAndRecordedDate(
//             @Param("profileId") Long profileId,
//             @Param("recordedDate") LocalDate recordedDate);
}
