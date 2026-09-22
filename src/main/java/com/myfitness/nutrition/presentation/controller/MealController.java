package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.service.NutritionApplicationService;
import com.myfitness.nutrition.presentation.dto.request.MealItemCreateRequest;
import com.myfitness.nutrition.presentation.dto.request.MealItemUpdateRequest;
import com.myfitness.nutrition.presentation.dto.response.DailyNutritionResponse;
import com.myfitness.nutrition.presentation.dto.response.MealFoodResponse;
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
        return DailyNutritionResponse.from(
                nutritionApplicationService.daily(userId, date));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MealFoodResponse addItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MealItemCreateRequest request) {
        return MealFoodResponse.from(
                nutritionApplicationService.addMealItem(
                        userId,
                        request.mealDate(),
                        request.mealType(),
                        request.foodId(),
                        request.servings()));
    }

    @PatchMapping("/items/{itemId}")
    public MealFoodResponse updateItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody MealItemUpdateRequest request) {
        return MealFoodResponse.from(
                nutritionApplicationService.updateMealItem(
                        userId,
                        itemId,
                        request.servings()));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {
        nutritionApplicationService.deleteMealItem(userId, itemId);
    }
}
