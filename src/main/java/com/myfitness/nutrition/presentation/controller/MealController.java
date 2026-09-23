package com.myfitness.nutrition.presentation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.myfitness.nutrition.application.port.in.NutritionUseCase;
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
    private final NutritionUseCase nutritionUseCase;

    public MealController(
            NutritionUseCase nutritionUseCase) {
        this.nutritionUseCase = nutritionUseCase;
    }

    @GetMapping("/daily")
    public DailyNutritionResponse daily(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return DailyNutritionResponse.from(
                nutritionUseCase.daily(userId, date));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MealFoodResponse addItem(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody MealItemCreateRequest request) {
        return MealFoodResponse.from(
                nutritionUseCase.addMealItem(
                        userId,
                        request.mealDate(),
                        request.mealType(),
                        request.foodId(),
                        request.servings()));
    }

    @PatchMapping("/items/{itemId}")
    public MealFoodResponse updateItem(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody MealItemUpdateRequest request) {
        return MealFoodResponse.from(
                nutritionUseCase.updateMealItem(
                        userId,
                        itemId,
                        request.servings()));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long itemId) {
        nutritionUseCase.deleteMealItem(userId, itemId);
    }
}
