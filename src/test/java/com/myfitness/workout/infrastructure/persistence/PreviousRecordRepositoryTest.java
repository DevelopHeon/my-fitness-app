package com.myfitness.workout.infrastructure.persistence;

import com.myfitness.workout.application.usecase.response.PreviousExerciseRecordResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.workout.application.port.out.PreviousExerciseRecordQueryPort;
import com.myfitness.workout.domain.repository.WorkoutRepository;
import com.myfitness.workout.domain.model.ExerciseSnapshot;
import com.myfitness.workout.domain.model.ExerciseType;
import com.myfitness.workout.domain.model.ExerciseCategory;
import com.myfitness.workout.domain.model.Workout;
import com.myfitness.workout.domain.model.WorkoutExercise;
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
    @Autowired PreviousExerciseRecordQueryPort previousRecordQuery;

    @Test
    @DisplayName("운동 출처와 ID가 같은 동일 종목의 가장 최근 완료 Workout 기록을 조회한다")
    void findsMostRecentCompletedWorkoutRecord() {
        ExerciseSnapshot exercise = new ExerciseSnapshot(
                ExerciseType.DEFAULT, 10L, "벤치프레스", ExerciseCategory.CHEST, null);

        saveCompletedWorkout(
                exercise, LocalDate.of(2026, 9, 10), "60", 10,
                Instant.parse("2026-09-10T01:00:00Z"));
        Workout latest = saveCompletedWorkout(
                exercise, LocalDate.of(2026, 9, 17), "70", 8,
                Instant.parse("2026-09-17T01:00:00Z"));

        PreviousExerciseRecordResult result = previousRecordQuery
                .findLatestCompletedByExercise(
                        1L, ExerciseType.DEFAULT, 10L)
                .orElseThrow();

        assertThat(result.workoutId()).isEqualTo(latest.getId());
        assertThat(result.sets().getFirst().weightKg())
                .isEqualByComparingTo("70");
        assertThat(result.sets().getFirst().reps()).isEqualTo(8);
    }

    private Workout saveCompletedWorkout(
            ExerciseSnapshot exercise,
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
