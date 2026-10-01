package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.util.List;

import com.healthlog.demo.entity.Memo;

public interface MemoRepository extends BaseLogRepository<Memo> {
    List<Memo> findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDescIdDesc(
            Long profileId, LocalDate from, LocalDate to);

    List<Memo> findByProfile_IdAndRecordedDateOrderByIdDesc(Long profileId, LocalDate recordedDate);

    List<Memo> findByProfile_IdAndTitleContainingIgnoreCase(Long profileId, String keyword);

}