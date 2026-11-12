package com.healthlog.demo.service.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.repository.SleepRepository;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.repository.WaterRepository;
import com.healthlog.demo.repository.WeightRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;
import com.healthlog.demo.service.sleep.SleepService;
import com.healthlog.demo.service.step.StepService;
import com.healthlog.demo.service.water.WaterService;
import com.healthlog.demo.service.weight.WeightService;

class HomeSummaryChartServiceImplTest {

    private static final Long PROFILE_ID = 20L;
    private static final Long USER_ID = 100L;

    @Test
    @DisplayName("共有されていないカテゴリに記録があれば未共有データありを返す")
    void reportsUnsharedDataWhenHiddenCategoryHasRecords() {
        Dependencies dependencies = new Dependencies();
        when(dependencies.access.hasReadAccess(PROFILE_ID, USER_ID, Category.weight)).thenReturn(false);
        when(dependencies.weightRepository.existsByProfile_Id(PROFILE_ID)).thenReturn(true);

        Map<String, Object> result = dependencies.service().getSummaryChartData(
                PROFILE_ID, USER_ID, LocalDate.now().minusDays(6), LocalDate.now());

        assertTrue((Boolean) result.get("hasUnsharedData"));
        assertFalse((Boolean) result.get("hasEverLogged"));
    }

    @Test
    @DisplayName("記録がなく共有されていないカテゴリもなければ未共有データなしを返す")
    void reportsNoUnsharedDataWhenNoHiddenRecordsExist() {
        Dependencies dependencies = new Dependencies();

        Map<String, Object> result = dependencies.service().getSummaryChartData(
                PROFILE_ID, USER_ID, LocalDate.now().minusDays(6), LocalDate.now());

        assertFalse((Boolean) result.get("hasUnsharedData"));
        assertFalse((Boolean) result.get("hasEverLogged"));
    }

    @Test
    @DisplayName("共有されていないカテゴリのEMPTYではなく閲覧可能カテゴリのグラフ形式を返す")
    void usesChartModeFromReadableCategory() {
        Dependencies dependencies = new Dependencies();
        when(dependencies.access.hasReadAccess(PROFILE_ID, USER_ID, Category.weight)).thenReturn(false);
        when(dependencies.access.hasReadAccess(PROFILE_ID, USER_ID, Category.sleep)).thenReturn(true);
        when(dependencies.sleepService.chartData(
                org.mockito.ArgumentMatchers.eq(PROFILE_ID),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of(
                        "labels", java.util.List.of("2026-11-06"),
                        "values", java.util.List.of(new java.math.BigDecimal("8.0")),
                        "chartMode", "DAY"));

        Map<String, Object> result = dependencies.service().getSummaryChartData(
                PROFILE_ID, USER_ID, LocalDate.now().minusDays(6), LocalDate.now());

        assertEquals("DAY", result.get("chartMode"));
        assertEquals(java.util.List.of("2026-11-06"), result.get("labels"));
    }

    private static class Dependencies {
        private final WeightService weightService = mock(WeightService.class);
        private final SleepService sleepService = mock(SleepService.class);
        private final WaterService waterService = mock(WaterService.class);
        private final StepService stepService = mock(StepService.class);
        private final WeightRepository weightRepository = mock(WeightRepository.class);
        private final SleepRepository sleepRepository = mock(SleepRepository.class);
        private final WaterRepository waterRepository = mock(WaterRepository.class);
        private final StepRepository stepRepository = mock(StepRepository.class);
        private final ProfileAccessValidation access = mock(ProfileAccessValidation.class);

        private HomeSummaryChartServiceImpl service() {
            return new HomeSummaryChartServiceImpl(
                    weightService,
                    sleepService,
                    waterService,
                    stepService,
                    weightRepository,
                    sleepRepository,
                    waterRepository,
                    stepRepository,
                    access);
        }
    }
}
