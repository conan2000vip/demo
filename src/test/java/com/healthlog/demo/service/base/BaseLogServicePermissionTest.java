package com.healthlog.demo.service.base;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.Step;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.StepRepository;
import com.healthlog.demo.service.helper.ProfileAccessValidation;

class BaseLogServicePermissionTest {

    private static final Long PROFILE_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final Long LOG_ID = 30L;

    @Test
    @DisplayName("VIEWER không thể xóa log qua service")
    void viewerCannotDeleteLog() {
        StepRepository repository = mock(StepRepository.class);
        ProfileAccessValidation accessValidation = mock(ProfileAccessValidation.class);
        BaseLogService<Step, Step> service = new BaseLogService<>(repository, accessValidation) {
            @Override
            protected Step mapToDto(Step entity) {
                return entity;
            }

            @Override
            protected Category category() {
                return Category.step;
            }
        };
        when(accessValidation.validateAndGetEditableProfile(PROFILE_ID, USER_ID, Category.step))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.FORBIDDEN, "viewer"));

        assertThrows(BusinessException.class, () -> service.deleteLog(LOG_ID, PROFILE_ID, USER_ID));

        verify(repository, never()).findById(LOG_ID);
        verify(repository, never()).deleteByIdAndProfile_Id(LOG_ID, PROFILE_ID);
    }
}
