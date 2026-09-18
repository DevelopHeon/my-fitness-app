package com.myfitness.workout.domain;

import com.myfitness.workout.exception.WorkoutRuleException;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "exercises", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exercise_user_name", columnNames = {"user_id", "name"})
})
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Exercise() {
    }

    private Exercise(Long userId, String name, String category, Instant createdAt) {
        if (userId == null || userId <= 0) throw new WorkoutRuleException("유효한 사용자 ID가 필요합니다.");
        if (name == null || name.isBlank()) throw new WorkoutRuleException("운동 종목 이름은 필수입니다.");
        this.userId = userId;
        this.name = name.trim();
        this.category = category == null || category.isBlank() ? null : category.trim();
        this.createdAt = createdAt;
    }

    public static Exercise create(Long userId, String name, String category, Instant createdAt) {
        return new Exercise(userId, name, category, createdAt);
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
