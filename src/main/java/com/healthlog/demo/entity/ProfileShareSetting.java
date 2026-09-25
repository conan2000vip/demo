package com.healthlog.demo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profile_share_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_owner_viewer_category", columnNames = { "owner_profile_id", "viewer_profile_id",
                "category" })
}, indexes = {
        @Index(name = "idx_owner_profile", columnList = "owner_profile_id"),
        @Index(name = "idx_viewer_profile", columnList = "viewer_profile_id")
})
public class ProfileShareSetting {
    public enum Category {weight,sleep,water,step,memo}
    public enum Role {none,viewer,editor}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_profile_id", nullable = false)
    private Profile ownerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_profile_id", nullable = false)
    private Profile viewerProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.viewer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProfileShareSetting(Profile ownerProfile, Profile viewerProfile, Category category, Role role) {
        this.ownerProfile = ownerProfile;
        this.viewerProfile = viewerProfile;
        this.category = category;
        this.role = role;
    }
}