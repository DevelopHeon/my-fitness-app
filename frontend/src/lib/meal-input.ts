import type { MealType } from "./nutrition-api";

export type MealDraft = { foodName: string; caloriesPerServing: number; servingDescription: string } & ReturnType<typeof mealDefaults>;

export function mealDefaults(now = new Date()): { mealDate: string; mealType: MealType } {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", hourCycle: "h23",
  }).formatToParts(now);
  const part = (name: string) => parts.find((value) => value.type === name)!.value;
  const hour = Number(part("hour"));
  return {
    mealDate: `${part("year")}-${part("month")}-${part("day")}`,
    mealType: hour >= 5 && hour < 10 ? "BREAKFAST" : hour >= 10 && hour < 15 ? "LUNCH"
      : hour >= 15 && hour < 21 ? "DINNER" : "SNACK",
  };
}

export function optionalNutrient(value: string): number | null {
  return value.trim() === "" ? null : Number(value);
}
