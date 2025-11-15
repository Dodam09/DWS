package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyWorkRow {

    private LocalDate workDate;   // 날짜
    private String workerName;    // 근무자
    private String companyName;   // 업체명
    private String grade;         // 전공 / 조공
    private int unitPrice;        // 단가
}
