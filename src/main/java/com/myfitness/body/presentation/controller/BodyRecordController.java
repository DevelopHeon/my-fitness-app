package com.myfitness.body.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.myfitness.body.application.port.in.BodyRecordUseCase;
import com.myfitness.body.presentation.dto.request.BodyRecordUpsertRequest;
import com.myfitness.body.presentation.dto.response.BodyRecordResponse;
import com.myfitness.body.presentation.dto.response.BodyTrendResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/body-records")
public class BodyRecordController {
    private final BodyRecordUseCase bodyRecordUseCase;

    public BodyRecordController(
            BodyRecordUseCase bodyRecordUseCase) {
        this.bodyRecordUseCase = bodyRecordUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BodyRecordResponse create(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody BodyRecordUpsertRequest request) {
        return BodyRecordResponse.from(
                bodyRecordUseCase.create(
                        userId,
                        request.weightKg(),
                        request.bodyFatPercentage(),
                        request.skeletalMuscleKg(),
                        request.measuredAt(),
                        request.memo()));
    }

    @GetMapping
    public List<BodyRecordResponse> list(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to) {
        return bodyRecordUseCase.list(userId, from, to)
                .stream()
                .map(BodyRecordResponse::from)
                .toList();
    }

    @GetMapping("/{bodyRecordId}")
    public BodyRecordResponse get(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long bodyRecordId) {
        return BodyRecordResponse.from(
                bodyRecordUseCase.get(
                        userId, bodyRecordId));
    }

    @PutMapping("/{bodyRecordId}")
    public BodyRecordResponse update(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long bodyRecordId,
            @Valid @RequestBody BodyRecordUpsertRequest request) {
        return BodyRecordResponse.from(
                bodyRecordUseCase.update(
                        userId,
                        bodyRecordId,
                        request.weightKg(),
                        request.bodyFatPercentage(),
                        request.skeletalMuscleKg(),
                        request.measuredAt(),
                        request.memo()));
    }

    @DeleteMapping("/{bodyRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long bodyRecordId) {
        bodyRecordUseCase.delete(
                userId, bodyRecordId);
    }

    @GetMapping("/trend")
    public BodyTrendResponse trend(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam(defaultValue = "30") int days) {
        return BodyTrendResponse.from(
                bodyRecordUseCase.trend(userId, days));
    }
}
