package com.myfitness.body.presentation.controller;

import com.myfitness.body.application.service.BodyRecordApplicationService;
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
    private final BodyRecordApplicationService bodyRecordApplicationService;

    public BodyRecordController(
            BodyRecordApplicationService bodyRecordApplicationService) {
        this.bodyRecordApplicationService = bodyRecordApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BodyRecordResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BodyRecordUpsertRequest request) {
        return BodyRecordResponse.from(
                bodyRecordApplicationService.create(
                        userId,
                        request.weightKg(),
                        request.bodyFatPercentage(),
                        request.skeletalMuscleKg(),
                        request.measuredAt(),
                        request.memo()));
    }

    @GetMapping
    public List<BodyRecordResponse> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to) {
        return bodyRecordApplicationService.list(userId, from, to)
                .stream()
                .map(BodyRecordResponse::from)
                .toList();
    }

    @GetMapping("/{bodyRecordId}")
    public BodyRecordResponse get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bodyRecordId) {
        return BodyRecordResponse.from(
                bodyRecordApplicationService.get(
                        userId, bodyRecordId));
    }

    @PutMapping("/{bodyRecordId}")
    public BodyRecordResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bodyRecordId,
            @Valid @RequestBody BodyRecordUpsertRequest request) {
        return BodyRecordResponse.from(
                bodyRecordApplicationService.update(
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
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bodyRecordId) {
        bodyRecordApplicationService.delete(
                userId, bodyRecordId);
    }

    @GetMapping("/trend")
    public BodyTrendResponse trend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "30") int days) {
        return BodyTrendResponse.from(
                bodyRecordApplicationService.trend(userId, days));
    }
}
