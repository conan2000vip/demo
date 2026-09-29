package com.healthlog.demo.service.profile;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.dto.profile.ProfileShareSettingDto;
import com.healthlog.demo.dto.profile.ProfileShareSettingDto.ShareRole;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.ProfileShareSetting.Role;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.ProfileShareSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileShareServiceImpl implements ProfileShareService {

    private final ProfileRepository profileRepository;
    private final ProfileShareSettingRepository profileShareSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProfileShareSettingDto.Item> getShareSettings(Long currentUserId, Long activeProfileId) {
        Profile activeProfile = validateProfileOwnership(currentUserId, activeProfileId);
        List<Profile> familyProfiles = profileRepository.findByUser_Id(currentUserId);
        List<ProfileShareSetting> existingSettings = profileShareSettingRepository.findByOwnerProfile_Id(activeProfile.getId());

        return familyProfiles.stream().map(target -> {
            ProfileShareSettingDto.Item item = new ProfileShareSettingDto.Item();
            item.setTargetProfileId(target.getId());
            item.setTargetProfileName(target.getName());
            item.setRelationship(target.getRelationship());
            item.setPrimary(target.isPrimary());
            item.setSelf(Objects.equals(target.getId(), activeProfileId));

            // データ5項目の共有権限をDBからDTOへ設定し、未設定の場合はVIEWERを使用する。
            ProfileShareSettingDto.CategoryRole roles = new ProfileShareSettingDto.CategoryRole();
            roles.setWeightRole(getRoleForCategory(existingSettings, target.getId(), Category.weight));
            roles.setSleepRole(getRoleForCategory(existingSettings, target.getId(), Category.sleep));
            roles.setWaterRole(getRoleForCategory(existingSettings, target.getId(), Category.water));
            roles.setStepRole(getRoleForCategory(existingSettings, target.getId(), Category.step));
            roles.setMemoRole(getRoleForCategory(existingSettings, target.getId(), Category.memo));
            item.setRoles(roles);
            return item;
        }).toList();
    }

    @Override
    @Transactional
    @SuppressWarnings("rawtypes")
    public void updateShareSettings(Long currentUserId, Long activeProfileId, List items) {
        Profile activeProfile = validateProfileOwnership(currentUserId, activeProfileId);

        for (Object value : items) {
            ProfileShareSettingDto.Item item = (ProfileShareSettingDto.Item) value;
            // 2. 自分自身への共有を禁止する（制約に対応）。
            if (Objects.equals(activeProfile.getId(), item.getTargetProfileId())) {
                continue;
            }

            Profile viewerProfile = profileRepository.findByIdAndUser_Id(item.getTargetProfileId(), currentUserId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "アクセス権限がありません"));
            ProfileShareSettingDto.CategoryRole roles = item.getRoles();

            // 5つのデータ項目について共有設定を一括登録または更新する。
            saveOrUpdateCategory(activeProfile, viewerProfile, Category.weight, roles.getWeightRole());
            saveOrUpdateCategory(activeProfile, viewerProfile, Category.sleep, roles.getSleepRole());
            saveOrUpdateCategory(activeProfile, viewerProfile, Category.water, roles.getWaterRole());
            saveOrUpdateCategory(activeProfile, viewerProfile, Category.step, roles.getStepRole());
            saveOrUpdateCategory(activeProfile, viewerProfile, Category.memo, roles.getMemoRole());
        }
    }

    // 共有設定の検証と変換を行う補助メソッド群。
    private Profile validateProfileOwnership(Long currentUserId, Long profileId) {
        return profileRepository.findByIdAndUser_Id(profileId, currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "他アカウントのプロファイルへのアクセスは拒否されました"));
    }

    private ShareRole getRoleForCategory(List<ProfileShareSetting> settings, Long viewerProfileId, Category category) {
        return settings.stream()
                .filter(s -> Objects.equals(s.getViewerProfile().getId(), viewerProfileId) && s.getCategory() == category)
                .findFirst()
                .map(s -> mapToDtoRole(s.getRole()))
                .orElse(ShareRole.VIEWER); // 未設定の場合はVIEWERを使用する。
    }

    private void saveOrUpdateCategory(Profile owner, Profile viewer, Category category, ShareRole dtoRole) {
        Role entityRole = mapToEntityRole(dtoRole);

        ProfileShareSetting setting = profileShareSettingRepository
                .findByOwnerProfile_IdAndViewerProfile_IdAndCategory(owner.getId(), viewer.getId(), category)
                .orElseGet(() -> new ProfileShareSetting(owner, viewer, category, entityRole));

        setting.setRole(entityRole);
        profileShareSettingRepository.save(setting);
    }

    private ShareRole mapToDtoRole(Role entityRole) {
        if (entityRole == null) return ShareRole.VIEWER;
        return switch (entityRole) {
            case editor -> ShareRole.EDITOR;
            case none -> ShareRole.NONE;
            default -> ShareRole.VIEWER;
        };
    }

    private Role mapToEntityRole(ShareRole dtoRole) {
        if (dtoRole == null) return Role.viewer;
        return switch (dtoRole) {
            case EDITOR -> Role.editor;
            case NONE -> Role.none;
            default -> Role.viewer;
        };
    }
}