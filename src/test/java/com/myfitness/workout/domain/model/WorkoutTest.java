package com.myfitness.workout.domain.model;

import com.myfitness.exercise.domain.model.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.workout.domain.exception.WorkoutRuleException;
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
        Workout workout = Workout.start(
                1L, LocalDate.of(2026, 9, 18), "가슴", startedAt);

        assertThat(workout.getStatus()).isEqualTo(WorkoutStatus.IN_PROGRESS);
        assertThat(workout.getStartedAt()).isEqualTo(startedAt);
        assertThat(workout.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("다른 사용자의 커스텀 운동 종목은 Workout에 추가할 수 없다")
    void rejectsCustomExerciseOwnedByAnotherUser() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        ExerciseReference otherUsersExercise = new ExerciseReference(
                ExerciseType.CUSTOM, 1L, "내 운동", ExerciseCategory.CHEST, 2L);

        assertThatThrownBy(() -> workout.addExercise(otherUsersExercise, null))
                .isInstanceOf(WorkoutRuleException.class)
                .hasMessageContaining("사용 가능한");
    }

    @Test
    @DisplayName("기본 운동 종목은 모든 사용자가 Workout에 추가할 수 있다")
    void allowsDefaultExerciseForEveryUser() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        ExerciseReference exercise = new ExerciseReference(
                ExerciseType.DEFAULT, 1L, "벤치프레스", ExerciseCategory.CHEST, null);

        workout.addExercise(exercise, null);

        assertThat(workout.getExercises()).hasSize(1);
        assertThat(workout.getExercises().getFirst().getExerciseType())
                .isEqualTo(ExerciseType.DEFAULT);
    }

    @Test
    @DisplayName("완료된 운동은 수정하거나 다시 완료할 수 없다")
    void rejectsChangesAfterWorkoutIsCompleted() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        ExerciseReference exercise = new ExerciseReference(
                ExerciseType.DEFAULT, 1L, "벤치프레스", ExerciseCategory.CHEST, null);
        workout.addExercise(exercise, null);
        workout.complete(startedAt.plusSeconds(3600));

        assertThatThrownBy(() -> workout.addExercise(
                new ExerciseReference(
                        ExerciseType.DEFAULT, 2L, "딥스", ExerciseCategory.CHEST, null),
                null))
                .isInstanceOf(WorkoutRuleException.class);
        assertThatThrownBy(() -> workout.complete(startedAt.plusSeconds(7200)))
                .isInstanceOf(WorkoutRuleException.class);
    }

    @Test
    @DisplayName("완료된 운동은 다시 편집 상태로 열어 수정한 뒤 재완료할 수 있다")
    void reopensCompletedWorkoutForEditing() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        workout.addExercise(
                new ExerciseReference(
                        ExerciseType.DEFAULT, 1L, "벤치프레스", ExerciseCategory.CHEST, null),
                null);
        workout.complete(startedAt.plusSeconds(3600));

        workout.reopen();
        workout.addExercise(
                new ExerciseReference(
                        ExerciseType.DEFAULT, 2L, "딥스", ExerciseCategory.CHEST, null),
                null);
        workout.complete(startedAt.plusSeconds(7200));

        assertThat(workout.getStatus()).isEqualTo(WorkoutStatus.COMPLETED);
        assertThat(workout.getExercises()).hasSize(2);
        assertThat(workout.getCompletedAt()).isEqualTo(startedAt.plusSeconds(7200));
    }

    @Test
    @DisplayName("세트에는 반복 횟수 또는 운동 시간 중 하나가 필요하다")
    void requiresRepsOrDurationForSet() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        WorkoutExercise exercise = workout.addExercise(
                new ExerciseReference(
                        ExerciseType.DEFAULT, 1L, "벤치프레스", ExerciseCategory.CHEST, null),
                null);

        assertThatThrownBy(() -> exercise.addSet(
                BigDecimal.ZERO, 0, null, false))
                .isInstanceOf(WorkoutRuleException.class);
    }

    @Test
    @DisplayName("세트를 삭제하면 남은 세트 번호를 다시 정렬한다")
    void reordersSetNumbersAfterDeletion() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        WorkoutExercise exercise = workout.addExercise(
                new ExerciseReference(
                        ExerciseType.DEFAULT, 1L, "벤치프레스", ExerciseCategory.CHEST, null),
                null);
        WorkoutSet first = exercise.addSet(
                new BigDecimal("60"), 10, null, true);
        exercise.addSet(new BigDecimal("70"), 8, null, true);

        exercise.removeSet(first);

        assertThat(exercise.getSets()).hasSize(1);
        assertThat(exercise.getSets().getFirst().getSetNumber()).isEqualTo(1);
    }
}
