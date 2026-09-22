package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.service.NutritionApplicationService;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.presentation.dto.request.NutritionGoalUpsertRequest;
import com.myfitness.nutrition.presentation.dto.response.NutritionGoalResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition-goals")
public class NutritionGoalController {
    private final NutritionApplicationService nutritionApplicationService;

    public NutritionGoalController(
            NutritionApplicationService nutritionApplicationService) {
        this.nutritionApplicationService = nutritionApplicationService;
    }

    @GetMapping("/current")
    public NutritionGoalResponse current(
            @RequestHeader("X-User-Id") Long userId) {
        NutritionGoal goal =
                nutritionApplicationService.currentGoal(userId);
        return goal == null
                ? null
                : NutritionGoalResponse.from(goal);
    }

    @PutMapping("/current")
    public NutritionGoalResponse upsert(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody NutritionGoalUpsertRequest request) {
        return NutritionGoalResponse.from(
                nutritionApplicationService.upsertGoal(
                        userId,
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }
}
