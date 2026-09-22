package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.service.NutritionApplicationService;
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
    private final NutritionApplicationService nutritionApplicationService;

    public FoodController(
            NutritionApplicationService nutritionApplicationService) {
        this.nutritionApplicationService = nutritionApplicationService;
    }

    @GetMapping
    public List<FoodResponse> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String query) {
        return nutritionApplicationService.listFoods(userId, query)
                .stream()
                .map(FoodResponse::from)
                .toList();
    }

    @GetMapping("/suggestions")
    public FoodSuggestionsResponse suggestions(
            @RequestHeader("X-User-Id") Long userId) {
        return FoodSuggestionsResponse.from(
                nutritionApplicationService.suggestions(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FoodUpsertRequest request) {
        return FoodResponse.from(
                nutritionApplicationService.createFood(
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
                nutritionApplicationService.updateFood(
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
        nutritionApplicationService.deleteFood(userId, foodId);
    }
}
