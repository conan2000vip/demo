package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.healthlog.demo.entity.Water;

public interface WaterRepository extends BaseLogRepository<Water> {

    // 1日の給水記録一覧を取得（各回の詳細表示用）
    List<Water> findByProfile_IdAndRecordedDateOrderByIdAsc(Long profileId, LocalDate recordedDate);

    // 指定日の摂取水分量を合計（合算）
    @Query("SELECT COALESCE(SUM(w.amountMl), 0) FROM Water w WHERE w.profile.id = :profileId AND w.recordedDate = :recordedDate")
    Long sumAmountMlByProfileIdAndRecordedDate(@Param("profileId") Long profileId, @Param("recordedDate") LocalDate recordedDate);

    // 指定期間の摂取水分量を合計（週次・月次レポート用）
    @Query("SELECT COALESCE(SUM(w.amountMl), 0) FROM Water w WHERE w.profile.id = :profileId AND w.recordedDate BETWEEN :from AND :to")
    Long sumAmountMlByProfileIdAndDateBetween(@Param("profileId") Long profileId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}