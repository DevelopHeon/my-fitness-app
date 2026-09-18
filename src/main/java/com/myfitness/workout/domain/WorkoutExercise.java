package com.myfitness.workout.domain;

import com.myfitness.workout.exception.WorkoutRuleException;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(length = 500)
    private String memo;

    @OneToMany(mappedBy = "workoutExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber asc")
    private List<WorkoutSet> sets = new ArrayList<>();

    protected WorkoutExercise() {
    }

    private WorkoutExercise(Workout workout, Exercise exercise, int orderIndex, String memo) {
        this.workout = workout;
        this.exercise = exercise;
        this.orderIndex = orderIndex;
        this.memo = memo == null || memo.isBlank() ? null : memo.trim();
    }

    static WorkoutExercise create(Workout workout, Exercise exercise, int orderIndex, String memo) {
        return new WorkoutExercise(workout, exercise, orderIndex, memo);
    }

    public WorkoutSet addSet(BigDecimal weightKg, int reps, Integer durationSeconds, boolean completed) {
        workout.ensureMutable();
        WorkoutSet set = WorkoutSet.create(this, sets.size() + 1, weightKg, reps, durationSeconds, completed);
        sets.add(set);
        return set;
    }

    public void updateSet(WorkoutSet set, BigDecimal weightKg, int reps, Integer durationSeconds, boolean completed) {
        workout.ensureMutable();
        ensureContains(set);
        set.update(weightKg, reps, durationSeconds, completed);
    }

    public void removeSet(WorkoutSet set) {
        workout.ensureMutable();
        ensureContains(set);
        sets.remove(set);
        for (int i = 0; i < sets.size(); i++) sets.get(i).changeSetNumber(i + 1);
    }

    void changeOrder(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    private void ensureContains(WorkoutSet set) {
        if (!sets.contains(set)) throw new WorkoutRuleException("해당 세트가 운동 기록에 없습니다.");
    }

    public Long getId() { return id; }
    public Workout getWorkout() { return workout; }
    public Exercise getExercise() { return exercise; }
    public int getOrderIndex() { return orderIndex; }
    public String getMemo() { return memo; }
    public List<WorkoutSet> getSets() { return Collections.unmodifiableList(sets); }
}
