package com.example.demo.service;

import com.example.demo.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;


@Service
public class DailyWorkService {

    private static final Random RANDOM = new Random();

    public ProcessResult processZip(MultipartFile zipFile) throws Exception {

        List<JobSeekerReceipt> jobSeekerList = new ArrayList<>();
        List<JobOfferReceipt> jobOfferList = new ArrayList<>();
        Set<String> workerSet = new HashSet<>();
        Set<String> companySet = new HashSet<>();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {

                String entryName = entry.getName();
                System.out.println("처리 중인 엔트리: " + entryName);

                // 1) 디렉토리는 건너뜀
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                // 2) 엑셀(xlsx) 아니면 건너뜀
                if (!entryName.toLowerCase().endsWith(".xlsx")) {
                    zis.closeEntry();
                    continue;
                }

                // 3) 엑셀 임시파일(~$로 시작)은 건너뜀
                if (entryName.startsWith("~$")) {
                    zis.closeEntry();
                    continue;
                }

                // 4) 진짜 엑셀 파일만 메모리로 복사
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                byte[] excelBytes = baos.toByteArray();

                // 혹시 0바이트 / 너무 작은 파일이면 스킵
                if (excelBytes.length < 10) {
                    System.out.println("파일 용량이 너무 작아서 스킵: " + entryName);
                    zis.closeEntry();
                    continue;
                }

                // 5) 이 byte[]를 InputStream으로 만들어서 엑셀 파싱
                try (ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes)) {
                    try {
                        processOneExcel(bais, jobSeekerList, jobOfferList, workerSet, companySet);
                    } catch (IOException e) {
                        // 이 파일만 스킵하고 다음 엔트리 계속
                        System.out.println("엑셀로 열 수 없어서 스킵: " + entryName + " / " + e.getMessage());
                    }
                }

                zis.closeEntry();
            }
        }

        ProcessResult result = new ProcessResult();
        result.setJobSeekerList(jobSeekerList);
        result.setJobOfferList(jobOfferList);
        result.setWorkerSet(workerSet);
        result.setCompanySet(companySet);
        return result;
    }



    private void processOneExcel(InputStream is,
                                 List<JobSeekerReceipt> jobSeekerList,
                                 List<JobOfferReceipt> jobOfferList,
                                 Set<String> workerSet,
                                 Set<String> companySet) throws Exception {

        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Sheet1 가정

            // 헤더가 있는 행 인덱스 (0부터 시작, 우리 파일은 9번째 줄)
            int headerRowIndex = 9;
            int firstDataRowIndex = headerRowIndex + 1;

            for (int rowIndex = firstDataRowIndex; ; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) break;

                Cell dateCell = row.getCell(0); // A열: 날짜
                if (dateCell == null || dateCell.getCellType() == CellType.BLANK) {
                    break; // 데이터 끝
                }

                LocalDate workDate = getLocalDate(dateCell);
                if (workDate == null) {
                    // 헤더 행("날짜")이거나 날짜가 아닌 값이면 스킵
                    continue;
                }
                String workerName = getString(row.getCell(2));   // C열: 근무자
                String companyName = getString(row.getCell(3));  // D열: 업체명
                String grade = getString(row.getCell(5));        // F열: 전공/조공
                int unitPrice = getInt(row.getCell(6));          // G열: 단가

                if (workerName == null || workerName.isBlank()) continue;
                if (companyName == null || companyName.isBlank()) continue;

                // 구직접수
                JobSeekerReceipt seeker = new JobSeekerReceipt();
                seeker.setName(workerName);
                seeker.setBirth(randomBirth());
                seeker.setAddress(randomIncheonAddress());
                seeker.setDesiredJob("전기");
                seeker.setDate(workDate);
                seeker.setCompanyName(companyName);
                seeker.setIncome(unitPrice);
                seeker.setFee((int) Math.round(unitPrice * 0.1));

                // 구인접수
                JobOfferReceipt offer = new JobOfferReceipt();
                offer.setCompanyName(companyName);
                offer.setJobType("전기");
                offer.setAddress(randomIncheonAddress());
                offer.setPhone(randomPhoneNumber());
                offer.setDate(workDate);
                offer.setGender("남자");
                offer.setWage(unitPrice);
                offer.setFee((int) Math.round(unitPrice * 0.1));
                offer.setWorkerName(workerName);
                offer.setGrade(grade);

                jobSeekerList.add(seeker);
                jobOfferList.add(offer);
                workerSet.add(workerName);
                companySet.add(companyName);
            }
        }
    }

    // ====== 유틸 메서드들 ======

    private LocalDate getLocalDate(Cell cell) {
    if (cell == null) return null;

    // 1) 엑셀 '날짜' 형식(숫자 + 날짜포맷)인 경우
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
        return cell.getDateCellValue().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    // 2) 문자열인 경우 (yyyy-MM-dd 같이 텍스트로 들어있을 때)
    String s = getString(cell);
    if (s == null || s.isBlank()) {
        return null;
    }

    // "날      짜" 같은 헤더가 들어오면 여기서 걸러야 함
    try {
        return LocalDate.parse(s.trim());   // yyyy-MM-dd 형식일 때만 성공
    } catch (Exception e) {
        // 날짜로 못 읽으면 그냥 null 반환 (헤더거나 이상한 값)
        return null;
    }
}


    private String getString(Cell cell) {
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private int getInt(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(cell.getNumericCellValue());
        }
        String s = getString(cell);
        if (s == null || s.isBlank()) return 0;
        return Integer.parseInt(s.replaceAll("[^0-9]", ""));
    }

    // 1960~1975 랜덤 생년월일 (문자열)
    private String randomBirth() {
        int year = 1960 + RANDOM.nextInt(1976 - 1960); // 1960~1975
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28); // 단순화
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    // 인천 랜덤 주소 (간단 버전)
    private static final String[] ADDRESSES = {
            "인천광역시 미추홀구 주안동",
            "인천광역시 남동구 구월동",
            "인천광역시 부평구 부평동",
            "인천광역시 서구 청라동",
            "인천광역시 연수구 송도동"
    };

    private String randomIncheonAddress() {
        return ADDRESSES[RANDOM.nextInt(ADDRESSES.length)];
    }

    // 랜덤 전화번호
    private String randomPhoneNumber() {
        int mid = 1000 + RANDOM.nextInt(9000);
        int last = 1000 + RANDOM.nextInt(9000);
        return String.format("010-%04d-%04d", mid, last);
    }
}
