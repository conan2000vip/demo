package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.healthlog.demo.entity.BaseLog;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeFamilyServiceImpl implements HomeFamilyService {

    private static final int NO_RECORD_DAYS = 3;

    private final HomeTodayService homeTodayService;
    private final HomeFeedbackService homeFeedbackService;
    private final WeightRepository weightRepository;
    private final SleepRepository sleepRepository;
    private final WaterRepository waterRepository;
    private final StepRepository stepRepository;

    @SuppressWarnings("null")
    @Override
    public List<Map<String, Object>> buildFamilyMembers(List<Profile> profiles, Long currentUserId, LocalDate today) {
        List<Map<String, Object>> members = new ArrayList<>();

        for (Profile profile : profiles) {
            Long id = profile.getId();
            Map<String, Object> t = homeTodayService.buildToday(id, currentUserId);

            boolean hasWeight = !"NO_RECORD".equals(t.get("weightGoalStatus"));
            boolean hasSleep = !"NO_RECORD".equals(t.get("sleepGoalStatus"));
            boolean hasWater = !"NO_RECORD".equals(t.get("waterGoalStatus"));
            boolean hasStep = !"NO_RECORD".equals(t.get("stepGoalStatus"));
            int doneItems = (hasWeight ? 1 : 0) + (hasSleep ? 1 : 0) + (hasWater ? 1 : 0) + (hasStep ? 1 : 0);

            List<BaseLog> latestLogs = latestLogsOf(id);
            LocalDate latestDate = latestLogs.stream().map(BaseLog::getRecordedDate).filter(Objects::nonNull)
                    .max(LocalDate::compareTo).orElse(null);
            boolean hasHistory = latestDate != null;
            long daysSince = hasHistory ? ChronoUnit.DAYS.between(latestDate, today) : 0;

            List<String> severeLabels = new ArrayList<>();
            boolean hasLv4 = homeFeedbackService.collectSevereLabels(id, severeLabels);

            List<String> issues = new ArrayList<>(severeLabels);
            if (hasHistory && daysSince >= NO_RECORD_DAYS) {
                issues.add(daysSince + "日未記録");
            }
            if (!hasWeight)
                issues.add("体重未入力");
            if (!hasSleep)
                issues.add("睡眠未入力");
            if (!hasWater)
                issues.add("水分未入力");
            if (!hasStep)
                issues.add("歩数未入力");

            String status;
            String statusLabel;
            if (!hasHistory) {
                status = "NO_RECORD";
                statusLabel = "未記録";
            } else if (issues.isEmpty()) {
                status = "OK";
                statusLabel = "記録順調";
            } else {
                boolean danger = hasLv4 || !severeLabels.isEmpty() || daysSince >= NO_RECORD_DAYS;
                status = danger ? "DANGER" : "WARN";
                statusLabel = issues.size() == 1 ? issues.get(0) : issues.get(0) + " ほか" + (issues.size() - 1) + "件";
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", id);
            m.put("name", profile.getName());
            m.put("age", profile.getBirthDate() != null ? profile.getAge() : null);
            m.put("avatar", profile.getAvatar());
            m.put("status", status);
            m.put("statusLabel", statusLabel);
            m.put("doneItems", doneItems);
            m.put("totalItems", 4);
            m.put("updatedTime", formatUpdatedTime(latestLogs, today));
            m.put("relationship", profile.getRelationship());
            m.put("isPrimary", profile.isPrimary());
            members.add(m);
        }
        return members;
    }

    @Override
    public Map<String, Object> buildFamilySummary(List<Map<String, Object>> members) {
        long attention = members.stream().filter(m -> !"OK".equals(m.get("status"))).count();
        Map<String, Object> summary = new HashMap<>();
        summary.put("attentionCount", (int) attention);
        summary.put("totalCount", members.size());
        return summary;
    }

    /** Mỗi loại lấy bản ghi mới nhất */
    private List<BaseLog> latestLogsOf(Long profileId) {
        BaseLog w = weightRepository.findByProfile_IdOrderByRecordedDateDesc(profileId).stream().findFirst()
                .orElse(null);
        BaseLog s = sleepRepository.findTopByProfile_IdOrderByRecordedDateDesc(profileId).orElse(null);
        BaseLog wa = waterRepository.findTopByProfile_IdOrderByRecordedDateDesc(profileId).orElse(null);
        BaseLog st = stepRepository.findTopByProfile_IdOrderByRecordedDateDescIdDesc(profileId).orElse(null);
        return Stream.of(w, s, wa, st).filter(Objects::nonNull).toList();
    }

    @SuppressWarnings("null")
    private String formatUpdatedTime(List<BaseLog> logs, LocalDate today) {
        LocalDateTime latest = logs.stream().map(BaseLog::getUpdatedAt).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(null);
        if (latest == null) {
            return "-";
        }
        return latest.toLocalDate().equals(today) ? latest.format(DateTimeFormatter.ofPattern("HH:mm"))
                : latest.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }
}