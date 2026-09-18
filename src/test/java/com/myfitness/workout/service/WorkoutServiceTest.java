package com.myfitness.workout.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.myfitness.workout.domain.Workout;
import com.myfitness.workout.exception.WorkoutAccessException;
import com.myfitness.workout.repository.WorkoutExerciseRepository;
import com.myfitness.workout.repository.WorkoutRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkoutServiceTest {

    private final WorkoutRepository workoutRepository = Mockito.mock(WorkoutRepository.class);
    private final WorkoutExerciseRepository workoutExerciseRepository =
            Mockito.mock(WorkoutExerciseRepository.class);
    private final WorkoutService service =
            new WorkoutService(workoutRepository, workoutExerciseRepository);

    @Test
    void 다른_사용자의_운동은_조회할_수_없다() {
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
