package com.healthlog.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass // JPAに親クラスであることを示し、このクラスのテーブルを作成しない
@Getter
@Setter
public abstract class BaseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Profileテーブルとの関連（各ログは1つのProfileに属する）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    // ログの記録日（例: 2026-09-09）。主な絞り込み・検索に使用
    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    // 必要に応じた詳細な時刻（例: 2026-09-09 08:30:00）
    @Column(name = "measured_at")
    private LocalDateTime measuredAt;

    // レコードの作成日時と更新日時を自動設定
    @Column(name = "created_at", nullable = false, updatable = false) 
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 新規作成時に日時を自動設定
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.recordedDate == null) {
            this.recordedDate = LocalDate.now();
        }
    }

    // 更新時に日時を自動設定
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}