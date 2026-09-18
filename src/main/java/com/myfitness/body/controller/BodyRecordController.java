package com.myfitness.body.controller;

import com.myfitness.body.dto.request.BodyRecordUpsertRequest;
import com.myfitness.body.dto.response.BodyRecordResponse;
import com.myfitness.body.dto.response.BodyTrendResponse;
import com.myfitness.body.service.BodyRecordApplicationService;
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
        return bodyRecordApplicationService.create(userId, request);
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
        return bodyRecordApplicationService.list(userId, from, to);
    }

    @GetMapping("/{bodyRecordId}")
    public BodyRecordResponse get(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bodyRecordId) {
        return bodyRecordApplicationService.get(userId, bodyRecordId);
    }

    @PutMapping("/{bodyRecordId}")
    public BodyRecordResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bodyRecordId,
            @Valid @RequestBody BodyRecordUpsertRequest request) {
        return bodyRecordApplicationService.update(
                userId, bodyRecordId, request);
    }

    @DeleteMapping("/{bodyRecordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long bodyRecordId) {
        bodyRecordApplicationService.delete(userId, bodyRecordId);
    }

    @GetMapping("/trend")
    public BodyTrendResponse trend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "30") int days) {
        return bodyRecordApplicationService.trend(userId, days);
    }
}
