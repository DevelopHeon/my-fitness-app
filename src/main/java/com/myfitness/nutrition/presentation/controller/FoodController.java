package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.port.in.NutritionUseCase;
import com.myfitness.nutrition.presentation.dto.request.FoodUpsertRequest;
import com.myfitness.nutrition.presentation.dto.response.FoodResponse;
import com.myfitness.nutrition.presentation.dto.response.FoodSuggestionsResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final NutritionUseCase nutritionUseCase;

    public FoodController(
            NutritionUseCase nutritionUseCase) {
        this.nutritionUseCase = nutritionUseCase;
    }

    @GetMapping
    public List<FoodResponse> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String query) {
        return nutritionUseCase.listFoods(userId, query)
                .stream()
                .map(FoodResponse::from)
                .toList();
    }

    @GetMapping("/suggestions")
    public FoodSuggestionsResponse suggestions(
            @RequestHeader("X-User-Id") Long userId) {
        return FoodSuggestionsResponse.from(
                nutritionUseCase.suggestions(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FoodUpsertRequest request) {
        return FoodResponse.from(
                nutritionUseCase.createFood(
                        userId,
                        request.name(),
                        request.servingAmount(),
                        request.servingUnit(),
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }

    @PutMapping("/{foodId}")
    public FoodResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long foodId,
            @Valid @RequestBody FoodUpsertRequest request) {
        return FoodResponse.from(
                nutritionUseCase.updateFood(
                        userId,
                        foodId,
                        request.name(),
                        request.servingAmount(),
                        request.servingUnit(),
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }

    @DeleteMapping("/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long foodId) {
        nutritionUseCase.deleteFood(userId, foodId);
    }
}
