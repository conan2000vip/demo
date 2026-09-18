package com.healthlog.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profiles", indexes = { @Index(name = "idx_profiles_user_id", columnList = "user_id"),
		@Index(name = "idx_profiles_user_primary", columnList = "user_id, is_primary") })
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // varchar(20)。具体的な値は業務要件による（本人/配偶者/子供など）。
    // 値が確定したらenumに変更可能。
    @Column(nullable = false, length = 20)
    private String relationship;

    @Column(length = 10)
    private String gender;

    @Column(precision = 5, scale = 1)
    private BigDecimal height;

    @Column(name = "target_weight", precision = 5, scale = 1)
    private BigDecimal targetWeight;

    @Column(name = "water_goal_ml")
    private Integer waterGoalMl;

    @Column(name = "step_goal")
    private Integer stepGoal;

    @Column(length = 20)
    private String avatar = "avatar_01";

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "daily_sleep_goal", precision = 3, scale = 1)
    private BigDecimal dailySleepGoal;

    // // ===== 1対多の関連: 1つのProfileが複数種類のログを持つ =====
    // @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Memo> memoLogs = new ArrayList<>();

    // @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Sleep> sleepLogs = new ArrayList<>();

    // @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Step> stepLogs = new ArrayList<>();

    // @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Water> waterLogs = new ArrayList<>();

    // @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Weight> weightLogs = new ArrayList<>();

    public Profile(User user, String name, String relationship) {
        this.user = user;
        this.name = name;
        this.relationship = relationship;
    }
}