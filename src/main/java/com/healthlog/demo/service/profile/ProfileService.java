package com.healthlog.demo.service.profile;

import java.util.List;
import jakarta.servlet.http.HttpSession;

import com.healthlog.demo.dto.profile.ProfileFormDto;
import com.healthlog.demo.entity.Profile;

public interface ProfileService {

    List<Profile> getProfiles(Long userId);

    Profile getProfile(Long userId, Long profileId);

    ProfileFormDto getProfileFormDto(Long userId, Long profileId);

    boolean hasAnyProfile(Long userId);

    Profile create(Long userId, ProfileFormDto formDto);

    Profile update(Long userId, ProfileFormDto formDto);

    void delete(Long userId, Long profileId, HttpSession session);

    void switchProfile(HttpSession session, Long userId, Long profileId);

    Profile resolveCurrentProfile(HttpSession session, Long userId);
}