package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class JobSeekerGroup {
    private String workerName;
    private String birthDate;
    private String address;
    private String desiredJob;
    private List<JobSeekerReceipt> items;
}
