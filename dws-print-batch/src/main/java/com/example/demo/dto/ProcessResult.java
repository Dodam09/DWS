package com.example.demo.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProcessResult {

    private List<JobSeekerReceipt> jobSeekerList;
    private List<JobOfferReceipt> jobOfferList;
    private Set<String> workerSet;
    private Set<String> companySet;
}
