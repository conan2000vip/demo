package com.healthlog.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.healthlog.demo.entity.Sleep.SleepType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SleepSummary {
    private Long id;
    private LocalDate recordedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SleepType sleepType;
    private Integer sleepMinutes;
    private String memo;
}
