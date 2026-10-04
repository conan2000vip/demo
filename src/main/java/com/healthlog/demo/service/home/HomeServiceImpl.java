package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.service.profile.ProfileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final ProfileService profileService;
    private final HomeTodayService homeTodayService;
    private final HomeFamilyService homeFamilyService;
    private final HomeStreakService homeStreakService;
    private final HomeFeedbackService homeFeedbackService;

    @Override
    public Map<String, Object> getHomeData(Long profileId, Long currentUserId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Profile currentProfile = profileService.getProfile(currentUserId, profileId);
        List<Profile> profileList = profileService.getProfiles(currentUserId);

        List<Profile> others = profileList.stream().filter(p -> !p.getId().equals(currentProfile.getId())).toList();
        List<Map<String, Object>> familyMembers = homeFamilyService.buildFamilyMembers(others, currentUserId, today);

        Map<String, Object> data = new HashMap<>();
        data.put("currentProfile", currentProfile);
        data.put("goals", currentProfile);
        data.put("profileList", profileList);
        data.put("today", homeTodayService.buildToday(profileId, currentUserId));
        data.put("familyMembers", familyMembers);
        data.put("familySummary", homeFamilyService.buildFamilySummary(familyMembers));
        data.put("feedbackList", homeFeedbackService.getHomeFeedback(profileId));
        Optional<HomeStreakService.StreakInfo> currentStreak = homeStreakService.getCurrentStreakInfo(profileId, today);
        if (currentStreak.isPresent()) {
            HomeStreakService.StreakInfo streak = currentStreak.get();
            data.put("currentStreak", streak.days());
            data.put("currentStreakStartDate", streak.startDate().toString());
        } else {
            data.put("currentStreak", 0);
            data.put("currentStreakStartDate", "");
        }
        Optional<HomeStreakService.StreakBreak> brokenStreak = homeStreakService.getBrokenStreak(profileId, today);
        data.put("hasPreviousStreak", brokenStreak.isPresent());
        if (brokenStreak.isPresent()) {
            HomeStreakService.StreakBreak streak = brokenStreak.get();
            data.put("brokenStreakDays", streak.days());
            data.put("brokenStreakStartDate", streak.startDate().toString());
            data.put("brokenStreakDate", streak.lastCompletedDate().toString());
        } else {
            data.put("brokenStreakDays", 0);
            data.put("brokenStreakStartDate", "");
            data.put("brokenStreakDate", "");
        }
        return data;
    }
}