package com.healthlog.demo.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sleep_logs", indexes = {
		@Index(name = "idx_sleep_profile_date", columnList = "profile_id, recorded_date"),
})
public class Sleep extends BaseLog {

    public enum SleepType {
        NIGHT, // 夜間睡眠
        NAP    // 昼寝
    }

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_type", nullable = false, length = 20)
    private SleepType sleepType = SleepType.NIGHT;

    @Column(name = "sleep_minutes")
    private Integer sleepMinutes;

    @Column(length = 500)
    private String memo;

    public Sleep(Profile profile, LocalDate recordedDate) {
        this.setProfile(profile);
        this.setRecordedDate(recordedDate);
    }
}