package com.myfitness.routine.service;

import com.myfitness.routine.domain.Routine;
import com.myfitness.routine.dto.request.*;
import com.myfitness.routine.dto.response.*;
import com.myfitness.workout.domain.ExerciseReference;
import com.myfitness.workout.domain.Workout;
import com.myfitness.workout.domain.WorkoutExercise;
import com.myfitness.workout.dto.request.ExerciseReferenceRequest;
import com.myfitness.workout.dto.response.PreviousExerciseRecordResponse;
import com.myfitness.workout.dto.response.WorkoutResponse;
import com.myfitness.workout.service.ExerciseService;
import com.myfitness.workout.service.WorkoutService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoutineApplicationService {
    private final RoutineService routineService;
    private final ExerciseService exerciseService;
    private final WorkoutService workoutService;

    public RoutineApplicationService(
            RoutineService routineService,
            ExerciseService exerciseService,
            WorkoutService workoutService) {
        this.routineService = routineService;
        this.exerciseService = exerciseService;
        this.workoutService = workoutService;
    }

    @Transactional
    public RoutineResponse create(Long userId, RoutineUpsertRequest request) {
        List<ExerciseReference> exercises =
                resolveExercises(userId, request.exercises());
        return RoutineResponse.from(
                routineService.create(userId, request.name(), exercises));
    }

    public List<RoutineResponse> list(Long userId) {
        return routineService.list(userId).stream()
                .map(RoutineResponse::from)
                .toList();
    }

    public RoutineResponse get(Long userId, Long routineId) {
        return RoutineResponse.from(routineService.getOwned(userId, routineId));
    }

    @Transactional
    public RoutineResponse update(
            Long userId,
            Long routineId,
            RoutineUpsertRequest request) {
        Routine routine = routineService.getOwned(userId, routineId);
        List<ExerciseReference> exercises =
                resolveExercises(userId, request.exercises());
        return RoutineResponse.from(
                routineService.update(routine, request.name(), exercises));
    }

    @Transactional
    public void delete(Long userId, Long routineId) {
        routineService.delete(routineService.getOwned(userId, routineId));
    }

    @Transactional
    public RoutineWorkoutStartResponse startWorkout(
            Long userId,
            Long routineId,
            StartRoutineWorkoutRequest request) {
        Routine routine = routineService.getOwned(userId, routineId);
        List<ExerciseReference> exercises = routine.getExercises().stream()
                .map(entry -> entry.toReference())
                .toList();

        Workout workout = workoutService.startWithExercises(
                userId, request.workoutDate(), request.memo(), exercises);

        List<PreviousExerciseRecordResponse> previousRecords =
                workout.getExercises().stream()
                        .map(entry -> workoutService.getPreviousCompletedExercise(
                                userId,
                                entry.getExerciseType(),
                                entry.getExerciseId()))
                        .filter(previous -> previous != null)
                        .map(PreviousExerciseRecordResponse::from)
                        .toList();

        return new RoutineWorkoutStartResponse(
                WorkoutResponse.from(workout), previousRecords);
    }

    private List<ExerciseReference> resolveExercises(
            Long userId,
            List<ExerciseReferenceRequest> requests) {
        return requests.stream()
                .map(request -> exerciseService.getAvailable(
                        userId,
                        request.exerciseType(),
                        request.exerciseId()))
                .toList();
    }
}
