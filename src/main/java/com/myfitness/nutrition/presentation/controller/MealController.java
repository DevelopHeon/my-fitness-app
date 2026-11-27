package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.port.in.NutritionPort;
import com.myfitness.nutrition.presentation.dto.request.MealItemCreateRequest;
import com.myfitness.nutrition.presentation.dto.request.MealItemUpdateRequest;
import com.myfitness.nutrition.presentation.dto.request.MealItemsCreateRequest;
import com.myfitness.nutrition.presentation.dto.response.DailyNutritionResponse;
import com.myfitness.nutrition.presentation.dto.response.MealFoodResponse;
import com.myfitness.nutrition.presentation.dto.response.NutritionCalendarDayResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
public class MealController {
    private final NutritionPort nutritionPort;

    public MealController(
            NutritionPort nutritionPort) {
        this.nutritionPort = nutritionPort;
    }

    @GetMapping("/daily")
    public DailyNutritionResponse daily(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return DailyNutritionResponse.from(
                nutritionPort.daily(userId, date));
    }

    @GetMapping("/calendar")
    public List<NutritionCalendarDayResponse> calendar(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return nutritionPort.calendar(userId, month).stream()
                .map(NutritionCalendarDayResponse::from)
                .toList();
    }

    @PostMapping("/items/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MealFoodResponse> addItems(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody MealItemsCreateRequest request) {
        return nutritionPort.addMealItems(userId, request.mealDate(), request.mealType(),
                        request.items().stream().map(MealItemsCreateRequest.ItemRequest::toCommand).toList())
                .stream()
                .map(MealFoodResponse::from)
                .toList();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MealFoodResponse addItem(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody MealItemCreateRequest request) {
        return MealFoodResponse.from(
                nutritionPort.addMealItem(
                        userId,
                        request.mealDate(),
                        request.mealType(),
                        request.foodName(),
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }

    @PatchMapping("/items/{itemId}")
    public MealFoodResponse updateItem(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody MealItemUpdateRequest request) {
        return MealFoodResponse.from(
                nutritionPort.updateMealItem(
                        userId,
                        itemId,
                        request.mealDate(),
                        request.mealType(),
                        request.foodName(),
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long itemId) {
        nutritionPort.deleteMealItem(userId, itemId);
    }
}
