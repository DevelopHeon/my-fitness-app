import { MealType } from "@/lib/nutrition-api";

export const mealTypes: { value: MealType; label: string }[] = [
  { value: "BREAKFAST", label: "아침" },
  { value: "LUNCH", label: "점심" },
  { value: "DINNER", label: "저녁" },
  { value: "SNACK", label: "간식" },
];

export const nutrients = [
  { key: "calories", label: "칼로리", unit: "kcal" },
  { key: "carbohydrateGrams", label: "탄수화물", unit: "g" },
  { key: "proteinGrams", label: "단백질", unit: "g" },
  { key: "fatGrams", label: "지방", unit: "g" },
] as const;

export const inputClass = "mt-1 block h-11 min-w-0 w-full max-w-full rounded-xl border border-zinc-300 bg-white px-3 py-2 text-sm";
export const buttonClass = "min-h-11 shrink-0 rounded-xl border border-zinc-200 px-3 py-2 text-sm font-semibold disabled:opacity-50";
