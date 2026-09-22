package com.myfitness.exercise.application.exception;

public class ExerciseNotFoundException extends RuntimeException {
    public ExerciseNotFoundException(String target) {
        super(target + "을(를) 찾을 수 없습니다.");
    }
}
