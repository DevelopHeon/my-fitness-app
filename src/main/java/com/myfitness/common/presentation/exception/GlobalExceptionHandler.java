package com.myfitness.common.presentation.exception;

import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException;
import com.myfitness.ai.domain.exception.AiRuleException;
import com.myfitness.body.application.exception.BodyRecordAccessException;
import com.myfitness.body.application.exception.BodyRecordNotFoundException;
import com.myfitness.body.domain.exception.BodyRecordRuleException;
import com.myfitness.exercise.application.exception.ExerciseNotFoundException;
import com.myfitness.exercise.domain.exception.ExerciseRuleException;
import com.myfitness.nutrition.application.exception.NutritionAccessException;
import com.myfitness.nutrition.application.exception.NutritionNotFoundException;
import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import com.myfitness.routine.application.exception.RoutineAccessException;
import com.myfitness.routine.application.exception.RoutineNotFoundException;
import com.myfitness.routine.domain.exception.RoutineRuleException;
import com.myfitness.workout.application.exception.WorkoutAccessException;
import com.myfitness.workout.application.exception.WorkoutNotFoundException;
import com.myfitness.workout.domain.exception.WorkoutRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkoutNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(WorkoutNotFoundException exception) {
        return ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(RoutineNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleRoutineNotFound(RoutineNotFoundException exception) {
        return ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(BodyRecordNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleBodyRecordNotFound(BodyRecordNotFoundException exception) {
        return ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(NutritionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNutritionNotFound(NutritionNotFoundException exception) {
        return ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleExerciseNotFound(ExerciseNotFoundException exception) {
        return ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(WorkoutAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccess(WorkoutAccessException exception) {
        return ApiErrorResponse.of("FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(RoutineAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleRoutineAccess(RoutineAccessException exception) {
        return ApiErrorResponse.of("FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(BodyRecordAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleBodyRecordAccess(BodyRecordAccessException exception) {
        return ApiErrorResponse.of("FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(NutritionAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleNutritionAccess(NutritionAccessException exception) {
        return ApiErrorResponse.of("FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(WorkoutRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleWorkoutRule(WorkoutRuleException exception) {
        return ApiErrorResponse.of("WORKOUT_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler(RoutineRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleRoutineRule(RoutineRuleException exception) {
        return ApiErrorResponse.of("ROUTINE_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler(BodyRecordRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBodyRecordRule(BodyRecordRuleException exception) {
        return ApiErrorResponse.of("BODY_RECORD_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler(NutritionRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleNutritionRule(NutritionRuleException exception) {
        return ApiErrorResponse.of("NUTRITION_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler(ExerciseRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleExerciseRule(ExerciseRuleException exception) {
        return ApiErrorResponse.of("EXERCISE_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ApiErrorResponse.of("INVALID_REQUEST", message);
    }

    @ExceptionHandler(AiConversationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleAiConversationNotFound(
            AiConversationNotFoundException exception) {
        return ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(AiConversationAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAiConversationAccess(
            AiConversationAccessException exception) {
        return ApiErrorResponse.of("FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler(AiRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleAiRule(AiRuleException exception) {
        return ApiErrorResponse.of(
                "AI_RULE_VIOLATION",
                exception.getMessage());
    }

    @ExceptionHandler(AiPolicyUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse handleAiPolicyUnavailable(AiPolicyUnavailableException exception) {
        return ApiErrorResponse.of("AI_POLICY_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(AiProviderUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse handleAiProviderUnavailable(
            AiProviderUnavailableException exception) {
        return ApiErrorResponse.of(
                "AI_PROVIDER_UNAVAILABLE",
                exception.getMessage());
    }

    @ExceptionHandler(InvalidFoodPhotoException.class)
    public ResponseEntity<ApiErrorResponse> handleFoodPhoto(
            InvalidFoodPhotoException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        };
        String code = switch (exception.getReason()) {
            case INVALID -> "INVALID_FOOD_PHOTO";
            case TOO_LARGE -> "PAYLOAD_TOO_LARGE";
            case UNSUPPORTED -> "UNSUPPORTED_MEDIA_TYPE";
        };
        return ResponseEntity.status(status).body(ApiErrorResponse.of(code, exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiErrorResponse handleUploadSize(MaxUploadSizeExceededException exception) {
        return ApiErrorResponse.of("PAYLOAD_TOO_LARGE", "사진은 5 MiB 이하로 선택해주세요.");
    }
}
