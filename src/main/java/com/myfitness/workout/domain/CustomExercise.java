package com.myfitness.workout.domain;

import com.myfitness.workout.exception.WorkoutRuleException;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "custom_exercises", uniqueConstraints = {
        @UniqueConstraint(name = "uk_custom_exercise_user_name", columnNames = {"user_id", "name"})
})
public class CustomExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExerciseCategory category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CustomExercise() {}

    private CustomExercise(
            Long userId,
            String name,
            ExerciseCategory category,
            Instant createdAt) {
        if (userId == null || userId <= 0) {
            throw new WorkoutRuleException("유효한 사용자 ID가 필요합니다.");
        }
        if (name == null || name.isBlank()) {
            throw new WorkoutRuleException("운동 종목 이름은 필수입니다.");
        }
        if (category == null) {
            throw new WorkoutRuleException("운동 카테고리는 필수입니다.");
        }
        this.userId = userId;
        this.name = name.trim();
        this.category = category;
        this.createdAt = createdAt;
    }

    public static CustomExercise create(
            Long userId,
            String name,
            ExerciseCategory category,
            Instant createdAt) {
        return new CustomExercise(userId, name, category, createdAt);
    }

    public ExerciseReference toReference() {
        return new ExerciseReference(ExerciseType.CUSTOM, id, name, category, userId);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public ExerciseCategory getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
