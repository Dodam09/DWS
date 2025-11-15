package com.example.demo.dto;

import java.util.List;

public class JobOfferGroup {

    // 상단 사업체 정보
    private String companyName;  // 사업체명
    private String address;      // 소재지
    private String phone;        // 전화번호
    private String jobType;      // 직종 (전기)

    // 이 업체에서 일한 날짜/사람 목록
    private List<JobOfferReceipt> items;

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getJobType() {
        return jobType;
    }
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    public List<JobOfferReceipt> getItems() {
        return items;
    }
    public void setItems(List<JobOfferReceipt> items) {
        this.items = items;
    }
}
