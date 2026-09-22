package com.myfitness.workout.application.service;

import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import com.myfitness.workout.application.port.out.WorkoutRepositoryPort;
import com.myfitness.workout.domain.model.WorkoutStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutInsightService implements WorkoutInsightQuery {
    private final WorkoutRepositoryPort workoutRepositoryPort;

    public WorkoutInsightService(WorkoutRepositoryPort workoutRepositoryPort) {
        this.workoutRepositoryPort = workoutRepositoryPort;
    }

    @Override
    public List<WorkoutInsight> findCompletedWorkouts(Long userId) {
        return workoutRepositoryPort
                .findByUserIdAndStatus(userId, WorkoutStatus.COMPLETED)
                .stream()
                .map(workout -> new WorkoutInsight(
                        workout.getWorkoutDate(),
                        workout.getStartedAt(),
                        workout.getExercises().stream()
                                .map(entry -> new ExerciseInsight(
                                        entry.getExerciseType().name(),
                                        entry.getExerciseId(),
                                        entry.getExerciseName(),
                                        entry.getCategory().name(),
                                        entry.getSets().stream()
                                                .map(set -> new SetInsight(
                                                        set.getWeightKg(),
                                                        set.getReps(),
                                                        set.isCompleted()))
                                                .toList()))
                                .toList()))
                .toList();
    }
}
