package com.example.demo.controller;

import com.example.demo.dto.JobOfferGroup;
import com.example.demo.dto.JobOfferReceipt;
import com.example.demo.dto.JobSeekerGroup;
import com.example.demo.dto.JobSeekerReceipt;
import com.example.demo.dto.ProcessResult;
import com.example.demo.service.DailyWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;          // ✅ 추가
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UploadController {

    private final DailyWorkService dailyWorkService;

    @GetMapping("/")
    public String showUploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) throws Exception {

        ProcessResult result = dailyWorkService.processZip(file);

        List<JobSeekerReceipt> jobSeekerList = result.getJobSeekerList();
        List<JobOfferReceipt> jobOfferList = result.getJobOfferList();

        // ✅ 0) 정렬: 날짜 기준 + 보조키
        jobSeekerList.sort(
                Comparator.comparing(JobSeekerReceipt::getDate)
                          .thenComparing(JobSeekerReceipt::getName)
        );

        jobOfferList.sort(
                Comparator.comparing(JobOfferReceipt::getDate)
                          .thenComparing(JobOfferReceipt::getCompanyName)
        );

        // === 1) 구직: 사람별 그룹 ===
        Map<String, List<JobSeekerReceipt>> seekerGrouped =
                jobSeekerList.stream()
                        .collect(Collectors.groupingBy(
                                JobSeekerReceipt::getName,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<JobSeekerGroup> jobSeekerGroups = new ArrayList<>();
        for (Map.Entry<String, List<JobSeekerReceipt>> entry : seekerGrouped.entrySet()) {
            List<JobSeekerReceipt> items = entry.getValue();
            if (items.isEmpty()) continue;

            JobSeekerReceipt first = items.get(0);

            JobSeekerGroup group = new JobSeekerGroup();
            group.setWorkerName(first.getName());
            group.setBirthDate(first.getBirth());
            group.setAddress(first.getAddress());
            group.setDesiredJob(first.getDesiredJob());
            group.setItems(items);   // 이미 날짜순으로 정렬된 리스트

            jobSeekerGroups.add(group);
        }

        // === 2) 구인: 업체별 그룹 ===
        Map<String, List<JobOfferReceipt>> offerGrouped =
                jobOfferList.stream()
                        .collect(Collectors.groupingBy(
                                JobOfferReceipt::getCompanyName,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<JobOfferGroup> jobOfferGroups = new ArrayList<>();
        for (Map.Entry<String, List<JobOfferReceipt>> entry : offerGrouped.entrySet()) {
            List<JobOfferReceipt> items = entry.getValue();
            if (items.isEmpty()) continue;

            JobOfferReceipt first = items.get(0);

            JobOfferGroup group = new JobOfferGroup();
            group.setCompanyName(first.getCompanyName());
            group.setAddress(first.getAddress());
            group.setPhone(first.getPhone());
            group.setJobType(first.getJobType());
            group.setItems(items);   // 이미 날짜순

            jobOfferGroups.add(group);
        }

        // === 3) 모델에 세팅 ===
        model.addAttribute("jobSeekerGroups", jobSeekerGroups);
        model.addAttribute("jobOfferGroups", jobOfferGroups);

        model.addAttribute("jobSeekerCount", jobSeekerGroups.size());
        model.addAttribute("jobOfferCount", jobOfferGroups.size());
        model.addAttribute("workerCount", result.getWorkerSet().size());
        model.addAttribute("companyCount", result.getCompanySet().size());

        return "print-view";
    }
}
