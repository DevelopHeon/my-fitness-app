package com.myfitness.workout.domain;

import com.myfitness.workout.exception.WorkoutRuleException;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "workouts")
public class Workout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutStatus status;

    @Column(length = 500)
    private String memo;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex asc")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    protected Workout() {}

    private Workout(Long userId, LocalDate workoutDate, String memo, Instant startedAt) {
        if (userId == null || userId <= 0) {
            throw new WorkoutRuleException("유효한 사용자 ID가 필요합니다.");
        }
        if (workoutDate == null) {
            throw new WorkoutRuleException("운동 날짜는 필수입니다.");
        }
        this.userId = userId;
        this.workoutDate = workoutDate;
        this.memo = normalize(memo);
        this.startedAt = startedAt;
        this.status = WorkoutStatus.IN_PROGRESS;
    }

    public static Workout start(
            Long userId,
            LocalDate workoutDate,
            String memo,
            Instant startedAt) {
        return new Workout(userId, workoutDate, memo, startedAt);
    }

    public WorkoutExercise addExercise(ExerciseReference exercise, String memo) {
        ensureMutable();
        if (!exercise.accessibleTo(userId)) {
            throw new WorkoutRuleException("사용 가능한 운동 종목만 추가할 수 있습니다.");
        }
        boolean duplicated = exercises.stream().anyMatch(entry ->
                entry.getExerciseType() == exercise.type()
                        && entry.getExerciseId().equals(exercise.id()));
        if (duplicated) {
            throw new WorkoutRuleException("이미 Workout에 추가된 운동 종목입니다.");
        }
        int nextOrder = exercises.stream()
                .mapToInt(WorkoutExercise::getOrderIndex)
                .max().orElse(0) + 1;
        WorkoutExercise workoutExercise =
                WorkoutExercise.create(this, exercise, nextOrder, memo);
        exercises.add(workoutExercise);
        return workoutExercise;
    }

    public void removeExercise(WorkoutExercise workoutExercise) {
        ensureMutable();
        if (!exercises.remove(workoutExercise)) {
            throw new WorkoutRuleException("해당 운동 기록이 Workout에 없습니다.");
        }
        reorderExercises();
    }

    public void complete(Instant completedAt) {
        ensureMutable();
        this.status = WorkoutStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void reopen() {
        if (status != WorkoutStatus.COMPLETED) {
            throw new WorkoutRuleException("완료된 운동만 다시 수정할 수 있습니다.");
        }
        this.status = WorkoutStatus.IN_PROGRESS;
        this.completedAt = null;
    }

    public void ensureMutable() {
        if (status == WorkoutStatus.COMPLETED) {
            throw new WorkoutRuleException("완료된 운동은 수정할 수 없습니다.");
        }
    }

    private void reorderExercises() {
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).changeOrder(i + 1);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getWorkoutDate() { return workoutDate; }
    public WorkoutStatus getStatus() { return status; }
    public String getMemo() { return memo; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<WorkoutExercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }
}
