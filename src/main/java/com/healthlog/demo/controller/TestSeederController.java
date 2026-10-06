package com.healthlog.demo.controller;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.entity.Water;
import com.healthlog.demo.entity.Weight;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-seeder")
@RequiredArgsConstructor
public class TestSeederController {

    private final ProfileRepository profileRepository;
    private final WeightRepository weightRepository;
    private final SleepRepository sleepRepository;
    private final WaterRepository waterRepository;
    private final StepRepository stepRepository;

    /**
     * API tạo dữ liệu Streak tự động Ví dụ: GET
     * /api/test-seeder/streak?profileId=53&scenario=7_DAYS
     */
    @GetMapping("/streak")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateStreakData(@RequestParam Long profileId,
            @RequestParam(defaultValue = "7_DAYS") String scenario) {

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile ID not found: " + profileId));

        // プロファイルの既存ログを削除する。
        System.out.println("=== STREAK TEST SEEDER: DELETE OLD DATA ===");
        weightRepository.deleteByProfile_Id(profileId);
        weightRepository.flush();

        sleepRepository.deleteByProfile_Id(profileId);
        sleepRepository.flush();

        waterRepository.deleteByProfile_Id(profileId);
        waterRepository.flush();

        stepRepository.deleteByProfile_Id(profileId);
        stepRepository.flush();
        System.out.println("=== STREAK TEST SEEDER: OLD DATA DELETED ===");

        // シナリオに応じて日数を計算する。
        int days = switch (scenario) {
        case "1_DAY" -> 1;
        case "6_DAYS" -> 6;
        case "7_DAYS" -> 7;
        case "14_DAYS" -> 14;
        case "30_DAYS" -> 30;
        case "60_DAYS" -> 60;
        case "90_DAYS" -> 90;
        case "100_DAYS" -> 100;
        case "180_DAYS" -> 180;
        case "730_DAYS" -> 730;
        case "1095_DAYS" -> 1095;
        case "500_DAYS" -> 500;
        case "1000_DAYS" -> 1000;
        case "1001_DAYS" -> 1001;
        case "1460_DAYS" -> 1460;
        case "1825_DAYS" -> 1825;
        case "2190_DAYS" -> 2190;
        case "365_DAYS", "1_YEAR" -> 365;
        case "RESET_MISSING_WEIGHT", "RESET_MISSING_SLEEP", "RESET_MISSING_WATER", "RESET_MISSING_STEP" -> 10;
        default -> 7;
        };

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 日ごとの連続データを生成する。
        for (int i = 0; i < days; i++) {
            LocalDate currentDate = startDate.plusDays(i);

            // RESETのテスト用に4日目の記録を欠落させる。
            boolean isResetDay = (i == 3);

            // =========================================================
            // A. Weight Log
            // =========================================================
            if (!("RESET_MISSING_WEIGHT".equals(scenario) && isResetDay)) {
                Weight weight = new Weight();
                weight.setProfile(profile);
                weight.setRecordedDate(currentDate);
                weight.setWeight(new java.math.BigDecimal("60.0"));
                weight.setMeasuredAt(LocalDateTime.of(currentDate, LocalTime.of(8, 0)));
                weight.setHeight(new java.math.BigDecimal("170.0"));
                weight.setMemo("STREAK TEST SEEDER");
                weightRepository.save(weight);
            }

            // =========================================================
            // B. Sleep Log
            // =========================================================
            if (!("RESET_MISSING_SLEEP".equals(scenario) && isResetDay)) {
                Sleep sleep = new Sleep();
                sleep.setProfile(profile);
                sleep.setRecordedDate(currentDate);
                sleep.setStartTime(LocalTime.of(23, 0));
                sleep.setEndTime(LocalTime.of(7, 0));
                sleep.setSleepType(Sleep.SleepType.NIGHT);
                sleep.setSleepMinutes(480);
                sleep.setMemo("STREAK TEST SEEDER");
                sleepRepository.save(sleep);
            }

            // =========================================================
            // C. Water Log
            // =========================================================
            if (!("RESET_MISSING_WATER".equals(scenario) && isResetDay)) {
                createWaterLog(profile, currentDate, LocalTime.of(8, 0), 500);
                createWaterLog(profile, currentDate, LocalTime.of(12, 0), 500);
                createWaterLog(profile, currentDate, LocalTime.of(18, 0), 500);
            }

            // =========================================================
            // D. Step Log
            // =========================================================
            if (!("RESET_MISSING_STEP".equals(scenario) && isResetDay)) {
                Step step = new Step();
                step.setProfile(profile);
                step.setRecordedDate(currentDate);
                step.setSteps(8000);
                step.setMemo("STREAK TEST SEEDER");
                stepRepository.save(step);
            }
        }

        // =============================================================
        // Response
        // =============================================================
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Đã khởi tạo dữ liệu test thành công!");
        response.put("scenario", scenario);
        response.put("profileId", profileId);
        response.put("startDate", startDate.toString());
        response.put("endDate", endDate.toString());
        response.put("generatedDays", days);
        return ResponseEntity.ok(response);
    }

    /**
     * Water は1日に複数レコード登録可能
     */
    private void createWaterLog(Profile profile, LocalDate date, LocalTime time, int amount) {

        Water water = new Water();
        water.setProfile(profile);
        water.setRecordedDate(date);
        water.setRecordedTime(time);
        water.setDrinkType(Water.DrinkType.WATER.getLabel());
        water.setAmountMl(amount);
        water.setMemo("STREAK TEST SEEDER");
        waterRepository.save(water);
    }
}