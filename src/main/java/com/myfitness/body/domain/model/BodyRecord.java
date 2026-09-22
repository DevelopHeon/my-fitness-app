package com.myfitness.body.domain.model;

import com.myfitness.body.domain.exception.BodyRecordRuleException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "body_records")
public class BodyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "body_fat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal bodyFatPercentage;

    @Column(name = "skeletal_muscle_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal skeletalMuscleKg;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BodyRecord() {}

    private BodyRecord(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo,
            Instant now) {
        validateUserId(userId);
        validateValues(
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                now);
        this.userId = userId;
        this.weightKg = weightKg;
        this.bodyFatPercentage = bodyFatPercentage;
        this.skeletalMuscleKg = skeletalMuscleKg;
        this.measuredAt = measuredAt;
        this.memo = normalize(memo);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static BodyRecord create(
            Long userId,
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo,
            Instant now) {
        return new BodyRecord(
                userId,
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                memo,
                now);
    }

    public void update(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            String memo,
            Instant now) {
        validateValues(
                weightKg,
                bodyFatPercentage,
                skeletalMuscleKg,
                measuredAt,
                now);
        this.weightKg = weightKg;
        this.bodyFatPercentage = bodyFatPercentage;
        this.skeletalMuscleKg = skeletalMuscleKg;
        this.measuredAt = measuredAt;
        this.memo = normalize(memo);
        this.updatedAt = now;
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BodyRecordRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }

    private static void validateValues(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            Instant now) {
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new BodyRecordRuleException("체중은 0보다 커야 합니다.");
        }
        if (bodyFatPercentage == null
                || bodyFatPercentage.signum() < 0
                || bodyFatPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new BodyRecordRuleException("체지방률은 0 이상 100 이하이어야 합니다.");
        }
        if (skeletalMuscleKg == null || skeletalMuscleKg.signum() <= 0) {
            throw new BodyRecordRuleException("골격근량은 0보다 커야 합니다.");
        }
        if (measuredAt == null) {
            throw new BodyRecordRuleException("측정 일시는 필수입니다.");
        }
        if (measuredAt.isAfter(now)) {
            throw new BodyRecordRuleException("미래 시각으로 신체 기록을 등록할 수 없습니다.");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getBodyFatPercentage() { return bodyFatPercentage; }
    public BigDecimal getSkeletalMuscleKg() { return skeletalMuscleKg; }
    public Instant getMeasuredAt() { return measuredAt; }
    public String getMemo() { return memo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
