package com.healthlog.demo.service.profile;

import java.util.List;
import com.healthlog.demo.dto.profile.ProfileShareSettingDto;

public interface ProfileShareService {
    
    List<ProfileShareSettingDto.Item> getShareSettings(Long currentUserId, Long activeProfileId);

    void updateShareSettings(Long currentUserId, Long activeProfileId, List<ProfileShareSettingDto.Item> items);
}