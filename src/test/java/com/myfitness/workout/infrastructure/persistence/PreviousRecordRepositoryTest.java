package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.exercise.domain.model.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.workout.domain.model.*;
import com.myfitness.workout.domain.repository.WorkoutExerciseRepository;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PreviousRecordRepositoryTest {
    @Autowired WorkoutRepository workoutRepository;
    @Autowired WorkoutExerciseRepository workoutExerciseRepository;

    @Test
    @DisplayName("운동 출처와 ID가 같은 동일 종목의 가장 최근 완료 Workout 기록을 조회한다")
    void findsMostRecentCompletedWorkoutRecord() {
        ExerciseReference exercise = new ExerciseReference(
                ExerciseType.DEFAULT, 10L, "벤치프레스", ExerciseCategory.CHEST, null);

        saveCompletedWorkout(
                exercise, LocalDate.of(2026, 9, 10), "60", 10,
                Instant.parse("2026-09-10T01:00:00Z"));
        Workout latest = saveCompletedWorkout(
                exercise, LocalDate.of(2026, 9, 17), "70", 8,
                Instant.parse("2026-09-17T01:00:00Z"));

        WorkoutExercise result = workoutExerciseRepository
                .findLatestCompletedByExercise(
                        1L, ExerciseType.DEFAULT, 10L)
                .orElseThrow();

        assertThat(result.getWorkout().getId()).isEqualTo(latest.getId());
        assertThat(result.getSets().getFirst().getWeightKg())
                .isEqualByComparingTo("70");
        assertThat(result.getSets().getFirst().getReps()).isEqualTo(8);
    }

    private Workout saveCompletedWorkout(
            ExerciseReference exercise,
            LocalDate date,
            String weight,
            int reps,
            Instant completedAt) {
        Workout workout = Workout.start(
                1L, date, null, completedAt.minusSeconds(3600));
        WorkoutExercise entry = workout.addExercise(exercise, null);
        entry.addSet(new BigDecimal(weight), reps, null, true);
        workout.complete(completedAt);
        return workoutRepository.save(workout);
    }
}
