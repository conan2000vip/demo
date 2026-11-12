package com.healthlog.demo.service.home;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.service.feedbackservice.FeedbackService;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.profile.ProfileService;

class HomeServiceImplTest {

    private static final Long PROFILE_ID = 20L;
    private static final Long USER_ID = 100L;

    @Test
    @DisplayName("フィードバックが空で閲覧できないカテゴリがあれば権限制限を返す")
    void marksEmptyFeedbackAsRestrictedWhenCategoriesCannotBeViewed() {
        ProfileService profileService = mock(ProfileService.class);
        HomeTodayService homeTodayService = mock(HomeTodayService.class);
        HomeFamilyService homeFamilyService = mock(HomeFamilyService.class);
        HomeStreakService homeStreakService = mock(HomeStreakService.class);
        ProfileAccessValidation access = mock(ProfileAccessValidation.class);
        FeedbackService feedbackService = mock(FeedbackService.class);
        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(PROFILE_ID);
        when(access.validateCanViewProfile(PROFILE_ID, USER_ID)).thenReturn(profile);
        when(profileService.getProfiles(USER_ID)).thenReturn(List.of(profile));
        when(homeTodayService.buildToday(PROFILE_ID, USER_ID)).thenReturn(Map.of());
        when(homeFamilyService.buildFamilyMembers(List.of(), USER_ID, java.time.LocalDate.now()))
                .thenReturn(List.of());
        when(homeFamilyService.buildFamilySummary(List.of())).thenReturn(Map.of());
        when(access.hasReadAccess(PROFILE_ID, USER_ID, Category.weight)).thenReturn(false);
        when(access.hasReadAccess(PROFILE_ID, USER_ID, Category.sleep)).thenReturn(false);
        when(access.hasReadAccess(PROFILE_ID, USER_ID, Category.water)).thenReturn(false);
        when(access.hasReadAccess(PROFILE_ID, USER_ID, Category.step)).thenReturn(false);
        when(access.hasReadAccess(PROFILE_ID, USER_ID, Category.memo)).thenReturn(false);
        when(feedbackService.getWeightFeedback(PROFILE_ID)).thenReturn(List.of());
        when(feedbackService.getSleepFeedback(PROFILE_ID)).thenReturn(List.of());
        when(feedbackService.getWaterFeedback(PROFILE_ID)).thenReturn(List.of());
        when(feedbackService.getStepFeedback(PROFILE_ID)).thenReturn(List.of());
        when(homeStreakService.getCurrentStreakInfo(PROFILE_ID, java.time.LocalDate.now()))
                .thenReturn(Optional.empty());
        when(homeStreakService.getBrokenStreak(PROFILE_ID, java.time.LocalDate.now()))
                .thenReturn(Optional.empty());

        HomeServiceImpl service = new HomeServiceImpl(profileService, homeTodayService, homeFamilyService,
                homeStreakService, access, feedbackService);

        Map<String, Object> result = service.getHomeData(PROFILE_ID, USER_ID);

        assertTrue((Boolean) result.get("hasRestrictedFeedbackCategories"));
    }
}
