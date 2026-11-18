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

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/test-seeder")
@RequiredArgsConstructor
public class TestSeederController {

    private final ProfileRepository profileRepository;
    private final WeightRepository weightRepository;
    private final SleepRepository sleepRepository;
    private final WaterRepository waterRepository;
    private final StepRepository stepRepository;
    private final EntityManager em;

    // API: /api/test-seeder/streak?profileId=53&scenario=7_DAYS
    // 自動生成された連続データを削除してから、指定された日数の連続データを作成する。RESET_MISSING_* シナリオでは、4日目の記録を欠落させる。
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
        scenario = scenario.trim().toUpperCase();
        int days = switch (scenario) {
        case "0_DAY" -> 0;
        case "1_DAY" -> 1;
        case "2_DAYS" -> 2;
        case "3_DAYS" -> 3;
        case "4_DAYS" -> 4;
        case "5_DAYS" -> 5;
        case "6_DAYS" -> 6;
        case "7_DAYS" -> 7;
        case "8_DAYS" -> 8;
        case "9_DAYS" -> 9;
        case "10_DAYS" -> 10;
        case "12_DAYS" -> 12;
        case "13_DAYS" -> 13;
        case "14_DAYS" -> 14;
        case "15_DAYS" -> 15;
        case "20_DAYS" -> 20;
        case "21_DAYS" -> 21;
        case "30_DAYS" -> 30;
        case "60_DAYS" -> 60;
        case "90_DAYS" -> 90;
        case "100_DAYS" -> 100;
        case "180_DAYS" -> 180;
        case "366_DAYS" -> 366;
        case "367_DAYS" -> 367;
        case "730_DAYS" -> 730;
        case "731_DAYS" -> 731;
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

            // 条件に応じてWeight Logを作成する。RESETのテスト用に4日目の記録を欠落させる。
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

            // 条件に応じてSleep Logを作成する。RESETのテスト用に4日目の記録を欠落させる。
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

            // 条件に応じてWater Logを作成する。RESETのテスト用に4日目の記録を欠落させる。
            if (!("RESET_MISSING_WATER".equals(scenario) && isResetDay)) {
                createWaterLog(profile, currentDate, LocalTime.of(8, 0), 500);
                createWaterLog(profile, currentDate, LocalTime.of(12, 0), 500);
                createWaterLog(profile, currentDate, LocalTime.of(18, 0), 500);
            }

            // ステップログは、RESETのテスト用に4日目の記録を欠落させる。
            if (!("RESET_MISSING_STEP".equals(scenario) && isResetDay)) {
                Step step = new Step();
                step.setProfile(profile);
                step.setRecordedDate(currentDate);
                step.setSteps(8000);
                step.setMemo("STREAK TEST SEEDER");
                stepRepository.save(step);
            }

            // へルパーメソッドで、created_atを指定した日付に更新する。
            backdate("weight_logs", profileId, currentDate);
            backdate("sleep_logs", profileId, currentDate);
            backdate("water_logs", profileId, currentDate);
            backdate("step_logs", profileId, currentDate);
        }

        // レスポンスを返す。
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

    // API: /api/test-seeder/random?profileId=30&days=60&seed=123&missingPercent=15
    // ランダムな日次データを作成する。seed指定で再現可能。省略すると毎回異なるデータになる。
    @GetMapping("/random")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateRandomData(@RequestParam Long profileId,
            @RequestParam(defaultValue = "30") int days, @RequestParam(required = false) Long seed,
            @RequestParam(defaultValue = "15") int missingPercent) {

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile ID not found: " + profileId));

        weightRepository.deleteByProfile_Id(profileId);
        weightRepository.flush();
        sleepRepository.deleteByProfile_Id(profileId);
        sleepRepository.flush();
        waterRepository.deleteByProfile_Id(profileId);
        waterRepository.flush();
        stepRepository.deleteByProfile_Id(profileId);
        stepRepository.flush();

        long usedSeed = seed != null ? seed : System.nanoTime();
        Random r = new Random(usedSeed);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        double weightBase = 55 + r.nextDouble() * 20; // プロファイルごとに基準体重を変える
        int created = 0;

        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);

            // 項目ごとに独立して欠損させる（グラフに途切れを作る）
            if (r.nextInt(100) >= missingPercent) {
                Weight w = new Weight();
                w.setProfile(profile);
                w.setRecordedDate(d);
                w.setWeight(BigDecimal.valueOf(weightBase + r.nextGaussian() * 1.2).setScale(1, RoundingMode.HALF_UP));
                w.setMeasuredAt(LocalDateTime.of(d, LocalTime.of(8, 0)));
                w.setHeight(new BigDecimal("170.0"));
                w.setMemo("RANDOM TEST SEEDER");
                weightRepository.save(w);
                created++;
            }
            if (r.nextInt(100) >= missingPercent) {
                int minutes = 300 + r.nextInt(240); // 5.0〜9.0時間
                Sleep s = new Sleep();
                s.setProfile(profile);
                s.setRecordedDate(d);
                s.setStartTime(LocalTime.of(23, 0));
                s.setEndTime(LocalTime.of(23, 0).plusMinutes(minutes));
                s.setSleepType(Sleep.SleepType.NIGHT);
                s.setSleepMinutes(minutes);
                s.setMemo("RANDOM TEST SEEDER");
                sleepRepository.save(s);
                created++;
            }
            if (r.nextInt(100) >= missingPercent) {
                int times = 2 + r.nextInt(4); // 2〜5回
                for (int k = 0; k < times; k++) {
                    createWaterLog(profile, d, LocalTime.of(7 + k * 3, 0), 200 + r.nextInt(400));
                }
                created++;
            }
            if (r.nextInt(100) >= missingPercent) {
                Step st = new Step();
                st.setProfile(profile);
                st.setRecordedDate(d);
                st.setSteps(1500 + r.nextInt(13000)); // 1500〜14500
                st.setMemo("RANDOM TEST SEEDER");
                stepRepository.save(st);
                created++;
            }

            backdate("weight_logs", profileId, d);
            backdate("sleep_logs", profileId, d);
            backdate("water_logs", profileId, d);
            backdate("step_logs", profileId, d);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("status", "SUCCESS");
        res.put("profileId", profileId);
        res.put("seed", usedSeed); // 同じデータを再生成したい時はこの値を指定する
        res.put("startDate", startDate.toString());
        res.put("endDate", endDate.toString());
        res.put("days", days);
        res.put("recordsCreatedItems", created);
        return ResponseEntity.ok(res);
    }

    // へルパーメソッド: Water Logを作成する
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

    // 処理の最後に、created_atを指定した日付に更新するためのヘルパーメソッド
    private void backdate(String table, Long profileId, LocalDate date) {
        em.flush();
        em.createNativeQuery("UPDATE " + table + " SET created_at = :ts WHERE profile_id = :pid AND recorded_date = :d")
                .setParameter("ts", date.atTime(12, 0)).setParameter("pid", profileId).setParameter("d", date)
                .executeUpdate();
    }

}