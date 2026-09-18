package com.myfitness.nutrition.controller;

import com.myfitness.nutrition.dto.request.MealItemCreateRequest;
import com.myfitness.nutrition.dto.request.MealItemUpdateRequest;
import com.myfitness.nutrition.dto.response.DailyNutritionResponse;
import com.myfitness.nutrition.dto.response.MealFoodResponse;
import com.myfitness.nutrition.service.NutritionApplicationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
public class MealController {
    private final NutritionApplicationService nutritionApplicationService;

    public MealController(
            NutritionApplicationService nutritionApplicationService) {
        this.nutritionApplicationService = nutritionApplicationService;
    }

    @GetMapping("/daily")
    public DailyNutritionResponse daily(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return nutritionApplicationService.daily(userId, date);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MealFoodResponse addItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MealItemCreateRequest request) {
        return nutritionApplicationService.addMealItem(userId, request);
    }

    @PatchMapping("/items/{itemId}")
    public MealFoodResponse updateItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody MealItemUpdateRequest request) {
        return nutritionApplicationService.updateMealItem(
                userId,
                itemId,
                request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {
        nutritionApplicationService.deleteMealItem(userId, itemId);
    }
}
