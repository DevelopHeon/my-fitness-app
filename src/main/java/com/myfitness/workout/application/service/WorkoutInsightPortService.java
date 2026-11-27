package com.myfitness.workout.application.service;

import com.myfitness.workout.application.usecase.response.WorkoutResult;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import com.myfitness.workout.application.usecase.WorkoutInsightUseCase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkoutInsightPortService implements WorkoutInsightQuery {
    private final WorkoutInsightUseCase useCase;

    public WorkoutInsightPortService(WorkoutInsightUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public List<WorkoutInsight> findCompletedWorkouts(Long userId) {
        return useCase.findCompletedWorkouts(userId).stream().map(WorkoutInsightPortService::toInsight).toList();
    }

    @Override
    public List<WorkoutInsight> findCompletedSince(Long userId, LocalDate from) {
        return useCase.findCompletedSince(userId, from).stream().map(WorkoutInsightPortService::toInsight).toList();
    }

    private static WorkoutInsight toInsight(WorkoutResult workout) {
        return new WorkoutInsight(
                workout.workoutDate(), workout.startedAt(),
                workout.exercises().stream().map(WorkoutInsightPortService::toExercise).toList());
    }

    private static ExerciseInsight toExercise(WorkoutResult.ExerciseResult exercise) {
        return new ExerciseInsight(
                exercise.exerciseType(), exercise.exerciseId(), exercise.exerciseName(), exercise.category(),
                exercise.sets().stream()
                        .map(set -> new SetInsight(set.weightKg(), set.reps(), set.completed()))
                        .toList());
    }
}
