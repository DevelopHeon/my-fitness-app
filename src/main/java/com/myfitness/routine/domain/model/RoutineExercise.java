package com.myfitness.routine.domain.model;

import com.myfitness.exercise.domain.model.*;
import jakarta.persistence.*;

@Entity
@Table(name = "routine_exercises")
public class RoutineExercise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 20)
    private ExerciseType exerciseType;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExerciseCategory category;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected RoutineExercise() {}

    private RoutineExercise(
            Routine routine,
            ExerciseReference exercise,
            int orderIndex) {
        this.routine = routine;
        this.exerciseType = exercise.type();
        this.exerciseId = exercise.id();
        this.exerciseName = exercise.name();
        this.category = exercise.category();
        this.orderIndex = orderIndex;
    }

    static RoutineExercise create(
            Routine routine,
            ExerciseReference exercise,
            int orderIndex) {
        return new RoutineExercise(routine, exercise, orderIndex);
    }

    public ExerciseReference toReference() {
        Long ownerUserId = exerciseType == ExerciseType.CUSTOM
                ? routine.getUserId()
                : null;
        return new ExerciseReference(
                exerciseType, exerciseId, exerciseName, category, ownerUserId);
    }

    public Long getId() { return id; }
    public Routine getRoutine() { return routine; }
    public ExerciseType getExerciseType() { return exerciseType; }
    public Long getExerciseId() { return exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public ExerciseCategory getCategory() { return category; }
    public int getOrderIndex() { return orderIndex; }
}
