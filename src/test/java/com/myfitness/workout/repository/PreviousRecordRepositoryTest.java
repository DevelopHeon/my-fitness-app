package com.myfitness.workout.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.workout.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PreviousRecordRepositoryTest {

    @Autowired ExerciseRepository exerciseRepository;
    @Autowired WorkoutRepository workoutRepository;
    @Autowired WorkoutExerciseRepository workoutExerciseRepository;

    @Test
    void 가장_최근에_완료한_운동의_기록을_조회한다() {
        Exercise exercise = exerciseRepository.save(
                Exercise.create(1L, "벤치프레스", "CHEST", Instant.parse("2026-09-01T00:00:00Z")));

        saveCompletedWorkout(exercise, LocalDate.of(2026, 9, 10), "60", 10,
                Instant.parse("2026-09-10T01:00:00Z"));
        Workout latest = saveCompletedWorkout(exercise, LocalDate.of(2026, 9, 17), "70", 8,
                Instant.parse("2026-09-17T01:00:00Z"));

        WorkoutExercise result = workoutExerciseRepository
                .findFirstByWorkout_UserIdAndExercise_IdAndWorkout_StatusOrderByWorkout_CompletedAtDesc(
                        1L, exercise.getId(), WorkoutStatus.COMPLETED)
                .orElseThrow();

        assertThat(result.getWorkout().getId()).isEqualTo(latest.getId());
        assertThat(result.getSets().getFirst().getWeightKg()).isEqualByComparingTo("70");
        assertThat(result.getSets().getFirst().getReps()).isEqualTo(8);
    }

    private Workout saveCompletedWorkout(
            Exercise exercise, LocalDate date, String weight, int reps, Instant completedAt) {
        Workout workout = Workout.start(1L, date, null, completedAt.minusSeconds(3600));
        WorkoutExercise entry = workout.addExercise(exercise, null);
        entry.addSet(new BigDecimal(weight), reps, null, true);
        workout.complete(completedAt);
        return workoutRepository.saveAndFlush(workout);
    }
}
