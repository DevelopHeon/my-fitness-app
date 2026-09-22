package com.myfitness.routine.application.exception;

public class RoutineNotFoundException extends RuntimeException {
    public RoutineNotFoundException() {
        super("Routine을 찾을 수 없습니다.");
    }
}
