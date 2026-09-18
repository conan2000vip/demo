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
@Table(name = "step_logs", indexes = {
		@Index(name = "uk_step_profile_date", columnList = "profile_id, recorded_date", unique = true)
})
public class Step extends BaseLog {

    @Column(nullable = false)
    private int steps;

    @Column(length = 500)
    private String memo;

    public Step(Profile profile, LocalDate recordedDate, int steps) {
        this.setProfile(profile);
        this.setRecordedDate(recordedDate);
        this.steps = steps;
    }
}