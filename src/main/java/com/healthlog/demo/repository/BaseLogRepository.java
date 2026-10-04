package com.healthlog.demo.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.healthlog.demo.entity.BaseLog;

@NoRepositoryBean
public interface BaseLogRepository<T extends BaseLog> extends JpaRepository<T, Long> {

        // === 1. リスト検索 - 旧4条件 ===
        List<T> findByProfile_IdOrderByRecordedDateDesc(Long profileId);

        List<T> findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDesc(
                        Long profileId, LocalDate from, LocalDate to);

        List<T> findByProfile_IdAndRecordedDateGreaterThanEqualOrderByRecordedDateDesc(
                        Long profileId, LocalDate from);

        List<T> findByProfile_IdAndRecordedDateLessThanEqualOrderByRecordedDateDesc(
                        Long profileId, LocalDate to);

        // === 2. ページ検索 - 旧4条件 ===
        Page<T> findByProfile_IdOrderByRecordedDateDesc(Long profileId, Pageable pageable);

        Page<T> findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDesc(
                        Long profileId, LocalDate from, LocalDate to, Pageable pageable);

        Page<T> findByProfile_IdAndRecordedDateGreaterThanEqualOrderByRecordedDateDesc(
                        Long profileId, LocalDate from, Pageable pageable);

        Page<T> findByProfile_IdAndRecordedDateLessThanEqualOrderByRecordedDateDesc(
                        Long profileId, LocalDate to, Pageable pageable);

        // === 3. データの存在確認と件数取得 ===
        boolean existsByProfile_Id(Long profileId);

        boolean existsByProfile_IdAndRecordedDate(Long profileId, LocalDate recordedDate);

        boolean existsByProfile_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        Long profileId, LocalDateTime from, LocalDateTime to);

        long countByProfile_IdAndRecordedDate(Long profileId, LocalDate recordedDate);

        // === 4. 最新または直近のレコードを取得 ===
        Optional<T> findTopByProfile_IdOrderByRecordedDateDesc(Long profileId);

        Optional<T> findTopByProfile_IdAndRecordedDateLessThanOrderByRecordedDateDesc(Long profileId, LocalDate date);

        // === 5. レコードを削除 ===
        void deleteByProfile_Id(Long profileId);

        void deleteByIdAndProfile_Id(Long id, Long profileId);
}