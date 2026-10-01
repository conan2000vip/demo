package com.healthlog.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.healthlog.demo.entity.Water;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WaterSummary {
    private Long id;
    private LocalDate recordedDate;
    private LocalTime recordedTime;
    private String drinkType;
    private String drinkTypeLabel;
    private int amountMl;
    private String memo;

    public static WaterSummary from(Water water) {
        String label = water.getDrinkType();
        if (label != null) {
            try {
                label = Water.DrinkType.valueOf(label).getLabel();
            } catch (IllegalArgumentException ignored) {
                label = Water.DrinkType.OTHER.getLabel();
            }
        }
        return new WaterSummary(water.getId(), water.getRecordedDate(), water.getRecordedTime(),
                water.getDrinkType(), label, water.getAmountMl(), water.getMemo());
    }
}
