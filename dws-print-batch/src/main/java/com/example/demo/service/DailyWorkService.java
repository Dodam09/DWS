package com.example.demo.service;

import com.example.demo.dto.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

                // 구직접수 (사람: 전국 주소)
                JobSeekerReceipt seeker = new JobSeekerReceipt();
                seeker.setName(workerName);
                seeker.setBirth(randomBirth());
                seeker.setAddress(randomNationwideAddress());
                seeker.setDesiredJob("전기");
                seeker.setDate(workDate);
                seeker.setCompanyName(companyName);
                seeker.setIncome(unitPrice);
                seeker.setFee((int) Math.round(unitPrice * 0.1));

                // 구인접수 (사업체: 인천 주소)
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

    // ====== 랜덤 주소 관련 ======

    // 전국(사람용) 상세 주소
    private static final String[] NATION_ADDRESSES = {
            // 서울
            "서울특별시 강남구 테헤란로 212",
            "서울특별시 강서구 공항대로 248",
            "서울특별시 영등포구 여의나루로 53",
            "서울특별시 마포구 월드컵북로 45",
            "서울특별시 송파구 올림픽로 300",
            "서울특별시 종로구 종로 1",
            "서울특별시 동작구 상도로 102",

            // 경기
            "경기도 수원시 영통구 봉영로 1545",
            "경기도 용인시 기흥구 중부대로 736",
            "경기도 성남시 분당구 성남대로 295",
            "경기도 부천시 길주로 210",
            "경기도 고양시 일산동구 정발산로 24",
            "경기도 안양시 동안구 시민대로 203",
            "경기도 광명시 철산로 29번길 11",
            "경기도 평택시 평남로 615",
            "경기도 시흥시 배곧로 278",

            // 인천 (사람용에도 일부 섞음)
            "인천광역시 남동구 예술로 150번길 12",
            "인천광역시 연수구 송도과학로 85",
            "인천광역시 서구 청라에메랄드로 77",
            "인천광역시 부평구 부평대로 168",
            "인천광역시 미추홀구 매소홀로 538",

            // 부산
            "부산광역시 해운대구 해운대로 813",
            "부산광역시 수영구 광남로 70",
            "부산광역시 부산진구 서면로 66",
            "부산광역시 남구 유엔평화로 24",

            // 대구
            "대구광역시 중구 동성로 25",
            "대구광역시 수성구 달구벌대로 2346",
            "대구광역시 북구 구암로 100",

            // 광주
            "광주광역시 동구 문화전당로 38",
            "광주광역시 서구 상무중앙로 75",

            // 대전
            "대전광역시 서구 둔산중로 50",
            "대전광역시 유성구 대학로 99",

            // 울산
            "울산광역시 남구 삼산중로 100",
            "울산광역시 북구 산업로 1100",

            // 강원
            "강원특별자치도 춘천시 중앙로 15",
            "강원특별자치도 원주시 서원대로 250",

            // 충청
            "충청북도 청주시 상당구 상당로 82",
            "충청남도 천안시 동남구 만남로 43",

            // 전라
            "전라북도 전주시 완산구 팔달로 254",
            "전라남도 순천시 장명로 16",

            // 경상
            "경상북도 포항시 북구 중앙로 248",
            "경상남도 창원시 성산구 원이대로 595",

            // 제주
            "제주특별자치도 제주시 연북로 89",
            "제주특별자치도 서귀포시 중정로 55"
    };

    // 인천(사업체용) 상세 주소
    private static final String[] INCHEON_ADDRESSES = {
            "인천광역시 미추홀구 주안로 160번길 21",
            "인천광역시 미추홀구 소성로 100",
            "인천광역시 남동구 구월로 45번길 27",
            "인천광역시 남동구 논현로 98번길 32",
            "인천광역시 남동구 소래역로 25",
            "인천광역시 연수구 송도문화로 35번길 11",
            "인천광역시 연수구 연수대로 210",
            "인천광역시 연수구 청량로 27",
            "인천광역시 서구 청라커낼로 240",
            "인천광역시 서구 가정로 120번길 15",
            "인천광역시 서구 검암동 검암로 35",
            "인천광역시 부평구 부평로 56",
            "인천광역시 부평구 심정로 22",
            "인천광역시 부평구 산곡로 93",
            "인천광역시 계양구 계양대로 95",
            "인천광역시 동구 샛골로 102",
            "인천광역시 강화군 강화대로 420",
            "인천광역시 옹진군 영흥로 321"
    };

    // 사람용 전국 랜덤 주소
    private String randomNationwideAddress() {
        return NATION_ADDRESSES[RANDOM.nextInt(NATION_ADDRESSES.length)];
    }

    // 사업체용 인천 랜덤 주소
    private String randomIncheonAddress() {
        return INCHEON_ADDRESSES[RANDOM.nextInt(INCHEON_ADDRESSES.length)];
    }

    // 랜덤 전화번호
    private String randomPhoneNumber() {
        int mid = 1000 + RANDOM.nextInt(9000);
        int last = 1000 + RANDOM.nextInt(9000);
        return String.format("010-%04d-%04d", mid, last);
    }
}
