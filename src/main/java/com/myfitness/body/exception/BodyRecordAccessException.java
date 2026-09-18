package com.myfitness.body.exception;

public class BodyRecordAccessException extends RuntimeException {
    public BodyRecordAccessException() {
        super("해당 BodyRecord에 접근할 수 없습니다.");
    }
}
