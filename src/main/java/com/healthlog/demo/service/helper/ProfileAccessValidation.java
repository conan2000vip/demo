package com.healthlog.demo.service.helper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.ProfileRepository;

@Component
public class ProfileAccessValidation {

    private final ProfileRepository profileRepository;

    public ProfileAccessValidation(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // ログイン中のユーザーに属するProfileか確認して取得する。
    public Profile validateAndGetProfile(Long profileId, Long currentUserId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "データが見つかりません"));

        if (!profile.getUser().getId().equals(currentUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "アクセス権限がありません");
        }

        return profile;
    }
}