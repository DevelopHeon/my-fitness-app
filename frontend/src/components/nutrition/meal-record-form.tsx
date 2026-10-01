"use client";

import { FormEvent, useState } from "react";
import { MealFood, MealInput, MealType } from "@/lib/nutrition-api";
import { MealDraft, mealDefaults, optionalNutrient } from "@/lib/meal-input";
import { buttonClass, inputClass, mealTypes, nutrients } from "./nutrition-fields";

export type MealFormSource = { item?: MealFood; draft?: MealDraft };

type MealFormValues = {
  mealDate: string;
  mealType: MealType;
  foodName: string;
  calories: string;
  carbohydrateGrams: string;
  proteinGrams: string;
  fatGrams: string;
};

type Props = {
  source: MealFormSource;
  busy: boolean;
  onSave: (input: MealInput, id?: number) => Promise<void>;
  onCancel: () => void;
};

function initialValues({ item, draft }: MealFormSource): MealFormValues {
  const defaults = mealDefaults();
  return {
    mealDate: item?.mealDate ?? draft?.mealDate ?? defaults.mealDate,
    mealType: item?.mealType ?? draft?.mealType ?? defaults.mealType,
    foodName: item?.foodName ?? draft?.foodName ?? "",
    calories: String(item?.calories ?? draft?.caloriesPerServing ?? ""),
    carbohydrateGrams: String(item?.carbohydrateGrams ?? ""),
    proteinGrams: String(item?.proteinGrams ?? ""),
    fatGrams: String(item?.fatGrams ?? ""),
  };
}

export default function MealRecordForm({ source, busy, onSave, onCancel }: Props) {
  const [values, setValues] = useState(() => initialValues(source));

  function changeField(key: keyof MealFormValues, value: string) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;

    const input: MealInput = {
      mealDate: values.mealDate,
      mealType: values.mealType,
      foodName: values.foodName.trim(),
      calories: Number(values.calories),
      carbohydrateGrams: optionalNutrient(values.carbohydrateGrams),
      proteinGrams: optionalNutrient(values.proteinGrams),
      fatGrams: optionalNutrient(values.fatGrams),
    };
    void onSave(input, source.item?.id);
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-2xl border border-zinc-300 bg-white p-4">
      <h2 className="font-bold">{source.item ? "식단 수정" : "식단 추가"}</h2>
      {source.draft?.servingDescription && (
        <p className="text-sm text-zinc-500">
          {source.draft.servingDescription} 추정값이에요. 먹은 양에 맞춰 칼로리를 수정한 뒤 저장해주세요.
        </p>
      )}
      <label className="block text-sm">
        날짜
        <input
          className={inputClass}
          type="date"
          required
          max={mealDefaults().mealDate}
          value={values.mealDate}
          onChange={(event) => changeField("mealDate", event.target.value)}
        />
      </label>
      <label className="block text-sm">
        식사 구분
        <select
          className={inputClass}
          value={values.mealType}
          onChange={(event) => changeField("mealType", event.target.value)}
        >
          {mealTypes.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <label className="block text-sm">
        음식명
        <input
          className={inputClass}
          autoFocus
          required
          maxLength={100}
          value={values.foodName}
          onChange={(event) => changeField("foodName", event.target.value)}
        />
      </label>
      <label className="block text-sm">
        섭취 칼로리 (kcal)
        <input
          className={inputClass}
          type="number"
          required
          min="0"
          max="999999.99"
          step="0.01"
          value={values.calories}
          onChange={(event) => changeField("calories", event.target.value)}
        />
      </label>
      <details className="rounded-xl border border-zinc-200 p-3">
        <summary className="cursor-pointer text-sm font-semibold">영양정보 추가 입력 (선택)</summary>
        <p className="mt-2 text-xs text-zinc-500">모르는 값은 비워두세요. 먹은 양 기준 g입니다.</p>
        {nutrients.slice(1).map(({ key, label, unit }) => (
          <label key={key} className="mt-3 block text-sm">
            {label} ({unit})
            <input
              className={inputClass}
              type="number"
              min="0"
              max="999999.99"
              step="0.01"
              value={values[key]}
              onChange={(event) => changeField(key, event.target.value)}
            />
          </label>
        ))}
      </details>
      <div className="flex gap-2">
        <button className={buttonClass + " bg-zinc-950 text-white"} disabled={busy}>
          {busy ? "저장 중..." : "저장"}
        </button>
        <button type="button" className={buttonClass} disabled={busy} onClick={onCancel}>취소</button>
      </div>
    </form>
  );
}
