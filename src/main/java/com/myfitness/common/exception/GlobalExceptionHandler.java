package com.myfitness.common.exception;

import com.myfitness.body.exception.BodyRecordAccessException;
import com.myfitness.body.exception.BodyRecordNotFoundException;
import com.myfitness.body.exception.BodyRecordRuleException;
import com.myfitness.nutrition.exception.NutritionAccessException;
import com.myfitness.nutrition.exception.NutritionNotFoundException;
import com.myfitness.nutrition.exception.NutritionRuleException;
import com.myfitness.routine.exception.RoutineAccessException;
import com.myfitness.routine.exception.RoutineNotFoundException;
import com.myfitness.routine.exception.RoutineRuleException;
import com.myfitness.workout.exception.WorkoutAccessException;
import com.myfitness.workout.exception.WorkoutNotFoundException;
import com.myfitness.workout.exception.WorkoutRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ApiErrorResponse.of("INVALID_REQUEST", message);
    }
}
