package com.myfitness.workout.application.exception;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(String target) {
        super(target + "을(를) 찾을 수 없습니다.");
    }
}
