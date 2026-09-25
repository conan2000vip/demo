package com.healthlog.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthlog.demo.entity.ProfileShareSetting;
import com.healthlog.demo.entity.ProfileShareSetting.Category;

public interface ProfileShareSettingRepository extends JpaRepository<ProfileShareSetting, Long> {

    List<ProfileShareSetting> findByOwnerProfile_Id(Long ownerProfileId);

    List<ProfileShareSetting> findByOwnerProfile_IdAndViewerProfile_Id(Long ownerProfileId, Long viewerProfileId);

    Optional<ProfileShareSetting> findByOwnerProfile_IdAndViewerProfile_IdAndCategory(
            Long ownerProfileId, Long viewerProfileId, Category category);

    void deleteByOwnerProfile_IdAndViewerProfile_Id(Long ownerProfileId, Long viewerProfileId);
}