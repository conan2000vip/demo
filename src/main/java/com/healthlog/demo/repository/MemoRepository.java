package com.healthlog.demo.repository;

import java.util.List;

import com.healthlog.demo.entity.Memo;

public interface MemoRepository extends BaseLogRepository<Memo> {
    // メモのタイトルまたは本文に含まれるキーワードで検索する。
    List<Memo> findByProfile_IdAndTitleContainingIgnoreCase(Long profileId, String keyword);

}