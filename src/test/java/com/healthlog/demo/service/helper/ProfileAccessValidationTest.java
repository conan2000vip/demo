package com.healthlog.demo.service.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.ProfileShareSetting.Role;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.ProfileShareSettingRepository;

@ExtendWith(MockitoExtension.class)
class ProfileAccessValidationTest {

    private static final Long USER_ID = 100L;
    private static final Long OWNER_ID = 10L;
    private static final Long VIEWER_ID = 20L;

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileShareSettingRepository shareSettingRepository;
    @Mock
    private AuthenticatedProfileContext authenticatedProfileContext;

    private ProfileAccessValidation validation;
    private Profile owner;
    private Profile viewer;

    @BeforeEach
    void setUp() {
        validation = new ProfileAccessValidation(profileRepository, shareSettingRepository,
                authenticatedProfileContext);
        owner = profile(OWNER_ID, false, false);
        viewer = profile(VIEWER_ID, false, false);

        when(profileRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(profileRepository.findByIdAndUser_Id(VIEWER_ID, USER_ID)).thenReturn(Optional.of(viewer));
        when(authenticatedProfileContext.getProfileId()).thenReturn(VIEWER_ID);
    }

    @Test
    @DisplayName("EDITOR được xem và sửa mọi category đã chia sẻ")
    void editorCanReadAndEditEveryCategory() {
        givenRole(Role.editor);

        for (Category category : Category.values()) {
            assertTrue(validation.hasReadAccess(OWNER_ID, USER_ID, category), category.name());
            assertTrue(validation.hasEditAccess(OWNER_ID, USER_ID, category), category.name());
        }
    }

    @Test
    @DisplayName("VIEWER chỉ được xem, không được sửa mọi category đã chia sẻ")
    void viewerCanReadButCannotEditAnyCategory() {
        givenRole(Role.viewer);

        for (Category category : Category.values()) {
            assertTrue(validation.hasReadAccess(OWNER_ID, USER_ID, category), category.name());
            assertFalse(validation.hasEditAccess(OWNER_ID, USER_ID, category), category.name());
        }
    }

    @Test
    @DisplayName("NONE không được xem hoặc sửa category đã chia sẻ")
    void noneCannotReadOrEditAnyCategory() {
        givenRole(Role.none);

        for (Category category : Category.values()) {
            assertFalse(validation.hasReadAccess(OWNER_ID, USER_ID, category), category.name());
            assertFalse(validation.hasEditAccess(OWNER_ID, USER_ID, category), category.name());
        }
    }

    @Test
    @DisplayName("Chưa có setting thì mặc định là VIEWER")
    void missingSettingDefaultsToViewer() {
        when(shareSettingRepository.findByOwnerProfile_IdAndViewerProfile_IdAndCategory(
                eq(OWNER_ID), eq(VIEWER_ID), any(Category.class))).thenReturn(Optional.empty());

        assertTrue(validation.hasReadAccess(OWNER_ID, USER_ID, Category.weight));
        assertFalse(validation.hasEditAccess(OWNER_ID, USER_ID, Category.weight));
    }

    @Test
    @DisplayName("Primary được sửa profile do primary quản lý bất kể setting chia sẻ")
    void primaryCanEditManagedProfile() {
        Profile primary = profile(VIEWER_ID, true, true);
        owner.setManagedByPrimary(true);
        when(profileRepository.findByIdAndUser_Id(VIEWER_ID, USER_ID)).thenReturn(Optional.of(primary));

        assertTrue(validation.hasReadAccess(OWNER_ID, USER_ID, Category.weight));
        assertTrue(validation.hasEditAccess(OWNER_ID, USER_ID, Category.weight));
        verify(shareSettingRepository, never()).findByOwnerProfile_IdAndViewerProfile_IdAndCategory(
                OWNER_ID, VIEWER_ID, Category.weight);
    }

    private void givenRole(Role role) {
        when(shareSettingRepository.findByOwnerProfile_IdAndViewerProfile_IdAndCategory(
                eq(OWNER_ID), eq(VIEWER_ID), any(Category.class)))
                .thenAnswer(invocation -> Optional.of(new ProfileShareSetting(
                        owner, viewer, invocation.getArgument(2), role)));
    }

    private static Profile profile(Long id, boolean primary, boolean managedByPrimary) {
        User user = new User();
        user.setId(USER_ID);
        Profile profile = new Profile(user, "Profile " + id, "family");
        profile.setId(id);
        profile.setPrimary(primary);
        profile.setManagedByPrimary(managedByPrimary);
        return profile;
    }
}
