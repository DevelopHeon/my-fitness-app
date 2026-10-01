import { request } from "@/lib/api-client";

export type MealType = "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK";
export type NutritionTotals = {
  calories: number;
  carbohydrateGrams: number | null;
  proteinGrams: number | null;
  fatGrams: number | null;
};
export type MealInput = NutritionTotals & {
  mealDate: string;
  mealType: MealType;
  foodName: string;
};
export type MealBatchInput = {
  mealDate: string;
  mealType: MealType;
  items: (NutritionTotals & { foodName: string })[];
};
export type NutritionCalendarDay = { date: string; calories: number; mealTypes: MealType[] };
export type MealFood = MealInput & { id: number; createdAt: string; updatedAt: string };
export type MealSection = { mealType: MealType; items: MealFood[]; total: NutritionTotals };
export type NutritionGoal = {
  id: number;
  calories: number;
  carbohydrateGrams: number;
  proteinGrams: number;
  fatGrams: number;
  updatedAt: string;
};
export type NutritionGoalInput = Omit<NutritionGoal, "id" | "updatedAt">;
export type DailyNutrition = {
  date: string;
  goal: NutritionGoal | null;
  consumed: NutritionTotals;
  remaining: NutritionTotals | null;
  meals: MealSection[];
};
export const nutritionApi = {
  getDaily: (date: string) => request<DailyNutrition>("/api/meals/daily?date=" + encodeURIComponent(date)),
  getCalendar: (month: string) => request<NutritionCalendarDay[]>("/api/meals/calendar?month=" + encodeURIComponent(month)),
  addMealItems: (input: MealBatchInput) => request<MealFood[]>("/api/meals/items/batch", {
    method: "POST", body: JSON.stringify(input),
  }),
  addMealItem: (input: MealInput) => request<MealFood>("/api/meals/items", {
    method: "POST", body: JSON.stringify(input),
  }),
  updateMealItem: (id: number, input: MealInput) => request<MealFood>("/api/meals/items/" + id, {
    method: "PATCH", body: JSON.stringify(input),
  }),
  deleteMealItem: (id: number) => request<void>("/api/meals/items/" + id, { method: "DELETE" }),
  upsertGoal: (input: NutritionGoalInput) => request<NutritionGoal>("/api/nutrition-goals/current", {
    method: "PUT", body: JSON.stringify(input),
  }),
};
