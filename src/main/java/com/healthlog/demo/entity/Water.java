package com.healthlog.demo.entity;

import java.time.LocalDate;
import java.time.LocalTime;

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
@Table(name = "water_logs", indexes = {
    @Index(name = "idx_water_profile_date", columnList = "profile_id, recorded_date")
})
public class Water extends BaseLog {

    public enum DrinkType {
        WATER("水"), TEA("お茶"), MILK("牛乳"), COFFEE("コーヒー"), JUICE("ジュース"), SPORTS_DRINK("スポーツドリンク"), OTHER("その他");

        private final String label;

        DrinkType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
    @Column(name = "recorded_time")
    private LocalTime recordedTime;

    @Column(name = "drink_type", length = 30)
    private String drinkType;

    @Column(name = "amount_ml", nullable = false)
    private int amountMl;

    @Column(length = 500)
    private String memo;

    public Water(Profile profile, LocalDate recordedDate, int amountMl) {
        this.setProfile(profile);
        this.setRecordedDate(recordedDate);
        this.amountMl = amountMl;
    }
}
