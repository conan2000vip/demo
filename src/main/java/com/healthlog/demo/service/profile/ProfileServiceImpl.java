package com.healthlog.demo.service.profile;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthlog.demo.constant.SessionConstants;
import com.healthlog.demo.dto.profile.ProfileFormDto;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.exception.BusinessException;
import com.healthlog.demo.repository.ProfileRepository;
import com.healthlog.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int MAX_PROFILES = 10;
    private static final List<String> UNIQUE_RELATIONSHIPS = List.of("父", "母", "配偶者", "祖父", "祖母");

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Profile> getProfiles(Long userId) {
        return profileRepository.findByUser_Id(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Profile getProfile(Long userId, Long profileId) {
        return profileRepository.findByIdAndUser_Id(profileId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "プロファイルが見つかりません"));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileFormDto getProfileFormDto(Long userId, Long profileId) {
        Profile profile = getProfile(userId, profileId);
        ProfileFormDto dto = new ProfileFormDto();
        dto.setId(profile.getId());
        dto.setName(profile.getName());
        dto.setBirthDate(profile.getBirthDate());
        dto.setRelationship(profile.getRelationship());
        dto.setGender(profile.getGender());
        dto.setHeight(profile.getHeight());
        dto.setTargetWeight(profile.getTargetWeight());
        dto.setWaterGoalMl(profile.getWaterGoalMl());
        dto.setStepGoal(profile.getStepGoal());
        dto.setDailySleepGoal(profile.getDailySleepGoal());
        dto.setAvatar(profile.getAvatar());
        dto.setPrimary(profile.isPrimary());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyProfile(Long userId) {
        return profileRepository.existsByUser_Id(userId);
    }

    @Override
    @Transactional
    public Profile create(Long userId, ProfileFormDto dto) {
        long count = profileRepository.countByUser_Id(userId);

        if (count >= MAX_PROFILES) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "プロファイルは最大" + MAX_PROFILES + "件までです");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setName(dto.getName());
        profile.setBirthDate(dto.getBirthDate());
        profile.setGender(dto.getGender());
        profile.setHeight(dto.getHeight());
        profile.setTargetWeight(dto.getTargetWeight());
        profile.setWaterGoalMl(dto.getWaterGoalMl());
        profile.setStepGoal(dto.getStepGoal());
        profile.setDailySleepGoal(dto.getDailySleepGoal());
        profile.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : "avatar_01");

        if (count == 0) {
            profile.setPrimary(true);
            profile.setRelationship("本人");
        } else {
            profile.setPrimary(false);
            if (dto.getRelationship() == null || dto.getRelationship().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "続柄を選択してください");
            }
            profile.setRelationship(dto.getRelationship());

            if (UNIQUE_RELATIONSHIPS.contains(dto.getRelationship()) && profileRepository
                    .existsByUser_IdAndRelationship(userId, dto.getRelationship())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "「" + dto.getRelationship() + "」はすでに登録されています");
            }
        }

        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public Profile update(Long userId, ProfileFormDto dto) {
        Profile dbProfile = getProfile(userId, dto.getId());

        if (dbProfile.isPrimary()) {
            dto.setRelationship(dbProfile.getRelationship());
        } else {
            if (dto.getRelationship() == null || dto.getRelationship().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "続柄を選択してください");
            }
            if (UNIQUE_RELATIONSHIPS.contains(dto.getRelationship())
                    && !dto.getRelationship().equals(dbProfile.getRelationship())
                    && profileRepository.existsByUser_IdAndRelationship(userId, dto.getRelationship())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "「" + dto.getRelationship() + "」はすでに登録されています");
            }
            dbProfile.setRelationship(dto.getRelationship());
        }

        dbProfile.setName(dto.getName());
        dbProfile.setBirthDate(dto.getBirthDate());
        dbProfile.setGender(dto.getGender());
        dbProfile.setHeight(dto.getHeight());
        dbProfile.setAvatar(dto.getAvatar());
        dbProfile.setTargetWeight(dto.getTargetWeight());
        dbProfile.setWaterGoalMl(dto.getWaterGoalMl());
        dbProfile.setStepGoal(dto.getStepGoal());
        dbProfile.setDailySleepGoal(dto.getDailySleepGoal());

        return profileRepository.save(dbProfile);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long profileId, HttpSession session) {
        Profile profile = getProfile(userId, profileId);

        if (profile.isPrimary()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "本人のプロファイルは削除できません");
        }

        long count = profileRepository.countByUser_Id(userId);
        if (count <= 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "最後のプロファイルは削除できません");
        }

        profileRepository.delete(profile);

        Long currentId = (Long) session.getAttribute(SessionConstants.CURRENT_PROFILE_ID);
        if (currentId != null && currentId.equals(profileId)) {
            profileRepository.findByUser_IdAndIsPrimaryTrue(userId)
                    .ifPresent(primary -> session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, primary.getId()));
        }
    }

    @Override
    @Transactional
    public void switchProfile(HttpSession session, Long userId, Long profileId) {
        Profile profile = getProfile(userId, profileId);
        session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, profile.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Profile resolveCurrentProfile(HttpSession session, Long userId) {
        Long currentId = (Long) session.getAttribute(SessionConstants.CURRENT_PROFILE_ID);

        if (currentId != null) {
            Optional<Profile> profile = profileRepository.findByIdAndUser_Id(currentId, userId);
            if (profile.isPresent()) {
                return profile.get();
            }
        }

        Optional<Profile> primary = profileRepository.findByUser_IdAndIsPrimaryTrue(userId);
        primary.ifPresent(profile -> session.setAttribute(SessionConstants.CURRENT_PROFILE_ID, profile.getId()));
        return primary.orElse(null);
    }
}