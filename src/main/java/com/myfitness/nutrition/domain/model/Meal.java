package com.myfitness.nutrition.domain.model;

import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "meals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meals_user_date_type",
                columnNames = {"user_id", "meal_date", "meal_type"}))
public class Meal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @OneToMany(
            mappedBy = "meal",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<MealFood> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Meal() {}

    private Meal(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Instant now) {
        if (userId == null || userId <= 0) {
            throw new NutritionRuleException("유효한 사용자 ID가 필요합니다.");
        }
        if (mealDate == null) {
            throw new NutritionRuleException("식사 날짜는 필수입니다.");
        }
        if (mealType == null) {
            throw new NutritionRuleException("식사 구분은 필수입니다.");
        }
        this.userId = userId;
        this.mealDate = mealDate;
        this.mealType = mealType;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Meal create(
            Long userId,
            LocalDate mealDate,
            MealType mealType,
            Instant now) {
        return new Meal(userId, mealDate, mealType, now);
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getMealDate() { return mealDate; }
    public MealType getMealType() { return mealType; }
    public List<MealFood> getItems() {
        return Collections.unmodifiableList(items);
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
