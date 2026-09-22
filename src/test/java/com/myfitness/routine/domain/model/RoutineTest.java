package com.myfitness.routine.domain.model;

import com.myfitness.exercise.domain.model.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.routine.domain.exception.RoutineRuleException;
import com.myfitness.workout.domain.model.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoutineTest {
    private final Instant now = Instant.parse("2026-09-18T08:00:00Z");

    @Test
    @DisplayName("루틴은 기본 운동과 커스텀 운동의 저장 순서를 유지한다")
    void keepsExerciseOrder() {
        ExerciseReference benchPress = new ExerciseReference(
                ExerciseType.DEFAULT, 1L, "벤치프레스", ExerciseCategory.CHEST, null);
        ExerciseReference custom = new ExerciseReference(
                ExerciseType.CUSTOM, 1L, "나만의 스쿼트", ExerciseCategory.LEGS, 1L);

        Routine routine = Routine.create(
                1L, "Push Legs", List.of(benchPress, custom), now);

        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getOrderIndex)
                .containsExactly(1, 2);
        assertThat(routine.getExercises())
                .extracting(RoutineExercise::getExerciseName)
                .containsExactly("벤치프레스", "나만의 스쿼트");
    }

    @Test
    @DisplayName("다른 사용자의 커스텀 운동 종목은 루틴에 추가할 수 없다")
    void rejectsCustomExerciseOwnedByAnotherUser() {
        ExerciseReference otherUsersExercise = new ExerciseReference(
                ExerciseType.CUSTOM, 1L, "타인 운동", ExerciseCategory.LEGS, 2L);

        assertThatThrownBy(() ->
                Routine.create(1L, "Legs", List.of(otherUsersExercise), now))
                .isInstanceOf(RoutineRuleException.class)
                .hasMessageContaining("사용 가능한");
    }

    @Test
    @DisplayName("운동 종목이 없는 루틴은 생성할 수 없다")
    void rejectsRoutineWithoutExercises() {
        assertThatThrownBy(() ->
                Routine.create(1L, "Empty", List.of(), now))
                .isInstanceOf(RoutineRuleException.class)
                .hasMessageContaining("하나 이상의 운동 종목");
    }
}
