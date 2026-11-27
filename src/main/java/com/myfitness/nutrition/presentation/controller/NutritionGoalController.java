package com.myfitness.nutrition.presentation.controller;

import com.myfitness.nutrition.application.port.in.NutritionGoalView;
import com.myfitness.nutrition.application.port.in.NutritionPort;
import com.myfitness.nutrition.presentation.dto.request.NutritionGoalUpsertRequest;
import com.myfitness.nutrition.presentation.dto.response.NutritionGoalResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nutrition-goals")
public class NutritionGoalController {
    private final NutritionPort nutritionPort;

    public NutritionGoalController(
            NutritionPort nutritionPort) {
        this.nutritionPort = nutritionPort;
    }

    @GetMapping("/current")
    public NutritionGoalResponse current(
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        NutritionGoalView goal =
                nutritionPort.currentGoal(userId);
        return goal == null
                ? null
                : NutritionGoalResponse.from(goal);
    }

    @PutMapping("/current")
    public NutritionGoalResponse upsert(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @Valid @RequestBody NutritionGoalUpsertRequest request) {
        return NutritionGoalResponse.from(
                nutritionPort.upsertGoal(
                        userId,
                        request.calories(),
                        request.carbohydrateGrams(),
                        request.proteinGrams(),
                        request.fatGrams()));
    }
}
