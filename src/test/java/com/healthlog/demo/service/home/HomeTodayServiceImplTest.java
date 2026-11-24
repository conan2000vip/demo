package com.healthlog.demo.service.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.Sleep;
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.entity.Water;
import com.healthlog.demo.entity.Weight;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.profile.ProfileService;
import com.healthlog.demo.service.weight.BmiCalculator;

class HomeTodayServiceImplTest {

    private static final Long PROFILE_ID = 20L;
    private static final Long USER_ID = 100L;

    @Test
    @DisplayName("Home lấy trực tiếp bản ghi trong ngày và giữ nguyên trạng thái mục tiêu")
    void loadsOnlyTodayDataForDashboard() {
        ProfileService profileService = mock(ProfileService.class);
        ProfileAccessValidation access = mock(ProfileAccessValidation.class);
        WeightRepository weightRepository = mock(WeightRepository.class);
        SleepRepository sleepRepository = mock(SleepRepository.class);
        WaterRepository waterRepository = mock(WaterRepository.class);
        StepRepository stepRepository = mock(StepRepository.class);
        BmiCalculator bmiCalculator = new BmiCalculator();

        Profile profile = mock(Profile.class);
        when(profile.getHeight()).thenReturn(new BigDecimal("175"));
        when(profile.getTargetWeight()).thenReturn(new BigDecimal("70"));
        when(profile.getDailySleepGoal()).thenReturn(new BigDecimal("7.5"));
        when(profile.getWaterGoalMl()).thenReturn(2000);
        when(profile.getStepGoal()).thenReturn(10000);
        when(profileService.getProfile(USER_ID, PROFILE_ID)).thenReturn(profile);

        Weight weight = mock(Weight.class);
        when(weight.getWeight()).thenReturn(new BigDecimal("70"));
        when(weightRepository.findFirstByProfile_IdAndRecordedDateOrderByIdDesc(PROFILE_ID, LocalDate.now()))
                .thenReturn(Optional.of(weight));
        Sleep sleep = mock(Sleep.class);
        when(sleep.getSleepMinutes()).thenReturn(420);
        when(sleepRepository.findByProfile_IdAndRecordedDateOrderByStartTimeAsc(PROFILE_ID, LocalDate.now()))
                .thenReturn(List.of(sleep));
        Water water = mock(Water.class);
        when(water.getAmountMl()).thenReturn(2000);
        when(waterRepository.findByProfile_IdAndRecordedDateOrderByIdAsc(PROFILE_ID, LocalDate.now()))
                .thenReturn(List.of(water));
        Step step = mock(Step.class);
        when(step.getSteps()).thenReturn(10000);
        when(stepRepository.findFirstByProfile_IdAndRecordedDate(PROFILE_ID, LocalDate.now()))
                .thenReturn(Optional.of(step));

        HomeTodayServiceImpl service = new HomeTodayServiceImpl(profileService, access, weightRepository,
                sleepRepository, waterRepository, stepRepository, bmiCalculator);

        var result = service.buildToday(PROFILE_ID, USER_ID);

        assertEquals(new BigDecimal("70"), result.get("weightKg"));
        assertEquals("normal", result.get("bmiStatusCode"));
        assertEquals("ACHIEVED", result.get("weightGoalStatus"));
        assertEquals("ACHIEVED", result.get("sleepGoalStatus"));
        assertEquals("ACHIEVED", result.get("waterGoalStatus"));
        assertEquals("ACHIEVED", result.get("stepGoalStatus"));
        verify(access).validateAndGetProfile(PROFILE_ID, USER_ID, Category.weight);
        verify(access).validateAndGetProfile(PROFILE_ID, USER_ID, Category.sleep);
        verify(access).validateAndGetProfile(PROFILE_ID, USER_ID, Category.water);
        verify(access).validateAndGetProfile(PROFILE_ID, USER_ID, Category.step);
    }
}
