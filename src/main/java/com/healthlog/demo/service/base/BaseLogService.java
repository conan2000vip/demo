package com.healthlog.demo.service.base;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.entity.BaseLog;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.BaseLogRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

public abstract class BaseLogService<T extends BaseLog, DTO> {

    protected final BaseLogRepository<T> repository;
    protected final ProfileAccessValidation profileAccessValidation;

    protected BaseLogService(BaseLogRepository<T> repository, ProfileAccessValidation profileAccessValidation) {
        this.repository = repository;
        this.profileAccessValidation = profileAccessValidation;
    }

    // 抽象メソッド：各サービスでエンティティからDTOへの変換を実装する。
    protected abstract DTO mapToDto(T entity);

    // === 1. プロファイルの認証・認可（IDOR対策） ===
    protected Profile validateAndGetProfile(Long profileId, Long currentUserId) {
        return profileAccessValidation.validateAndGetProfile(profileId, currentUserId);
    }

    // === 2. 安全に1件のレコード詳細を取得 ===
    @Transactional(readOnly = true)
    public T findEntityByIdAndProfile(Long id, Long profileId) {
        return repository.findById(id)
                .filter(log -> log.getProfile().getId().equals(profileId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "データが見つかりません"));
    }

    // === 3. 条件で絞り込んだ一覧を取得（4条件） ===
    @Transactional(readOnly = true)
    public List<DTO> getLogs(Long profileId, Long currentUserId, LocalDate from, LocalDate to) {
        validateAndGetProfile(profileId, currentUserId);
        
        List<T> logs;
        if (from != null && to != null) {
            logs = repository.findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDesc(profileId, from, to);
        } else if (from != null) {
            logs = repository.findByProfile_IdAndRecordedDateGreaterThanEqualOrderByRecordedDateDesc(profileId, from);
        } else if (to != null) {
            logs = repository.findByProfile_IdAndRecordedDateLessThanEqualOrderByRecordedDateDesc(profileId, to);
        } else {
            logs = repository.findByProfile_IdOrderByRecordedDateDesc(profileId);
        }
        return logs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DTO> getLogsPaged(Long profileId, Long currentUserId, LocalDate from, LocalDate to, Pageable pageable) {
        validateAndGetProfile(profileId, currentUserId);
        
        Page<T> logs;
        if (from != null && to != null) {
            logs = repository.findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDesc(profileId, from, to, pageable);
        } else if (from != null) {
            logs = repository.findByProfile_IdAndRecordedDateGreaterThanEqualOrderByRecordedDateDesc(profileId, from, pageable);
        } else if (to != null) {
            logs = repository.findByProfile_IdAndRecordedDateLessThanEqualOrderByRecordedDateDesc(profileId, to, pageable);
        } else {
            logs = repository.findByProfile_IdOrderByRecordedDateDesc(profileId, pageable);
        }
        return logs.map(this::mapToDto);
    }

    // === 4. 最新レコードを取得（ダッシュボード用） ===
    @Transactional(readOnly = true)
    public DTO getLatestLog(Long profileId, Long currentUserId) {
        validateAndGetProfile(profileId, currentUserId);
        return repository.findTopByProfile_IdOrderByRecordedDateDesc(profileId)
                .map(this::mapToDto)
                .orElse(null);
    }

    // === 5. レコードを安全に削除 ===
    @Transactional
    public void deleteLog(Long id, Long profileId, Long currentUserId) {
        validateAndGetProfile(profileId, currentUserId);
        findEntityByIdAndProfile(id, profileId); // レコードの存在とプロファイルの一致を確認する。
        repository.deleteByIdAndProfile_Id(id, profileId);
    }
}