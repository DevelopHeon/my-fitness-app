import { request } from "@/lib/api-client";

export type ServingUnit = "G" | "ML" | "COUNT";
export type MealType = "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK";

export type NutritionTotals = {
  calories: number;
  carbohydrateGrams: number;
  proteinGrams: number;
  fatGrams: number;
};

export type Food = {
  id: number;
  name: string;
  servingAmount: number;
  servingUnit: ServingUnit;
  calories: number;
  carbohydrateGrams: number;
  proteinGrams: number;
  fatGrams: number;
  createdAt: string;
  updatedAt: string;
};

export type FoodInput = {
  name: string;
  servingAmount: number;
  servingUnit: ServingUnit;
  calories: number;
  carbohydrateGrams: number;
  proteinGrams: number;
  fatGrams: number;
};

export type FoodSuggestions = {
  recent: Food[];
  frequent: Food[];
};

export type MealFood = {
  id: number;
  sourceFoodId: number;
  foodName: string;
  servingAmount: number;
  servingUnit: ServingUnit;
  caloriesPerServing: number;
  carbohydrateGramsPerServing: number;
  proteinGramsPerServing: number;
  fatGramsPerServing: number;
  servings: number;
  total: NutritionTotals;
};

export type MealSection = {
  mealType: MealType;
  items: MealFood[];
  total: NutritionTotals;
};

export type NutritionGoal = {
  id: number;
  calories: number;
  carbohydrateGrams: number;
  proteinGrams: number;
  fatGrams: number;
  updatedAt: string;
};

export type NutritionGoalInput = Omit<
  NutritionGoal,
  "id" | "updatedAt"
>;

export type DailyNutrition = {
  date: string;
  goal: NutritionGoal | null;
  consumed: NutritionTotals;
  remaining: NutritionTotals | null;
  meals: MealSection[];
};

export const nutritionApi = {
  getDaily: (date: string) =>
    request<DailyNutrition>(
      "/api/meals/daily?date=" + encodeURIComponent(date),
    ),

  listFoods: (query?: string) => {
    const suffix = query?.trim()
      ? "?query=" + encodeURIComponent(query.trim())
      : "";
    return request<Food[]>("/api/foods" + suffix);
  },

  getSuggestions: () =>
    request<FoodSuggestions>("/api/foods/suggestions"),

  createFood: (input: FoodInput) =>
    request<Food>("/api/foods", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  updateFood: (foodId: number, input: FoodInput) =>
    request<Food>("/api/foods/" + foodId, {
      method: "PUT",
      body: JSON.stringify(input),
    }),

  deleteFood: (foodId: number) =>
    request<void>("/api/foods/" + foodId, {
      method: "DELETE",
    }),

  addMealItem: (
    mealDate: string,
    mealType: MealType,
    foodId: number,
    servings: number,
  ) =>
    request<MealFood>("/api/meals/items", {
      method: "POST",
      body: JSON.stringify({
        mealDate,
        mealType,
        foodId,
        servings,
      }),
    }),

  updateMealItem: (itemId: number, servings: number) =>
    request<MealFood>("/api/meals/items/" + itemId, {
      method: "PATCH",
      body: JSON.stringify({ servings }),
    }),

  deleteMealItem: (itemId: number) =>
    request<void>("/api/meals/items/" + itemId, {
      method: "DELETE",
    }),

  upsertGoal: (input: NutritionGoalInput) =>
    request<NutritionGoal>("/api/nutrition-goals/current", {
      method: "PUT",
      body: JSON.stringify(input),
    }),
};
