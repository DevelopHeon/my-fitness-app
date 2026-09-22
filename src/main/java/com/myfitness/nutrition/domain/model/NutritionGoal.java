package com.myfitness.nutrition.domain.model;

import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "nutrition_goals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nutrition_goals_user",
                columnNames = "user_id"))
public class NutritionGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

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

    protected NutritionGoal() {}

    private NutritionGoal(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            Instant now) {
        if (userId == null || userId <= 0) {
            throw new NutritionRuleException("유효한 사용자 ID가 필요합니다.");
        }
        validate(calories, carbohydrateGrams, proteinGrams, fatGrams);
        this.userId = userId;
        this.calories = calories;
        this.carbohydrateGrams = carbohydrateGrams;
        this.proteinGrams = proteinGrams;
        this.fatGrams = fatGrams;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static NutritionGoal create(
            Long userId,
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            Instant now) {
        return new NutritionGoal(
                userId,
                calories,
                carbohydrateGrams,
                proteinGrams,
                fatGrams,
                now);
    }

    public void update(
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            Instant now) {
        validate(calories, carbohydrateGrams, proteinGrams, fatGrams);
        this.calories = calories;
        this.carbohydrateGrams = carbohydrateGrams;
        this.proteinGrams = proteinGrams;
        this.fatGrams = fatGrams;
        this.updatedAt = now;
    }

    private static void validate(
            BigDecimal calories,
            BigDecimal carbohydrateGrams,
            BigDecimal proteinGrams,
            BigDecimal fatGrams) {
        validatePositive(calories, "목표 칼로리");
        validateNonNegative(carbohydrateGrams, "목표 탄수화물");
        validateNonNegative(proteinGrams, "목표 단백질");
        validateNonNegative(fatGrams, "목표 지방");
    }

    private static void validatePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new NutritionRuleException(label + " 값은 0보다 커야 합니다.");
        }
    }

    private static void validateNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new NutritionRuleException(label + " 값은 0 이상이어야 합니다.");
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public BigDecimal getCalories() { return calories; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
