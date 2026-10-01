package com.myfitness.ai.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** 검증된 분석 결과만 보존한다. 원본 사진이나 모델의 자유 문장은 포함하지 않는다. */
public record FoodPhotoAnalysis(Status status, List<Item> items) {
    public enum Status { FOOD, NOT_FOOD, UNCERTAIN }

    public FoodPhotoAnalysis {
        if (status == null || items == null || items.size() > 5
                || (status == Status.FOOD) != !items.isEmpty()) {
            throw new IllegalArgumentException("INVALID_FOOD_PHOTO_RESULT");
        }
        items = List.copyOf(items);
    }

    public record Item(String foodName, String servingDescription, BigDecimal caloriesPerServing) {
        public Item {
            if (foodName == null || foodName.isBlank() || foodName.length() > 100
                    || servingDescription == null || servingDescription.isBlank() || servingDescription.length() > 200
                    || caloriesPerServing == null || caloriesPerServing.signum() < 0
                    || caloriesPerServing.compareTo(new BigDecimal("999999.99")) > 0
                    || caloriesPerServing.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("INVALID_FOOD_PHOTO_ITEM");
            }
            foodName = foodName.trim();
            servingDescription = servingDescription.trim();
        }
    }
}
