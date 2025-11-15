package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JobOfferReceipt {

    private String companyName;   // 사업체명
    private String jobType;       // 직종 (전기)
    private String address;       // 소재지
    private String phone;         // 전화번호
    private LocalDate date;       // 접수일자
    private String gender;        // 성별 (남자)
    private int wage;             // 임금 (단가)
    private int fee;              // 소개요금 (단가의 10%)
    private String workerName;    // 근무자
    private String grade;         // 전/조공
}
