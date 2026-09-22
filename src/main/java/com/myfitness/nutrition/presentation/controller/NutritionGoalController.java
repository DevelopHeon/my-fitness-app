package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.port.in.NutritionUseCase;
import com.myfitness.nutrition.domain.model.NutritionGoal;
import com.myfitness.nutrition.presentation.dto.request.NutritionGoalUpsertRequest;
import com.myfitness.nutrition.presentation.dto.response.NutritionGoalResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition-goals")
public class NutritionGoalController {
    private final NutritionUseCase nutritionUseCase;

    public NutritionGoalController(
            NutritionUseCase nutritionUseCase) {
        this.nutritionUseCase = nutritionUseCase;
    }

    @GetMapping("/current")
    public NutritionGoalResponse current(
            @RequestHeader("X-User-Id") Long userId) {
        NutritionGoal goal =
                nutritionUseCase.currentGoal(userId);
        return goal == null
                ? null
                : NutritionGoalResponse.from(goal);
    }

    @PutMapping("/current")
    public NutritionGoalResponse upsert(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody NutritionGoalUpsertRequest request) {
        return NutritionGoalResponse.from(
                nutritionUseCase.upsertGoal(
                        userId,
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }
}
