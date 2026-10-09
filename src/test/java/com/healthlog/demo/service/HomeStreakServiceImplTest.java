package com.healthlog.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.healthlog.demo.repository.WeightRepository;
import com.healthlog.demo.service.home.HomeStreakServiceImpl;

// ストリーク計算ロジックのテスト
@ExtendWith(MockitoExtension.class)
class HomeStreakServiceImplTest {

    private static final Long PROFILE_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    @Mock
    private WeightRepository weightRepository;

    @InjectMocks
    private HomeStreakServiceImpl service;

    // 仮説: 指定された日付までの連続完了日を返すようにモックする。
    private void givenCompleteDays(List<LocalDate> dates) {
        List<LocalDateTime> all = dates.stream().map(d -> d.atTime(10, 0)).toList();
        when(weightRepository.findCompleteStreakDays(eq(PROFILE_ID), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime before = invocation.getArgument(1);
                    return all.stream().filter(t -> t.isBefore(before)).toList();
                });
    }

    // ヘルパー: 2つの日付の範囲をリストにする。
    private static List<LocalDate> range(LocalDate from, LocalDate to) {
        List<LocalDate> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.add(d);
        }
        return result;
    }

    // ストリークが途切れていない場合のテスト
    @Test
    @DisplayName("Chưa có dữ liệu nào thì streak = 0")
    void noData_streakIsZero() {
        givenCompleteDays(List.of());

        assertEquals(0, service.getCurrentStreak(PROFILE_ID, TODAY));
        assertTrue(service.getCurrentStreakInfo(PROFILE_ID, TODAY).isEmpty());
        assertFalse(service.hasPreviousStreak(PROFILE_ID, TODAY));
    }

    // ストリークが途切れた場合のテスト
    @Test
    @DisplayName("Chỉ có hôm nay thì streak = 1")
    void onlyToday_streakIsOne() {
        givenCompleteDays(List.of(TODAY));

        assertEquals(1, service.getCurrentStreak(PROFILE_ID, TODAY));
    }

    @Test
    @DisplayName("Chỉ có hôm qua (hôm nay chưa nhập) thì streak vẫn còn = 1")
    void onlyYesterday_streakStillAlive() {
        givenCompleteDays(List.of(TODAY.minusDays(1)));

        assertEquals(1, service.getCurrentStreak(PROFILE_ID, TODAY));
        assertFalse(service.hasPreviousStreak(PROFILE_ID, TODAY));
    }

    @Test
    @DisplayName("7 ngày liên tiếp kết thúc hôm nay thì streak = 7")
    void sevenDaysEndingToday() {
        givenCompleteDays(range(TODAY.minusDays(6), TODAY));

        assertEquals(7, service.getCurrentStreak(PROFILE_ID, TODAY));
        assertEquals(7, service.getCurrentStreakInfo(PROFILE_ID, TODAY).get().days());
    }

    @Test
    @DisplayName("7 ngày liên tiếp kết thúc hôm qua thì streak = 7")
    void sevenDaysEndingYesterday() {
        givenCompleteDays(range(TODAY.minusDays(7), TODAY.minusDays(1)));

        assertEquals(7, service.getCurrentStreak(PROFILE_ID, TODAY));
    }

    @Test
    @DisplayName("Bỏ một ngày ở giữa thì chỉ đếm đoạn mới nhất")
    void gapInMiddle_countsOnlyLatestRun() {
        List<LocalDate> dates = new ArrayList<>();
        dates.addAll(range(TODAY.minusDays(1), TODAY)); // hôm qua, hôm nay: 2 ngày
        dates.addAll(range(TODAY.minusDays(5), TODAY.minusDays(3))); // bỏ ngày TODAY-2
        givenCompleteDays(dates);

        assertEquals(2, service.getCurrentStreak(PROFILE_ID, TODAY));
    }

    // 同じ日に複数の記録があっても1日としてカウントされることを確認するテスト
    @Test
    @DisplayName("Nhiều bản ghi trong cùng một ngày vẫn chỉ tính 1 ngày")
    void duplicateEntriesSameDay_countOnce() {
        List<LocalDate> dates = new ArrayList<>(range(TODAY.minusDays(2), TODAY));
        dates.add(TODAY);
        dates.add(TODAY.minusDays(1));
        givenCompleteDays(dates);

        assertEquals(3, service.getCurrentStreak(PROFILE_ID, TODAY));
    }

    // ストリークが月や年をまたいでも正しくカウントされることを確認するテスト
    @Test
    @DisplayName("Streak đi qua ranh giới tháng và năm")
    void streakAcrossMonthAndYearBoundary() {
        LocalDate today = LocalDate.of(2027, 1, 2);
        givenCompleteDays(range(LocalDate.of(2026, 12, 30), today)); // 30,31/12 + 1,2/1

        assertEquals(4, service.getCurrentStreak(PROFILE_ID, today));
    }

    // 将来の日付の記録はストリークにカウントされないことを確認するテスト
    @Test
    @DisplayName("Ngày trong tương lai không được tính")
    void futureDay_isIgnored() {
        givenCompleteDays(List.of(TODAY, TODAY.plusDays(1)));

        assertEquals(1, service.getCurrentStreak(PROFILE_ID, TODAY));
    }

    @Test
    @DisplayName("Lần hoàn thành cuối cách đây 2 ngày thì streak = 0 và có streak đã đứt")
    void lastCompleteTwoDaysAgo_streakBroken() {
        givenCompleteDays(range(TODAY.minusDays(6), TODAY.minusDays(2))); // 5 ngày liên tiếp

        assertEquals(0, service.getCurrentStreak(PROFILE_ID, TODAY));
        assertTrue(service.hasPreviousStreak(PROFILE_ID, TODAY));
        assertEquals(5, service.getBrokenStreak(PROFILE_ID, TODAY).get().days());
    }

    @Test
    @DisplayName("Streak đã đứt chỉ có 1 ngày thì vẫn được ghi nhận là 1")
    void brokenStreakOfOneDay() {
        givenCompleteDays(List.of(TODAY.minusDays(3)));

        assertEquals(0, service.getCurrentStreak(PROFILE_ID, TODAY));
        assertEquals(1, service.getBrokenStreak(PROFILE_ID, TODAY).get().days());
    }

    @Test
    @DisplayName("Đang có streak thì không có thông báo streak đứt")
    void activeStreak_noBrokenNotice() {
        givenCompleteDays(range(TODAY.minusDays(3), TODAY));

        assertTrue(service.getBrokenStreak(PROFILE_ID, TODAY).isEmpty());
    }

    // ストリークが途切れた後に再開した場合、ストリークは1から再スタートすることを確認するテスト
    @Test
    @DisplayName("Quay lại sau khi đứt thì streak bắt đầu lại từ 1")
    void restartAfterBreak() {
        List<LocalDate> dates = new ArrayList<>(range(TODAY.minusDays(10), TODAY.minusDays(5))); // streak cũ 6 ngày
        dates.add(TODAY); // nhập lại hôm nay
        givenCompleteDays(dates);
        assertEquals(1, service.getCurrentStreak(PROFILE_ID, TODAY));
    }

    // うるう年の2月29日をまたいでも正しくカウントされることを確認するテスト
    @Test
    @DisplayName("Năm nhuận: Streak đi qua ngày 29/02")
    void leapYear_streakAcrossFebruary29() {
        LocalDate today = LocalDate.of(2028, 3, 2);
        givenCompleteDays(range(LocalDate.of(2028, 2, 27), today));
        assertEquals(5, service.getCurrentStreak(PROFILE_ID, today));
    }
}