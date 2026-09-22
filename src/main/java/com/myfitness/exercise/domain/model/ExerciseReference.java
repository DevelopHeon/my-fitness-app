package com.myfitness.exercise.domain.model;

import com.myfitness.exercise.domain.exception.ExerciseRuleException;

public record ExerciseReference(
        ExerciseType type,
        Long id,
        String name,
        ExerciseCategory category,
        Long ownerUserId
) {
    public ExerciseReference {
        if (type == null || id == null || id <= 0) {
            throw new ExerciseRuleException("유효한 운동 종목 식별자가 필요합니다.");
        }
        if (name == null || name.isBlank() || category == null) {
            throw new ExerciseRuleException("운동 종목 이름과 카테고리는 필수입니다.");
        }
        if (type == ExerciseType.CUSTOM && (ownerUserId == null || ownerUserId <= 0)) {
            throw new ExerciseRuleException("커스텀 운동 종목에는 소유 사용자가 필요합니다.");
        }
    }

    public boolean accessibleTo(Long userId) {
        return type == ExerciseType.DEFAULT || ownerUserId.equals(userId);
    }

    public String key() {
        return type.name() + ":" + id;
    }
}
