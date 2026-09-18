package com.myfitness.body.exception;

public class BodyRecordNotFoundException extends RuntimeException {
    public BodyRecordNotFoundException() {
        super("BodyRecord를 찾을 수 없습니다.");
    }
}
