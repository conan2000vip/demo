package com.healthlog.demo.service.memo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.common.DateRangerFilter;
import com.healthlog.demo.entity.Memo;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.MemoRepository;
import com.healthlog.demo.service.base.BaseLogService;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

@Service
@Transactional
public class MemoServiceImpl extends BaseLogService<Memo, Memo> implements MemoService {
    private static final int PAGE_SIZE = 10;
    private static final int MAX_MEMOS_PER_DAY = 5;
    private static final int RECENT_MEMOS_LIMIT = 5;
    private final MemoRepository memoRepository;

    public MemoServiceImpl(MemoRepository memoRepository, ProfileAccessValidation accessValidation) {
        super(memoRepository, accessValidation);
        this.memoRepository = memoRepository;
    }

    @Override
    protected Memo mapToDto(Memo entity) {
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long profileId, Long currentUserId, DateRangerFilter range, int page) {
        validatePageAndRange(range, page);
        Profile profile = validateAndGetProfile(profileId, currentUserId);
        Page<Memo> resultPage = getLogsPaged(profileId, currentUserId, range, PageRequest.of(page, PAGE_SIZE));
        Map<String, Object> result = new HashMap<>();
        result.put("currentProfile", profile);
        result.put("logs", resultPage.getContent());
        result.put("hasAnyLog", memoRepository.existsByProfile_Id(profileId));
        result.put("currentPage", resultPage.getNumber());
        result.put("totalPages", resultPage.getTotalPages());
        result.put("hasPrevious", resultPage.hasPrevious());
        result.put("hasNext", resultPage.hasNext());
        result.put("filterStartDate", range != null ? range.getFrom() : null);
        result.put("filterEndDate", range != null ? range.getTo() : null);
        return result;
    }

    @Override
    public Memo create(Long profileId, Long currentUserId, Memo input) {
        Profile profile = validateAndGetProfile(profileId, currentUserId);
        normalizeInput(input);
        validateMemo(input);
        long countOnDate = memoRepository.countByProfile_IdAndRecordedDate(profileId, input.getRecordedDate());
        if (countOnDate >= MAX_MEMOS_PER_DAY) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "1日に記録できるメモは" + MAX_MEMOS_PER_DAY + "件までです");
        }
        input.setProfile(profile);
        input.setMeasuredAt(input.getRecordedDate().atStartOfDay());
        return memoRepository.save(input);
    }

    @Override
    public Memo update(Long profileId, Long currentUserId, Long logId, Memo input) {
        validateAndGetProfile(profileId, currentUserId);
        Memo existing = findEntityByIdAndProfile(logId, profileId);
        normalizeInput(input);
        validateMemo(input);
        existing.setRecordedDate(input.getRecordedDate());
        existing.setMeasuredAt(input.getRecordedDate().atStartOfDay());
        existing.setTitle(input.getTitle());
        existing.setContent(input.getContent());
        return memoRepository.save(existing);
    }

    @Override
    public void delete(Long profileId, Long currentUserId, Long logId) {
        deleteLog(logId, profileId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Memo> getRecentThreeDays(Long profileId, Long currentUserId) {
        validateAndGetProfile(profileId, currentUserId);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(2);
        List<Memo> memos = memoRepository.findByProfile_IdAndRecordedDateBetweenOrderByRecordedDateDescIdDesc(
                profileId, from, to);
        return memos.size() > RECENT_MEMOS_LIMIT ? memos.subList(0, RECENT_MEMOS_LIMIT) : memos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Memo> getByDate(Long profileId, Long currentUserId, LocalDate recordedDate) {
        validateAndGetProfile(profileId, currentUserId);
        if (recordedDate == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "記録日を指定してください");
        }
        return memoRepository.findByProfile_IdAndRecordedDateOrderByIdDesc(profileId, recordedDate);
    }

    private void normalizeInput(Memo memo) {
        if (memo.getTitle() != null) {
            String title = memo.getTitle().trim();
            memo.setTitle(title.isEmpty() ? null : title);
        }
        if (memo.getContent() != null)
            memo.setContent(memo.getContent().trim());
    }

    private void validateMemo(Memo memo) {
        if (memo.getRecordedDate() == null || memo.getRecordedDate().isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未来の日付は指定できません。日付を確認してください。");
        }
        if (memo.getContent() == null || memo.getContent().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メモ内容を入力してください。");
        }
        if (memo.getContent().length() > 2000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "メモ内容は2000文字以内で入力してください。");
        }
        if (memo.getTitle() != null && memo.getTitle().length() > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "タイトルは100文字以内で入力してください。");
        }
    }

    private void validatePageAndRange(DateRangerFilter range, int page) {
        if (page < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ページ番号が正しくありません。");
        }
        if (range != null && range.getFrom() != null && range.getTo() != null
                && range.getFrom().isAfter(range.getTo())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "開始日が終了日より後になっています。");
        }
    }
}
