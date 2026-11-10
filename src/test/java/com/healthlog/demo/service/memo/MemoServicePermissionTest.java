package com.healthlog.demo.service.memo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthlog.demo.entity.Memo;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.MemoRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

class MemoServicePermissionTest {

    private static final Long PROFILE_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final Long LOG_ID = 30L;

    @Test
    @DisplayName("VIEWER không thể sửa memo qua service")
    void viewerCannotUpdateMemo() {
        MemoRepository repository = mock(MemoRepository.class);
        ProfileAccessValidation accessValidation = mock(ProfileAccessValidation.class);
        MemoServiceImpl service = new MemoServiceImpl(repository, accessValidation);
        when(accessValidation.validateAndGetEditableProfile(PROFILE_ID, USER_ID, Category.memo))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN, "viewer"));

        assertThrows(BusinessException.class, () -> service.update(PROFILE_ID, USER_ID, LOG_ID, new Memo()));

        verify(repository, never()).findById(LOG_ID);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Memo.class));
    }
}
