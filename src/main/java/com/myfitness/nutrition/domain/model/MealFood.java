package com.myfitness.nutrition.domain.model;

import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(name = "source_food_id", nullable = false)
    private Long sourceFoodId;

    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    @Column(name = "serving_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal servingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "serving_unit", nullable = false, length = 20)
    private ServingUnit servingUnit;

    @Column(name = "calories_per_serving", nullable = false, precision = 8, scale = 2)
    private BigDecimal caloriesPerServing;

    @Column(name = "carbohydrate_grams_per_serving", nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrateGramsPerServing;

    @Column(name = "protein_grams_per_serving", nullable = false, precision = 8, scale = 2)
    private BigDecimal proteinGramsPerServing;

    @Column(name = "fat_grams_per_serving", nullable = false, precision = 8, scale = 2)
    private BigDecimal fatGramsPerServing;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal servings;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MealFood() {}

    private MealFood(
            Meal meal,
            Food food,
            BigDecimal servings,
            Instant now) {
        if (meal == null || food == null) {
            throw new NutritionRuleException("식사와 음식 정보가 필요합니다.");
        }
        validateServings(servings);
        if (!meal.belongsTo(food.getUserId())) {
            throw new NutritionRuleException("다른 사용자의 음식은 식단에 추가할 수 없습니다.");
        }

        this.meal = meal;
        this.sourceFoodId = food.getId();
        this.foodName = food.getName();
        this.servingAmount = food.getServingAmount();
        this.servingUnit = food.getServingUnit();
        this.caloriesPerServing = food.getCalories();
        this.carbohydrateGramsPerServing = food.getCarbohydrateGrams();
        this.proteinGramsPerServing = food.getProteinGrams();
        this.fatGramsPerServing = food.getFatGrams();
        this.servings = servings;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static MealFood fromFood(
            Meal meal,
            Food food,
            BigDecimal servings,
            Instant now) {
        return new MealFood(meal, food, servings, now);
    }

    public void updateServings(BigDecimal servings, Instant now) {
        validateServings(servings);
        this.servings = servings;
        this.updatedAt = now;
    }

    private static void validateServings(BigDecimal servings) {
        if (servings == null || servings.signum() <= 0) {
            throw new NutritionRuleException("섭취 회분은 0보다 커야 합니다.");
        }
        if (servings.compareTo(new BigDecimal("100")) > 0) {
            throw new NutritionRuleException("섭취 회분은 100 이하로 입력해주세요.");
        }
    }

    private BigDecimal total(BigDecimal value) {
        return value.multiply(servings).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalCalories() {
        return total(caloriesPerServing);
    }

    public BigDecimal totalCarbohydrateGrams() {
        return total(carbohydrateGramsPerServing);
    }

    public BigDecimal totalProteinGrams() {
        return total(proteinGramsPerServing);
    }

    public BigDecimal totalFatGrams() {
        return total(fatGramsPerServing);
    }

    public Long getId() { return id; }
    public Meal getMeal() { return meal; }
    public Long getSourceFoodId() { return sourceFoodId; }
    public String getFoodName() { return foodName; }
    public BigDecimal getServingAmount() { return servingAmount; }
    public ServingUnit getServingUnit() { return servingUnit; }
    public BigDecimal getCaloriesPerServing() { return caloriesPerServing; }
    public BigDecimal getCarbohydrateGramsPerServing() { return carbohydrateGramsPerServing; }
    public BigDecimal getProteinGramsPerServing() { return proteinGramsPerServing; }
    public BigDecimal getFatGramsPerServing() { return fatGramsPerServing; }
    public BigDecimal getServings() { return servings; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
