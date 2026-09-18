package com.healthlog.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
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
@Table(name = "weight_logs", indexes = {
		@Index(name = "idx_weight_profile_date", columnList = "profile_id, recorded_date")
})
@AttributeOverride(name = "measuredAt", column = @Column(name = "measured_at", nullable = false))
public class Weight extends BaseLog {

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal weight;

    @Column(precision = 5, scale = 1)
    private BigDecimal height;

    @Column(length = 500)
    private String memo;

    public Weight(Profile profile, LocalDate recordedDate, BigDecimal weight, LocalDateTime measuredAt) {
        this.setProfile(profile);
        this.setRecordedDate(recordedDate);
        this.weight = weight;
        this.setMeasuredAt(measuredAt);
    }
}