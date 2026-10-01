"use client";

import { FormEvent, useState } from "react";
import { NutritionGoal, NutritionGoalInput } from "@/lib/nutrition-api";
import { buttonClass, inputClass, nutrients } from "./nutrition-fields";

type Props = {
  goal: NutritionGoal | null;
  busy: boolean;
  onSave: (input: NutritionGoalInput) => Promise<void>;
};

export default function NutritionGoalForm({ goal, busy, onSave }: Props) {
  const [values, setValues] = useState(() => ({
    calories: String(goal?.calories ?? ""),
    carbohydrateGrams: String(goal?.carbohydrateGrams ?? ""),
    proteinGrams: String(goal?.proteinGrams ?? ""),
    fatGrams: String(goal?.fatGrams ?? ""),
  }));

  function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;

    void onSave({
      calories: Number(values.calories),
      carbohydrateGrams: Number(values.carbohydrateGrams),
      proteinGrams: Number(values.proteinGrams),
      fatGrams: Number(values.fatGrams),
    });
  }

  return (
    <form onSubmit={submit} className="space-y-3 rounded-2xl border bg-white p-4">
      <h2 className="font-bold">하루 영양 목표</h2>
      {nutrients.map(({ key, label, unit }) => (
        <label key={key} className="block text-sm">
          {label} ({unit})
          <input
            className={inputClass}
            type="number"
            step="0.01"
            min={key === "calories" ? "0.01" : "0"}
            max="999999.99"
            required
            value={values[key]}
            onChange={(event) => setValues((current) => ({ ...current, [key]: event.target.value }))}
          />
        </label>
      ))}
      <button className={buttonClass} disabled={busy}>목표 저장</button>
    </form>
  );
}
