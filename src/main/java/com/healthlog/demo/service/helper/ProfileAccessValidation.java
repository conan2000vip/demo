package com.healthlog.demo.service.helper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.ProfileShareSetting.Category;
import com.healthlog.demo.entity.ProfileShareSetting.Role;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.ProfileShareSettingRepository;

@Component
public class ProfileAccessValidation {

    private final ProfileRepository profileRepository;
    private final ProfileShareSettingRepository shareSettingRepository;
    private final AuthenticatedProfileContext authenticatedProfileContext;

    public ProfileAccessValidation(ProfileRepository profileRepository,
            ProfileShareSettingRepository shareSettingRepository,
            AuthenticatedProfileContext authenticatedProfileContext) {
        this.profileRepository = profileRepository;
        this.shareSettingRepository = shareSettingRepository;
        this.authenticatedProfileContext = authenticatedProfileContext;
    }

    /**
     * Validates read access using the account's primary profile as the
     * authenticated actor. The profile in the URL is only the viewed profile.
     */
    public Profile validateAndGetProfile(Long profileId, Long currentUserId, Category category) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "データが見つかりません"));

        if (!profile.getUser().getId().equals(currentUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "アクセス権限がありません");
        }

        Long authenticatedProfileId = resolveAuthenticatedProfileId(currentUserId);
        if (profile.getId().equals(authenticatedProfileId)) {
            return profile;
        }

        Profile authenticatedProfile = profileRepository.findByIdAndUser_Id(authenticatedProfileId, currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "認証プロファイルが見つかりません"));
        if (authenticatedProfile.isPrimary() && profile.isManagedByPrimary()) {
            return profile;
        }

        Role role = shareSettingRepository
                .findByOwnerProfile_IdAndViewerProfile_IdAndCategory(profile.getId(), authenticatedProfileId,
                        category)
                .map(setting -> setting.getRole())
                .orElse(Role.viewer);
        if (role == Role.none) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "このカテゴリは共有されていません");
        }
        return profile;
    }

    public Profile validateAndGetEditableProfile(Long profileId, Long currentUserId, Category category) {
        Profile profile = validateAndGetProfile(profileId, currentUserId, category);
        Long authenticatedProfileId = resolveAuthenticatedProfileId(currentUserId);
        if (profile.getId().equals(authenticatedProfileId)) {
            return profile;
        }

        Profile authenticatedProfile = profileRepository.findByIdAndUser_Id(authenticatedProfileId, currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "認証プロファイルが見つかりません"));
        if (authenticatedProfile.isPrimary() && profile.isManagedByPrimary()) {
            return profile;
        }

        Role role = shareSettingRepository
                .findByOwnerProfile_IdAndViewerProfile_IdAndCategory(profile.getId(), authenticatedProfileId,
                        category)
                .map(setting -> setting.getRole())
                .orElse(Role.viewer);
        if (role != Role.editor) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "このカテゴリを編集する権限がありません");
        }
        return profile;
    }

    public Profile validateCanViewProfile(Long profileId, Long currentUserId) {
        return profileRepository.findByIdAndUser_Id(profileId, currentUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "プロファイルが見つかりません"));
    }

    public boolean hasReadAccess(Long profileId, Long currentUserId, Category category) {
        try {
            validateAndGetProfile(profileId, currentUserId, category);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private Long resolveAuthenticatedProfileId(Long currentUserId) {
        Long profileId = authenticatedProfileContext.getProfileId();
        if (profileId == null || !profileRepository.findByIdAndUser_Id(profileId, currentUserId).isPresent()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "認証プロファイルが見つかりません");
        }
        return profileId;
    }
}