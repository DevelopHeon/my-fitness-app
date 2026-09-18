package com.myfitness.workout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.workout.exception.WorkoutRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WorkoutTest {

    private final Instant startedAt = Instant.parse("2026-09-18T06:00:00Z");

    @Test
    void 운동은_진행중_상태로_시작한다() {
        Workout workout = Workout.start(1L, LocalDate.of(2026, 9, 18), "가슴", startedAt);

        assertThat(workout.getStatus()).isEqualTo(WorkoutStatus.IN_PROGRESS);
        assertThat(workout.getStartedAt()).isEqualTo(startedAt);
        assertThat(workout.getCompletedAt()).isNull();
    }

    @Test
    void 다른_사용자의_운동종목은_추가할_수_없다() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        Exercise otherUsersExercise = Exercise.create(2L, "벤치프레스", "CHEST", startedAt);

        assertThatThrownBy(() -> workout.addExercise(otherUsersExercise, null))
                .isInstanceOf(WorkoutRuleException.class)
                .hasMessageContaining("소유");
    }

    @Test
    void 완료된_운동은_수정하거나_다시_완료할_수_없다() {
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
    void 세트는_반복수나_운동시간_중_하나가_있어야_한다() {
        Workout workout = Workout.start(1L, LocalDate.now(), null, startedAt);
        WorkoutExercise exercise = workout.addExercise(
                Exercise.create(1L, "벤치프레스", "CHEST", startedAt), null);

        assertThatThrownBy(() -> exercise.addSet(BigDecimal.ZERO, 0, null, false))
                .isInstanceOf(WorkoutRuleException.class);
    }

    @Test
    void 세트_삭제시_세트번호를_다시_정렬한다() {
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
