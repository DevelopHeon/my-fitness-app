package com.myfitness.nutrition.controller;

import com.myfitness.nutrition.dto.request.NutritionGoalUpsertRequest;
import com.myfitness.nutrition.dto.response.NutritionGoalResponse;
import com.myfitness.nutrition.service.NutritionApplicationService;
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
        return nutritionApplicationService.currentGoal(userId);
    }

    @PutMapping("/current")
    public NutritionGoalResponse upsert(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody NutritionGoalUpsertRequest request) {
        return nutritionApplicationService.upsertGoal(userId, request);
    }
}
