package com.healthlog.demo.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ProfileShareSettingDto {

    public enum ShareRole {
        EDITOR, // 編集者
        VIEWER, // 閲覧者
        NONE    // 閲覧不可
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRole {
        private ShareRole weightRole = ShareRole.NONE;
        private ShareRole sleepRole = ShareRole.NONE;
        private ShareRole waterRole = ShareRole.NONE;
        private ShareRole stepRole = ShareRole.NONE;
        private ShareRole memoRole = ShareRole.NONE;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long targetProfileId;
        private String targetProfileName;
        private String relationship;
        private CategoryRole roles = new CategoryRole();
    }
}