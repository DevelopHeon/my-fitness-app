package com.myfitness.routine.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.routine.exception.RoutineRuleException;
import com.myfitness.workout.domain.Exercise;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoutineTest {
    private final Instant now = Instant.parse("2026-09-18T08:00:00Z");

    @Test
    @DisplayName("루틴은 저장된 운동 종목 순서를 유지한다")
    void keepsExerciseOrder() {
        Exercise benchPress = Exercise.create(1L, "벤치프레스", "CHEST", now);
        Exercise squat = Exercise.create(1L, "스쿼트", "LEGS", now);

        Routine routine = Routine.create(1L, "Push Legs", List.of(benchPress, squat), now);

        assertThat(routine.getExercises()).extracting(RoutineExercise::getOrderIndex)
                .containsExactly(1, 2);
        assertThat(routine.getExercises()).extracting(entry -> entry.getExercise().getName())
                .containsExactly("벤치프레스", "스쿼트");
    }

    @Test
    @DisplayName("다른 사용자의 운동 종목은 루틴에 추가할 수 없다")
    void rejectsExerciseOwnedByAnotherUser() {
        Exercise otherUsersExercise = Exercise.create(2L, "스쿼트", "LEGS", now);

        assertThatThrownBy(() ->
                Routine.create(1L, "Legs", List.of(otherUsersExercise), now))
                .isInstanceOf(RoutineRuleException.class)
                .hasMessageContaining("본인 소유");
    }

    @Test
    @DisplayName("운동 종목이 없는 루틴은 생성할 수 없다")
    void rejectsRoutineWithoutExercises() {
        assertThatThrownBy(() -> Routine.create(1L, "Empty", List.of(), now))
                .isInstanceOf(RoutineRuleException.class)
                .hasMessageContaining("하나 이상의 운동 종목");
    }
}
