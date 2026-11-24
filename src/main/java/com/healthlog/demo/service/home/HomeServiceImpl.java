package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.dto.feedback.FeedbackItem;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.feedbackservice.FeedbackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final ProfileService profileService;
    private final HomeTodayService homeTodayService;
    private final HomeFamilyService homeFamilyService;
    private final HomeStreakService homeStreakService;
    private final ProfileAccessValidation profileAccessValidation;
    private final FeedbackService feedbackService;

    @Override
    public Map<String, Object> getHomeData(Long targetProfileId, Long currentUserId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Profile currentProfile = profileAccessValidation.validateCanViewProfile(targetProfileId, currentUserId);
        List<Profile> profileList = profileService.getProfiles(currentUserId);
        Map<Category, Boolean> readAccess = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            readAccess.put(category,
                    profileAccessValidation.hasReadAccess(targetProfileId, currentUserId, category));
        }

        List<Profile> others = profileList.stream().filter(p -> !p.getId().equals(currentProfile.getId())).toList();
        List<Map<String, Object>> familyMembers = homeFamilyService.buildFamilyMembers(others, currentUserId, today);

        Map<String, Object> data = new HashMap<>();
        data.put("currentProfile", currentProfile);
        data.put("goals", visibleGoals(currentProfile, readAccess));
        data.put("profileList", profileList);
        data.put("today", homeTodayService.buildToday(targetProfileId, currentUserId));
        boolean canViewWeight = readAccess.get(Category.weight);
        boolean canViewSleep = readAccess.get(Category.sleep);
        boolean canViewWater = readAccess.get(Category.water);
        boolean canViewStep = readAccess.get(Category.step);
        data.put("canViewWeight", canViewWeight);
        data.put("canViewSleep", canViewSleep);
        data.put("canViewWater", canViewWater);
        data.put("canViewStep", canViewStep);
        data.put("canViewMemo", readAccess.get(Category.memo));
        data.put("familyMembers", familyMembers);
        data.put("familySummary", homeFamilyService.buildFamilySummary(familyMembers));
        List<FeedbackItem> feedbackList = visibleFeedback(targetProfileId, readAccess);
        data.put("feedbackList", feedbackList);
        data.put("hasRestrictedFeedbackCategories",
                !canViewWeight || !canViewSleep || !canViewWater || !canViewStep);
        Optional<HomeStreakService.StreakInfo> currentStreak = homeStreakService.getCurrentStreakInfo(targetProfileId,
                today);
        if (currentStreak.isPresent()) {
            HomeStreakService.StreakInfo streak = currentStreak.get();
            data.put("currentStreak", streak.days());
            data.put("currentStreakStartDate", streak.startDate().toString());
        } else {
            data.put("currentStreak", 0);
            data.put("currentStreakStartDate", "");
        }
        Optional<HomeStreakService.StreakBreak> brokenStreak = homeStreakService.getBrokenStreak(targetProfileId,
                today);
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

    private Map<String, Object> visibleGoals(Profile profile, Map<Category, Boolean> readAccess) {
        Map<String, Object> goals = new HashMap<>();
        if (readAccess.get(Category.weight)) {
            goals.put("targetWeight", profile.getTargetWeight());
        }
        if (readAccess.get(Category.sleep)) {
            goals.put("formattedSleepGoal", profile.getFormattedSleepGoal());
        }
        if (readAccess.get(Category.water)) {
            goals.put("waterGoalMl", profile.getWaterGoalMl());
        }
        if (readAccess.get(Category.step)) {
            goals.put("stepGoal", profile.getStepGoal());
        }
        return goals;
    }

    private List<FeedbackItem> visibleFeedback(Long profileId, Map<Category, Boolean> readAccess) {
        List<FeedbackItem> feedback = new java.util.ArrayList<>();
        if (readAccess.get(Category.weight)) {
            feedback.addAll(feedbackService.getWeightFeedback(profileId));
        }
        if (readAccess.get(Category.sleep)) {
            feedback.addAll(feedbackService.getSleepFeedback(profileId));
        }
        if (readAccess.get(Category.water)) {
            feedback.addAll(feedbackService.getWaterFeedback(profileId));
        }
        if (readAccess.get(Category.step)) {
            feedback.addAll(feedbackService.getStepFeedback(profileId));
        }
        return feedback;
    }
}