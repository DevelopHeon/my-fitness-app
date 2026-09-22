package com.myfitness.workout.domain.model;

import com.myfitness.workout.domain.exception.WorkoutRuleException;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "workout_sets")
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "weight_kg", precision = 7, scale = 2, nullable = false)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private int reps;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(nullable = false)
    private boolean completed;

    protected WorkoutSet() {
    }

    private WorkoutSet(
            WorkoutExercise workoutExercise, int setNumber, BigDecimal weightKg,
            int reps, Integer durationSeconds, boolean completed) {
        this.workoutExercise = workoutExercise;
        this.setNumber = setNumber;
        update(weightKg, reps, durationSeconds, completed);
    }

    static WorkoutSet create(
            WorkoutExercise workoutExercise, int setNumber, BigDecimal weightKg,
            int reps, Integer durationSeconds, boolean completed) {
        return new WorkoutSet(workoutExercise, setNumber, weightKg, reps, durationSeconds, completed);
    }

    void update(BigDecimal weightKg, int reps, Integer durationSeconds, boolean completed) {
        BigDecimal normalizedWeight = weightKg == null ? BigDecimal.ZERO : weightKg;
        if (normalizedWeight.signum() < 0) throw new WorkoutRuleException("중량은 0 이상이어야 합니다.");
        if (reps < 0) throw new WorkoutRuleException("반복 횟수는 0 이상이어야 합니다.");
        if (durationSeconds != null && durationSeconds < 0) throw new WorkoutRuleException("운동 시간은 0 이상이어야 합니다.");
        if (reps == 0 && (durationSeconds == null || durationSeconds == 0)) {
            throw new WorkoutRuleException("반복 횟수 또는 운동 시간 중 하나는 필요합니다.");
        }
        this.weightKg = normalizedWeight;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.completed = completed;
    }

    void changeSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public Long getId() { return id; }
    public int getSetNumber() { return setNumber; }
    public BigDecimal getWeightKg() { return weightKg; }
    public int getReps() { return reps; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public boolean isCompleted() { return completed; }
}
