package com.myfitness.workout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.workout.exception.WorkoutRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkoutTest {

    private final Instant startedAt = Instant.parse("2026-09-18T06:00:00Z");

    @Test
    @DisplayName("운동은 IN_PROGRESS 상태로 시작한다")
    void startsWithInProgressStatus() {
        Workout workout = Workout.start(1L, LocalDate.of(2026, 9, 18), "가슴", startedAt);

        assertThat(workout.getStatus()).isEqualTo(WorkoutStatus.IN_PROGRESS);
        assertThat(workout.getStartedAt()).isEqualTo(startedAt);
        assertThat(workout.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("다른 사용자의 운동 종목은 Workout에 추가할 수 없다")
    void rejectsExerciseOwnedByAnotherUser() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        Exercise otherUsersExercise = Exercise.create(2L, "벤치프레스", "CHEST", startedAt);

        assertThatThrownBy(() -> workout.addExercise(otherUsersExercise, null))
                .isInstanceOf(WorkoutRuleException.class)
                .hasMessageContaining("소유");
    }

    @Test
    @DisplayName("완료된 운동은 수정하거나 다시 완료할 수 없다")
    void rejectsChangesAfterWorkoutIsCompleted() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        Exercise exercise = Exercise.create(1L, "벤치프레스", "CHEST", startedAt);
        workout.addExercise(exercise, null);
        workout.complete(startedAt.plusSeconds(3600));

        assertThatThrownBy(() -> workout.addExercise(exercise, null))
                .isInstanceOf(WorkoutRuleException.class);
        assertThatThrownBy(() -> workout.complete(startedAt.plusSeconds(7200)))
                .isInstanceOf(WorkoutRuleException.class);
    }

    @Test
    @DisplayName("세트에는 반복 횟수 또는 운동 시간 중 하나가 필요하다")
    void requiresRepsOrDurationForSet() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        WorkoutExercise exercise = workout.addExercise(
                Exercise.create(1L, "벤치프레스", "CHEST", startedAt), null);

        assertThatThrownBy(() -> exercise.addSet(BigDecimal.ZERO, 0, null, false))
                .isInstanceOf(WorkoutRuleException.class);
    }

    @Test
    @DisplayName("세트를 삭제하면 남은 세트 번호를 다시 정렬한다")
    void reordersSetNumbersAfterDeletion() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        WorkoutExercise exercise = workout.addExercise(
                Exercise.create(1L, "벤치프레스", "CHEST", startedAt), null);
        WorkoutSet first = exercise.addSet(new BigDecimal("60"), 10, null, true);
        exercise.addSet(new BigDecimal("70"), 8, null, true);

        exercise.removeSet(first);

        assertThat(exercise.getSets()).hasSize(1);
        assertThat(exercise.getSets().getFirst().getSetNumber()).isEqualTo(1);
    }
}
