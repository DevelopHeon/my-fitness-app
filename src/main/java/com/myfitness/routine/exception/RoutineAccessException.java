package com.myfitness.routine.exception;

public class RoutineAccessException extends RuntimeException {
    public RoutineAccessException() {
        super("해당 Routine에 접근할 수 없습니다.");
    }
}
