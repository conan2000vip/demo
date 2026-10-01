package com.healthlog.demo.service.home;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.healthlog.demo.entity.Profile;

public interface HomeFamilyService {
    List<Map<String, Object>> buildFamilyMembers(List<Profile> profiles, Long currentUserId, LocalDate today);

    Map<String, Object> buildFamilySummary(List<Map<String, Object>> familyMembers);
}