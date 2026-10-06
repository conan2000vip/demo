package com.healthlog.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthlog.demo.dto.profile.ProfileShareSettingDto;
import com.healthlog.demo.dto.profile.PinSettingRequest;
import com.healthlog.demo.entity.Profile;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.constant.SessionConstants;
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
    public ResponseEntity<Object> getShareSettings(
            @PathVariable("id") Long activeProfileId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("LOGIN_USER");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        ResponseEntity<Object> permissionError = validateShareSettingsPermission(
            currentUser.getId(), activeProfileId, session);
        if (permissionError != null) return permissionError;

        List<ProfileShareSettingDto.Item> settings = profileShareService.getShareSettings(currentUser.getId(), activeProfileId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(settings);
    }

    // 共有設定の一覧を受け取り、対象プロファイルの設定を一括登録または更新する。
    @PostMapping
    public ResponseEntity<Object> updateShareSettings(
            @PathVariable("id") Long activeProfileId,
            @RequestBody List<ProfileShareSettingDto.Item> items,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("LOGIN_USER");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        ResponseEntity<Object> permissionError = validateShareSettingsPermission(
                currentUser.getId(), activeProfileId, session);
        if (permissionError != null) return permissionError;

        profileShareService.updateShareSettings(currentUser.getId(), activeProfileId, items);
        List<ProfileShareSettingDto.Item> savedSettings = profileShareService
                .getShareSettings(currentUser.getId(), activeProfileId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(savedSettings);
    }

    @GetMapping("/pin")
    public ResponseEntity<Object> getPinStatus(@PathVariable("id") Long profileId, HttpSession session) {
        User currentUser = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        ResponseEntity<Object> permissionError = validateShareSettingsPermission(
                currentUser.getId(), profileId, session);
        if (permissionError != null) return permissionError;
        return ResponseEntity.ok(Map.of("enabled", profileService.hasPin(currentUser.getId(), profileId)));
    }

    @PostMapping("/pin")
    public ResponseEntity<Object> updatePin(@PathVariable("id") Long profileId,
            @RequestBody PinSettingRequest request, HttpSession session) {
        User currentUser = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        ResponseEntity<Object> permissionError = validateShareSettingsPermission(
                currentUser.getId(), profileId, session);
        if (permissionError != null) return permissionError;
        try {
            profileService.updatePin(currentUser.getId(), profileId, request.getAction(),
                    request.getCurrentPin(), request.getNewPin(), request.getConfirmation());
            return ResponseEntity.ok(Map.of("enabled", profileService.hasPin(currentUser.getId(), profileId)));
        } catch (com.healthlog.demo.exception.BusinessException e) {
            return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
        }
    }

    private ResponseEntity<Object> validateShareSettingsPermission(
            Long userId, Long requestedProfileId, HttpSession session) {
        Long authenticatedProfileId = (Long) session.getAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID);
        if (authenticatedProfileId == null) {
            Profile selectedProfile = profileService.resolveCurrentProfile(session, userId);
            if (selectedProfile != null && selectedProfile.isPrimary()) {
                authenticatedProfileId = selectedProfile.getId();
                session.setAttribute(SessionConstants.AUTHENTICATED_PROFILE_ID, authenticatedProfileId);
            }
        }
        if (authenticatedProfileId == null || !authenticatedProfileId.equals(requestedProfileId)) {
            if (authenticatedProfileId == null) {
                return ResponseEntity.status(403).body(Map.of(
                        "message", "認証プロファイルが見つかりません。"));
            }
            Profile authenticatedProfile = profileService.getProfile(userId, authenticatedProfileId);
            Profile requestedProfile = profileService.getProfile(userId, requestedProfileId);
            Profile currentProfile = profileService.resolveCurrentProfile(session, userId);
            boolean canManageManagedProfile = currentProfile != null
                    && currentProfile.getId().equals(authenticatedProfile.getId())
                    && authenticatedProfile.isPrimary()
                    && requestedProfile.isManagedByPrimary();
            if (!canManageManagedProfile) {
                return ResponseEntity.status(403).body(Map.of(
                        "message", "このプロファイルの共有設定を変更する権限がありません。"));
            }
        }
        return null;
    }
}