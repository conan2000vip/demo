package com.healthlog.demo.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemoSummary {
    private Long id;
    private LocalDate recordedDate;
    private String title;
    private String content;
}