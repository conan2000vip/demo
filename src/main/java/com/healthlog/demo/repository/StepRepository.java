package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.healthlog.demo.entity.Step;

public interface StepRepository extends BaseLogRepository<Step> {

    // 指定日の歩数レコードを取得（検索および上書き・更新用）
    Optional<Step> findFirstByProfile_IdAndRecordedDate(Long profileId, LocalDate recordedDate);

    // 全期間で最新のレコードを取得（同日ならIDで順位付け）
    Optional<Step> findTopByProfile_IdOrderByRecordedDateDescIdDesc(Long profileId);

}
