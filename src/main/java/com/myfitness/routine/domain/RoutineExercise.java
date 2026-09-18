package com.myfitness.routine.domain;

import com.myfitness.workout.domain.Exercise;
import jakarta.persistence.*;

@Entity
@Table(name = "routine_exercises")
public class RoutineExercise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected RoutineExercise() {}

    private RoutineExercise(Routine routine, Exercise exercise, int orderIndex) {
        this.routine = routine;
        this.exercise = exercise;
        this.orderIndex = orderIndex;
    }

    static RoutineExercise create(Routine routine, Exercise exercise, int orderIndex) {
        return new RoutineExercise(routine, exercise, orderIndex);
    }

    public Long getId() { return id; }
    public Routine getRoutine() { return routine; }
    public Exercise getExercise() { return exercise; }
    public int getOrderIndex() { return orderIndex; }
}
