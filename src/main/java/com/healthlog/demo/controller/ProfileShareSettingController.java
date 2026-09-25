package com.healthlog.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthlog.demo.dto.profile.ProfileShareSettingDto;
import com.healthlog.demo.entity.User;
import com.healthlog.demo.service.profile.ProfileShareService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/profile/{id}/share-settings")
@RequiredArgsConstructor
public class ProfileShareSettingController {

    private final ProfileShareService profileShareService;

    // 共有設定の一覧を取得し、ログインユーザーが所有するプロファイルの設定を返す。
    @GetMapping
    public ResponseEntity<List<ProfileShareSettingDto.Item>> getShareSettings(
            @PathVariable("id") Long activeProfileId,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("LOGIN_USER");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        List<ProfileShareSettingDto.Item> settings = profileShareService.getShareSettings(currentUser.getId(), activeProfileId);
        return ResponseEntity.ok(settings);
    }

    // 共有設定の一覧を受け取り、対象プロファイルの設定を一括登録または更新する。
    @PostMapping
    public ResponseEntity<Void> updateShareSettings(
            @PathVariable("id") Long activeProfileId,
            @RequestBody List<ProfileShareSettingDto.Item> items,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("LOGIN_USER");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        profileShareService.updateShareSettings(currentUser.getId(), activeProfileId, items);
        return ResponseEntity.ok().build();
    }
}