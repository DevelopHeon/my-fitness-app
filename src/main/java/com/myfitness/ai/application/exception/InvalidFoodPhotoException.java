package com.myfitness.ai.application.exception;

public class InvalidFoodPhotoException extends RuntimeException {
    public enum Reason { INVALID, TOO_LARGE, UNSUPPORTED }
    private final Reason reason;

    public InvalidFoodPhotoException(Reason reason) {
        super(switch (reason) {
            case INVALID -> "읽을 수 있는 음식 사진 한 장을 선택해주세요. 이미지는 1600만 픽셀 이하여야 합니다.";
            case TOO_LARGE -> "사진은 5 MiB 이하로 선택해주세요.";
            case UNSUPPORTED -> "JPG 또는 PNG 사진을 선택해주세요.";
        });
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
