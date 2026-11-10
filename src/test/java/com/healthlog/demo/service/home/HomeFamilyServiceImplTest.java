package com.healthlog.demo.service.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthlog.demo.entity.BaseLog;
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

@ExtendWith(MockitoExtension.class)
class HomeFamilyServiceImplTest {

    private static final Long PROFILE_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @Mock
    private ProfileAccessValidation profileAccessValidation;
    @Mock
    private HomeTodayService homeTodayService;
    @Mock
    private WeightRepository weightRepository;
    @Mock
    private SleepRepository sleepRepository;
    @Mock
    private WaterRepository waterRepository;
    @Mock
    private StepRepository stepRepository;

    private HomeFamilyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HomeFamilyServiceImpl(profileAccessValidation, homeTodayService, weightRepository,
                sleepRepository, waterRepository, stepRepository);

        lenient().when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.weight)).thenReturn(true);
        lenient().when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.sleep)).thenReturn(true);
        lenient().when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.water)).thenReturn(true);
        lenient().when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.step)).thenReturn(true);
        lenient().when(homeTodayService.buildToday(PROFILE_ID, USER_ID))
                .thenReturn(todayStatuses("NO_RECORD", "NO_RECORD", "NO_RECORD", "NO_RECORD"));
        lenient().when(weightRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID))
                .thenReturn(Optional.empty());
        lenient().when(sleepRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID))
                .thenReturn(Optional.empty());
        lenient().when(waterRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID))
                .thenReturn(Optional.empty());
        lenient().when(stepRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Chưa có lịch sử thì status là NO_RECORD")
    void noHistory_isNoRecord() {
        Map<String, Object> member = buildMember();

        assertEquals("NO_RECORD", member.get("status"));
        assertEquals("未記録", member.get("statusLabel"));
        assertEquals(0, member.get("doneItems"));
        assertEquals(4, member.get("totalItems"));
    }

    @Test
    @DisplayName("Có đủ bản ghi hôm nay thì status là OK")
    void allItemsRecordedToday_isOk() {
        when(homeTodayService.buildToday(PROFILE_ID, USER_ID)).thenReturn(todayStatuses("OK", "OK", "OK", "OK"));
        setLatestDates(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(3));

        Map<String, Object> member = buildMember();

        assertEquals("OK", member.get("status"));
        assertEquals("記録順調", member.get("statusLabel"));
        assertEquals(4, member.get("doneItems"));
        assertEquals(4, member.get("totalItems"));
        assertEquals(TODAY.toString(), member.get("updatedTime"));
    }

    @Test
    @DisplayName("Thiếu bản ghi hôm nay nhưng lịch sử gần đây thì status là WARN")
    void missingTodayItemWithRecentHistory_isWarn() {
        when(homeTodayService.buildToday(PROFILE_ID, USER_ID)).thenReturn(todayStatuses("OK", "NO_RECORD", "OK", "OK"));
        setLatestDates(TODAY, null, TODAY.minusDays(1), TODAY.minusDays(2));

        Map<String, Object> member = buildMember();

        assertEquals("WARN", member.get("status"));
        assertEquals("睡眠未入力", member.get("statusLabel"));
        assertEquals(3, member.get("doneItems"));
    }

    @Test
    @DisplayName("Lần ghi gần nhất cách đúng 3 ngày thì status là DANGER")
    void latestRecordThreeDaysAgo_isDanger() {
        setLatestDates(TODAY.minusDays(3), null, null, null);

        Map<String, Object> member = buildMember();

        assertEquals("DANGER", member.get("status"));
        assertEquals("3日未記録 ほか4件", member.get("statusLabel"));
    }

    @Test
    @DisplayName("Lần ghi gần nhất cách 2 ngày thì vẫn là WARN")
    void latestRecordTwoDaysAgo_isWarn() {
        setLatestDates(TODAY.minusDays(2), null, null, null);

        Map<String, Object> member = buildMember();

        assertEquals("WARN", member.get("status"));
        assertEquals("体重未入力 ほか3件", member.get("statusLabel"));
    }

    @Test
    @DisplayName("Mục không có quyền xem không được tính vào tổng và cảnh báo")
    void inaccessibleItemsAreExcluded() {
        when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.sleep)).thenReturn(false);
        when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.water)).thenReturn(false);
        when(profileAccessValidation.hasReadAccess(PROFILE_ID, USER_ID, Category.step)).thenReturn(false);
        when(homeTodayService.buildToday(PROFILE_ID, USER_ID))
                .thenReturn(todayStatuses("OK", "NO_RECORD", "NO_RECORD", "NO_RECORD"));
        setLatestDates(TODAY, null, null, null);

        Map<String, Object> member = buildMember();

        assertEquals("OK", member.get("status"));
        assertEquals(1, member.get("doneItems"));
        assertEquals(1, member.get("totalItems"));
    }

    @Test
    @DisplayName("Summary đếm các thành viên cần chú ý")
    void summaryCountsMembersNeedingAttention() {
        Map<String, Object> summary = service.buildFamilySummary(List.of(Map.of("status", "OK"),
                Map.of("status", "WARN"), Map.of("status", "DANGER"), Map.of("status", "NO_RECORD")));

        assertEquals(3, summary.get("attentionCount"));
        assertEquals(4, summary.get("totalCount"));
    }

    private Map<String, Object> buildMember() {
        Profile profile = new Profile(null, "Thành viên", "family");
        profile.setId(PROFILE_ID);
        return service.buildFamilyMembers(List.of(profile), USER_ID, TODAY).get(0);
    }

    private static Map<String, Object> todayStatuses(String weight, String sleep, String water, String step) {
        return Map.of("weightGoalStatus", weight, "sleepGoalStatus", sleep, "waterGoalStatus", water, "stepGoalStatus",
                step);
    }

    private void setLatestDates(LocalDate weightDate, LocalDate sleepDate, LocalDate waterDate, LocalDate stepDate) {
        Optional<Weight> weight = weightDate == null ? Optional.empty() : latestLog(Weight.class, weightDate);
        Optional<Sleep> sleep = sleepDate == null ? Optional.empty() : latestLog(Sleep.class, sleepDate);
        Optional<Water> water = waterDate == null ? Optional.empty() : latestLog(Water.class, waterDate);
        Optional<Step> step = stepDate == null ? Optional.empty() : latestLog(Step.class, stepDate);
        if (weightDate != null) {
            when(weightRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID)).thenReturn(weight);
        }
        if (sleepDate != null) {
            when(sleepRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID)).thenReturn(sleep);
        }
        if (waterDate != null) {
            when(waterRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID)).thenReturn(water);
        }
        if (stepDate != null) {
            when(stepRepository.findTopByProfile_IdOrderByRecordedDateDesc(PROFILE_ID)).thenReturn(step);
        }
    }

    private static <T extends BaseLog> Optional<T> latestLog(Class<T> type, LocalDate date) {
        return Optional.of(logWithDate(type, date));
    }

    private static <T extends BaseLog> T logWithDate(Class<T> type, LocalDate date) {
        T log = mock(type);
        when(log.getRecordedDate()).thenReturn(date);
        return log;
    }
}
