package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.healthlog.demo.entity.Weight;

public interface WeightRepository extends BaseLogRepository<Weight> {

    // 指定日の最新体重記録を取得
    Optional<Weight> findFirstByProfile_IdAndRecordedDateOrderByIdDesc(Long profileId, LocalDate recordedDate);

    // 1日の体重記録をすべて取得（朝・夜の詳細表示用）
    List<Weight> findByProfile_IdAndRecordedDateOrderByIdDesc(Long profileId, LocalDate recordedDate);
}