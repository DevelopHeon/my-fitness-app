package com.myfitness.workout.application.exception;

public class WorkoutAccessException extends RuntimeException {

    public WorkoutAccessException() {
        super("해당 운동 기록에 접근할 수 없습니다.");
    }
}
