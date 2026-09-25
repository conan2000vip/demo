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
        private ShareRole weightRole = ShareRole.VIEWER;
        private ShareRole sleepRole = ShareRole.VIEWER;
        private ShareRole waterRole = ShareRole.VIEWER;
        private ShareRole stepRole = ShareRole.VIEWER;
        private ShareRole memoRole = ShareRole.VIEWER;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long targetProfileId;
        private String targetProfileName;
        private String relationship;
        private boolean isPrimary;
        private boolean isSelf;
        private CategoryRole roles = new CategoryRole();
    }
}