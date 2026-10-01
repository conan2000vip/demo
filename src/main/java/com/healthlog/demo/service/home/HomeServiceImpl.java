package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        LocalDate today = LocalDate.now();
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
        data.put("currentStreak", homeStreakService.getCurrentStreak(profileId, today));
        return data;
    }
}