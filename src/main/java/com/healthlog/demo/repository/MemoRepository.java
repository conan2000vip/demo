package com.healthlog.demo.repository;

import java.util.List;

import com.healthlog.demo.entity.Memo;

public interface MemoRepository extends BaseLogRepository<Memo> {
    // title/content内のキーワードでメモを検索（Memo固有の処理）
    List<Memo> findByProfile_IdAndTitleContainingIgnoreCase(Long profileId, String keyword);

}