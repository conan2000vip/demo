package com.healthlog.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthlog.demo.dto.profile.ProfileShareSettingDto;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.service.profile.ProfileShareService;
import com.healthlog.demo.service.profile.ProfileService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile/{id}/share-settings")
@RequiredArgsConstructor
public class ProfileShareSettingController {

    private final ProfileShareService profileShareService;
    private final ProfileService profileService;

    // 共有設定の一覧を取得し、ログインユーザーが所有するプロファイルの設定を返す。
    @GetMapping
    public ResponseEntity<?> getShareSettings(
            @PathVariable("id") Long activeProfileId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("LOGIN_USER");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        ResponseEntity<Map<String, String>> permissionError = validateShareSettingsPermission(
            currentUser.getId(), activeProfileId, session);
        if (permissionError != null) return permissionError;

        List<ProfileShareSettingDto.Item> settings = profileShareService.getShareSettings(currentUser.getId(), activeProfileId);
        return ResponseEntity.ok(settings);
    }

    // 共有設定の一覧を受け取り、対象プロファイルの設定を一括登録または更新する。
    @PostMapping
    public ResponseEntity<?> updateShareSettings(
            @PathVariable("id") Long activeProfileId,
            @RequestBody List<ProfileShareSettingDto.Item> items,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("LOGIN_USER");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        ResponseEntity<Map<String, String>> permissionError = validateShareSettingsPermission(
                currentUser.getId(), activeProfileId, session);
        if (permissionError != null) return permissionError;

        profileShareService.updateShareSettings(currentUser.getId(), activeProfileId, items);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Map<String, String>> validateShareSettingsPermission(
            Long userId, Long requestedProfileId, HttpSession session) {
        Profile selectedProfile = profileService.resolveCurrentProfile(session, userId);
        if (selectedProfile == null
                || (!selectedProfile.isPrimary() && !selectedProfile.getId().equals(requestedProfileId))) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "このプロファイルの共有設定を変更する権限がありません。自分のプロファイルを選択してください。"));
        }
        return null;
    }
}