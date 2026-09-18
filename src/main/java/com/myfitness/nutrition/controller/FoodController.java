package com.myfitness.nutrition.controller;

import com.myfitness.nutrition.dto.request.FoodUpsertRequest;
import com.myfitness.nutrition.dto.response.FoodResponse;
import com.myfitness.nutrition.dto.response.FoodSuggestionsResponse;
import com.myfitness.nutrition.service.NutritionApplicationService;
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
        return nutritionApplicationService.listFoods(userId, query);
    }

    @GetMapping("/suggestions")
    public FoodSuggestionsResponse suggestions(
            @RequestHeader("X-User-Id") Long userId) {
        return nutritionApplicationService.suggestions(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody FoodUpsertRequest request) {
        return nutritionApplicationService.createFood(userId, request);
    }

    @PutMapping("/{foodId}")
    public FoodResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long foodId,
            @Valid @RequestBody FoodUpsertRequest request) {
        return nutritionApplicationService.updateFood(
                userId,
                foodId,
                request);
    }

    @DeleteMapping("/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long foodId) {
        nutritionApplicationService.deleteFood(userId, foodId);
    }
}
