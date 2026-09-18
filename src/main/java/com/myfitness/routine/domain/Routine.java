package com.myfitness.routine.domain;

import com.myfitness.routine.exception.RoutineRuleException;
import com.myfitness.workout.domain.Exercise;
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

    private Routine(Long userId, String name, List<Exercise> exercises, Instant now) {
        validateUserId(userId);
        this.userId = userId;
        this.name = normalizeName(name);
        this.createdAt = now;
        this.updatedAt = now;
        replaceExercises(exercises, now);
    }

    public static Routine create(Long userId, String name, List<Exercise> exercises, Instant now) {
        return new Routine(userId, name, exercises, now);
    }

    public void update(String name, List<Exercise> exercises, Instant now) {
        this.name = normalizeName(name);
        replaceExercises(exercises, now);
    }

    private void replaceExercises(List<Exercise> exerciseList, Instant now) {
        if (exerciseList == null || exerciseList.isEmpty()) {
            throw new RoutineRuleException("루틴에는 하나 이상의 운동 종목이 필요합니다.");
        }
        Set<Long> ids = new HashSet<>();
        for (Exercise exercise : exerciseList) {
            if (!exercise.belongsTo(userId)) {
                throw new RoutineRuleException("본인 소유 운동 종목만 루틴에 추가할 수 있습니다.");
            }
            if (exercise.getId() != null && !ids.add(exercise.getId())) {
                throw new RoutineRuleException("같은 운동 종목을 루틴에 중복 추가할 수 없습니다.");
            }
        }
        exercises.clear();
        for (int i = 0; i < exerciseList.size(); i++) {
            exercises.add(RoutineExercise.create(this, exerciseList.get(i), i + 1));
        }
        this.updatedAt = now;
    }

    public boolean belongsTo(Long userId) { return this.userId.equals(userId); }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) throw new RoutineRuleException("유효한 사용자 ID가 필요합니다.");
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) throw new RoutineRuleException("루틴 이름은 필수입니다.");
        return name.trim();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RoutineExercise> getExercises() { return Collections.unmodifiableList(exercises); }
}
