package com.healthlog.demo.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "memo_logs", indexes = {
		@Index(name = "idx_memo_profile_date", columnList = "profile_id,recorded_date"), })

public class Memo extends BaseLog {

    @Column(length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    public Memo(Profile profile, LocalDate recordedDate, String content) {
        this.setProfile(profile);
        this.setRecordedDate(recordedDate);
        this.content = content;
    }
}