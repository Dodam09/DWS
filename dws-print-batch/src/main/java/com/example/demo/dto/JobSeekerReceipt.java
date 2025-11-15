package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JobSeekerReceipt {

    private String name;          // 성명
    private String birth;         // 생년월일 (예: 1970-03-15)
    private String address;       // 주소
    private String desiredJob;    // 희망직종 (전기)
    private LocalDate date;       // 일자
    private String companyName;   // 사업체명
    private int income;           // 입금 (단가)
    private int fee;              // 소개요금 (단가의 10%)
}
