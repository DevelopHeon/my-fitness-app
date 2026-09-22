package com.myfitness.nutrition.domain.model;

import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "foods",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_foods_user_name",
                columnNames = {"user_id", "name"}))
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "serving_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal servingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "serving_unit", nullable = false, length = 20)
    private ServingUnit servingUnit;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(name = "carbohydrate_grams", nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrateGrams;

    @Column(name = "protein_grams", nullable = false, precision = 8, scale = 2)
    private BigDecimal proteinGrams;

    @Column(name = "fat_grams", nullable = false, precision = 8, scale = 2)
    private BigDecimal fatGrams;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Food() {}

    private Food(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            Instant now) {
        validateUserId(userId);
        validate(
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams);
        this.userId = userId;
        this.name = normalizeName(name);
        this.servingAmount = servingAmount;
        this.servingUnit = servingUnit;
        this.calories = calories;
        this.carbohydrateGrams = carbohydrateGrams;
        this.proteinGrams = proteinGrams;
        this.fatGrams = fatGrams;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Food create(
            Long userId,
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            Instant now) {
        return new Food(
                userId,
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams,
                now);
    }

    public void update(
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            Instant now) {
        validate(
                name,
                servingAmount,
                servingUnit,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams);
        this.name = normalizeName(name);
        this.servingAmount = servingAmount;
        this.servingUnit = servingUnit;
        this.calories = calories;
        this.carbohydrateGrams = carbohydrateGrams;
        this.proteinGrams = proteinGrams;
        this.fatGrams = fatGrams;
        this.updatedAt = now;
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new NutritionRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }

    private static void validate(
            String name,
            BigDecimal servingAmount,
            ServingUnit servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        if (name == null || name.isBlank()) {
            throw new NutritionRuleException("음식 이름은 필수입니다.");
        }
        if (normalizeName(name).length() > 100) {
            throw new NutritionRuleException("음식 이름은 100자 이하여야 합니다.");
        }
        if (servingAmount == null || servingAmount.signum() <= 0) {
            throw new NutritionRuleException("1회 제공량은 0보다 커야 합니다.");
        }
        if (servingUnit == null) {
            throw new NutritionRuleException("제공량 단위는 필수입니다.");
        }
        validateNonNegative(calories, "칼로리");
        validateNonNegative(carbohydrateGrams, "탄수화물");
        validateNonNegative(proteinGrams, "단백질");
        validateNonNegative(fatGrams, "지방");
    }

    private static void validateNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new NutritionRuleException(label + " 값은 0 이상이어야 합니다.");
        }
    }

    private static String normalizeName(String name) {
        return name.trim();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public BigDecimal getServingAmount() { return servingAmount; }
    public ServingUnit getServingUnit() { return servingUnit; }
    public BigDecimal getCalories() { return calories; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
