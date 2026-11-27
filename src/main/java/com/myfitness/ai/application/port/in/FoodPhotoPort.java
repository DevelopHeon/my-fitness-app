package com.myfitness.ai.application.port.in;

public interface FoodPhotoPort {
    AiFoodPhotoView analyze(Long userId, Long conversationId, PhotoInput input);

    record PhotoInput(byte[] bytes, String contentType) {}
}
