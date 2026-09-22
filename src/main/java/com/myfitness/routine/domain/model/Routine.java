package com.myfitness.routine.domain.model;

import com.myfitness.routine.domain.exception.RoutineRuleException;
import com.myfitness.exercise.domain.model.ExerciseReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "routines", uniqueConstraints = {
        @UniqueConstraint(name = "uk_routine_user_name", columnNames = {"user_id", "name"})
})
public class Routine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex asc")
    private List<RoutineExercise> exercises = new ArrayList<>();

    protected Routine() {}

    private Routine(
            Long userId,
            String name,
            List<ExerciseReference> exercises,
            Instant now) {
        validateUserId(userId);
        this.userId = userId;
        this.name = normalizeName(name);
        this.createdAt = now;
        this.updatedAt = now;
        replaceExercises(exercises, now);
    }

    public static Routine create(
            Long userId,
            String name,
            List<ExerciseReference> exercises,
            Instant now) {
        return new Routine(userId, name, exercises, now);
    }

    public void update(
            String name,
            List<ExerciseReference> exercises,
            Instant now) {
        this.name = normalizeName(name);
        replaceExercises(exercises, now);
    }

    private void replaceExercises(
            List<ExerciseReference> exerciseList,
            Instant now) {
        if (exerciseList == null || exerciseList.isEmpty()) {
            throw new RoutineRuleException("루틴에는 하나 이상의 운동 종목이 필요합니다.");
        }

        Set<String> keys = new HashSet<>();
        for (ExerciseReference exercise : exerciseList) {
            if (!exercise.accessibleTo(userId)) {
                throw new RoutineRuleException("사용 가능한 운동 종목만 루틴에 추가할 수 있습니다.");
            }
            if (!keys.add(exercise.key())) {
                throw new RoutineRuleException("같은 운동 종목을 루틴에 중복 추가할 수 없습니다.");
            }
        }

        exercises.clear();
        for (int i = 0; i < exerciseList.size(); i++) {
            exercises.add(RoutineExercise.create(
                    this, exerciseList.get(i), i + 1));
        }
        this.updatedAt = now;
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RoutineRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new RoutineRuleException("루틴 이름은 필수입니다.");
        }
        return name.trim();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RoutineExercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }
}
