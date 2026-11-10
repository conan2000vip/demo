package com.healthlog.demo.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthlog.demo.dto.profile.ProfileShareSettingDto.CategoryRole;
import com.healthlog.demo.dto.profile.ProfileShareSettingDto.Item;
import com.healthlog.demo.dto.profile.ProfileShareSettingDto.ShareRole;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.ProfileShareSetting.Role;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.ProfileShareSettingRepository;

@ExtendWith(MockitoExtension.class)
class ProfileShareServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final Long OWNER_ID = 10L;
    private static final Long VIEWER_ID = 20L;

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileShareSettingRepository shareSettingRepository;

    private ProfileShareServiceImpl service;
    private Profile owner;
    private Profile viewer;

    @BeforeEach
    void setUp() {
        service = new ProfileShareServiceImpl(profileRepository, shareSettingRepository);
        owner = profile(OWNER_ID);
        viewer = profile(VIEWER_ID);

        when(profileRepository.findByIdAndUser_Id(OWNER_ID, USER_ID)).thenReturn(Optional.of(owner));
        when(profileRepository.findByIdAndUser_Id(VIEWER_ID, USER_ID)).thenReturn(Optional.of(viewer));
    }

    @Test
    @DisplayName("共有設定で EDITOR/VIEWER/NONE をカテゴリごとに保存する")
    void updateSettingsMapsAllRolesToCategories() {
        when(shareSettingRepository.findByOwnerProfile_IdAndViewerProfile_IdAndCategory(
                eq(OWNER_ID), eq(VIEWER_ID), any(Category.class))).thenReturn(Optional.empty());
        CategoryRole roles = new CategoryRole(
                ShareRole.EDITOR, ShareRole.VIEWER, ShareRole.NONE, ShareRole.EDITOR, ShareRole.NONE);
        Item item = new Item(VIEWER_ID, null, null, roles);

        service.updateShareSettings(USER_ID, OWNER_ID, List.of(item));

        ArgumentCaptor<ProfileShareSetting> captor = ArgumentCaptor.forClass(ProfileShareSetting.class);
        verify(shareSettingRepository, times(5)).saveAndFlush(captor.capture());
        List<ProfileShareSetting> saved = captor.getAllValues();

        assertRole(saved, Category.weight, Role.editor);
        assertRole(saved, Category.sleep, Role.viewer);
        assertRole(saved, Category.water, Role.none);
        assertRole(saved, Category.step, Role.editor);
        assertRole(saved, Category.memo, Role.none);
    }

    private static void assertRole(List<ProfileShareSetting> settings, Category category, Role role) {
        assertEquals(role, settings.stream()
                .filter(setting -> setting.getCategory() == category)
                .findFirst()
                .orElseThrow()
                .getRole());
    }

    private static Profile profile(Long id) {
        User user = new User();
        user.setId(USER_ID);
        Profile profile = new Profile(user, "Profile " + id, "family");
        profile.setId(id);
        return profile;
    }
}
