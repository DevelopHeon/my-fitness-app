package com.myfitness.dashboard.infrastructure.query;

import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.dashboard.application.port.out.DashboardDataPort;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import org.springframework.stereotype.Component;

@Component
public class DashboardDataAdapter implements DashboardDataPort {
    private final WorkoutInsightQuery workoutInsightQuery;
    private final BodyInsightQuery bodyInsightQuery;

    public DashboardDataAdapter(
            WorkoutInsightQuery workoutInsightQuery,
            BodyInsightQuery bodyInsightQuery) {
        this.workoutInsightQuery = workoutInsightQuery;
        this.bodyInsightQuery = bodyInsightQuery;
    }

    @Override
    public DashboardSourceData load(Long userId) {
        return new DashboardSourceData(
                workoutInsightQuery.findCompletedWorkouts(userId).stream()
                        .map(workout -> new WorkoutData(
                                workout.workoutDate(),
                                workout.startedAt(),
                                workout.exercises().stream()
                                        .map(exercise -> new ExerciseData(
                                                exercise.exerciseType(),
                                                exercise.exerciseId(),
                                                exercise.exerciseName(),
                                                exercise.category(),
                                                exercise.sets().stream()
                                                        .map(set -> new SetData(
                                                                set.weightKg(),
                                                                set.reps(),
                                                                set.completed()))
                                                        .toList()))
                                        .toList()))
                        .toList(),
                bodyInsightQuery.findAll(userId).stream()
                        .map(body -> new BodyData(
                                body.id(),
                                body.measuredAt(),
                                body.weightKg(),
                                body.bodyFatPercentage(),
                                body.skeletalMuscleKg()))
                        .toList());
    }
}
