package com.myfitness.nutrition.domain.model;

import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "meal_foods")
public class MealFood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(name = "carbohydrate_grams", precision = 8, scale = 2)
    private BigDecimal carbohydrateGrams;

    @Column(name = "protein_grams", precision = 8, scale = 2)
    private BigDecimal proteinGrams;

    @Column(name = "fat_grams", precision = 8, scale = 2)
    private BigDecimal fatGrams;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MealFood() {}

    public static MealFood create(Meal meal, String name, BigDecimal calories,
            BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat, Instant now) {
        MealFood item = new MealFood();
        item.update(meal, name, calories, carbohydrate, protein, fat, now);
        item.createdAt = now;
        return item;
    }

    public void update(Meal meal, String name, BigDecimal calories,
            BigDecimal carbohydrate, BigDecimal protein, BigDecimal fat, Instant now) {
        if (meal == null || now == null || name == null || name.isBlank() || name.trim().length() > 100) {
            throw new NutritionRuleException("식사와 1~100자의 음식명이 필요합니다.");
        }
        if (calories == null) {
            throw new NutritionRuleException("칼로리는 필수입니다.");
        }
        validate(calories);
        validate(carbohydrate);
        validate(protein);
        validate(fat);
        this.meal = meal;
        this.foodName = name.trim();
        this.calories = calories;
        this.carbohydrateGrams = carbohydrate;
        this.proteinGrams = protein;
        this.fatGrams = fat;
        this.updatedAt = now;
    }

    private static void validate(BigDecimal value) {
        if (value != null && (value.signum() < 0 || value.compareTo(new BigDecimal("999999.99")) > 0
                || value.stripTrailingZeros().scale() > 2)) {
            throw new NutritionRuleException("영양값은 0~999999.99 범위에서 소수 둘째 자리까지 입력해주세요.");
        }
    }

    public Long getId() { return id; }
    public Meal getMeal() { return meal; }
    public String getFoodName() { return foodName; }
    public BigDecimal getCalories() { return calories; }
    public BigDecimal getCarbohydrateGrams() { return carbohydrateGrams; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
