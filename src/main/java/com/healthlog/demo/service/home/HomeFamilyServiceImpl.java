package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.healthlog.demo.entity.Profile;
import lombok.RequiredArgsConstructor;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

@Service
@RequiredArgsConstructor
public class HomeFamilyServiceImpl implements HomeFamilyService {

    private static final int NO_RECORD_DAYS = 3;

    private final ProfileAccessValidation profileAccessValidation;
    private final HomeTodayService homeTodayService;

    @Override
    public List<Map<String, Object>> buildFamilyMembers(List<Profile> profiles, Long currentUserId,
            Long activeProfileId, LocalDate today) {
        List<Map<String, Object>> members = new ArrayList<>();

        for (Profile profile : profiles) {
            Long id = profile.getId();

            System.out.println("=== DEBUG PERMISSION ===");
            System.out.println("Owner (Thẻ gia đình): " + id + " | Viewer (Đang xem): " + activeProfileId);

            // チェック：activeProfileIdがnullの場合、profileAccessValidation.hasReadAccessはnullを返す可能性があるため、activeProfileIdがnullの場合はfalseを返すようにする
            boolean canViewWeight = profileAccessValidation.hasReadAccess(id, activeProfileId, Category.weight);
            boolean canViewSleep = profileAccessValidation.hasReadAccess(id, activeProfileId, Category.sleep);
            boolean canViewWater = profileAccessValidation.hasReadAccess(id, activeProfileId, Category.water);
            boolean canViewStep = profileAccessValidation.hasReadAccess(id, activeProfileId, Category.step);

            int totalItems = (canViewWeight ? 1 : 0) + (canViewSleep ? 1 : 0) + (canViewWater ? 1 : 0)
                    + (canViewStep ? 1 : 0);
            Map<String, Object> t = homeTodayService.buildToday(id, currentUserId, activeProfileId);

            boolean hasWeight = !"NO_RECORD".equals(t.get("weightGoalStatus"));
            boolean hasSleep = !"NO_RECORD".equals(t.get("sleepGoalStatus"));
            boolean hasWater = !"NO_RECORD".equals(t.get("waterGoalStatus"));
            boolean hasStep = !"NO_RECORD".equals(t.get("stepGoalStatus"));

            int doneItems = (hasWeight ? 1 : 0) + (hasSleep ? 1 : 0) + (hasWater ? 1 : 0) + (hasStep ? 1 : 0);

            boolean hasHistory = hasWeight || hasSleep || hasWater || hasStep;
            LocalDate latestDate = hasHistory ? today : null;
            long daysSince = hasHistory ? ChronoUnit.DAYS.between(latestDate, today) : 0;

            List<String> severeLabels = new ArrayList<>();
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
                boolean danger = daysSince >= NO_RECORD_DAYS;
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
            m.put("totalItems", totalItems);
            m.put("updatedTime", "-");
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

}