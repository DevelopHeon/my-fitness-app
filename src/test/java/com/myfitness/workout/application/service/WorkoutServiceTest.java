package com.myfitness.workout.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.application.exception.WorkoutAccessException;
import com.myfitness.workout.domain.repository.WorkoutExerciseRepository;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkoutServiceTest {

    private final WorkoutRepository workoutRepository = Mockito.mock(WorkoutRepository.class);
    private final WorkoutExerciseRepository workoutExerciseRepository =
            Mockito.mock(WorkoutExerciseRepository.class);
    private final WorkoutService service =
            new WorkoutService(workoutRepository, workoutExerciseRepository);

    @Test
    @DisplayName("다른 사용자의 Workout은 조회할 수 없다")
    void rejectsWorkoutAccessByAnotherUser() {
        Workout workout = Workout.start(
                2L,
                LocalDate.of(2026, 9, 18),
                null,
                Instant.parse("2026-09-18T06:00:00Z"));
        when(workoutRepository.findById(10L)).thenReturn(Optional.of(workout));

        assertThatThrownBy(() -> service.getOwned(1L, 10L))
                .isInstanceOf(WorkoutAccessException.class);
    }
}
