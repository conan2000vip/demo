package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.healthlog.demo.entity.Weight;

public interface WeightRepository extends BaseLogRepository<Weight> {

    // 指定日の最新体重記録を取得
    Optional<Weight> findFirstByProfile_IdAndRecordedDateOrderByIdDesc(Long profileId, LocalDate recordedDate);

    // 1日の体重記録をすべて取得（朝・夜の詳細表示用）
    List<Weight> findByProfile_IdAndRecordedDateOrderByIdDesc(Long profileId, LocalDate recordedDate);

    @Query(value = """
            SELECT MAX(w.created_at)
            FROM weight_logs w
            WHERE w.profile_id = :profileId
              AND w.created_at < :beforeDate
              AND EXISTS (
                  SELECT 1 FROM sleep_logs s
                  WHERE s.profile_id = :profileId
                    AND DATE(s.created_at) = DATE(w.created_at)
              )
              AND EXISTS (
                  SELECT 1 FROM water_logs wa
                  WHERE wa.profile_id = :profileId
                    AND DATE(wa.created_at) = DATE(w.created_at)
              )
              AND EXISTS (
                  SELECT 1 FROM step_logs st
                  WHERE st.profile_id = :profileId
                    AND DATE(st.created_at) = DATE(w.created_at)
              )
            GROUP BY DATE(w.created_at)
            ORDER BY DATE(w.created_at) DESC
            """, nativeQuery = true)
    List<LocalDateTime> findCompleteStreakDays(@Param("profileId") Long profileId,
            @Param("beforeDate") LocalDateTime beforeDate);
}