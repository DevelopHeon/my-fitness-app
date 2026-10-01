"use client";

import { FormEvent, useRef, useState } from "react";
import { FoodPhotoItem } from "@/lib/ai-api";
import { MealBatchInput, MealFood } from "@/lib/nutrition-api";
import { MealDraft, mealDefaults, optionalNutrient } from "@/lib/meal-input";
import MealPhotoInput from "./meal-photo-input";
import { buttonClass, inputClass, mealTypes, nutrients } from "./nutrition-fields";

export type MealFormSource = { item?: MealFood; draft?: MealDraft; photo?: File };

type FoodValues = {
  foodName: string;
  calories: string;
  carbohydrateGrams: string;
  proteinGrams: string;
  fatGrams: string;
  servingDescription: string;
};

function foodValues(item?: MealFood, photo?: FoodPhotoItem): FoodValues {
  return {
    foodName: item?.foodName ?? photo?.foodName ?? "",
    calories: String(item?.calories ?? photo?.caloriesPerServing ?? ""),
    carbohydrateGrams: String(item?.carbohydrateGrams ?? ""),
    proteinGrams: String(item?.proteinGrams ?? ""),
    fatGrams: String(item?.fatGrams ?? ""),
    servingDescription: photo?.servingDescription ?? "",
  };
}

export default function MealRecordForm({ source, busy, onSave, onCancel }: {
  source: MealFormSource;
  busy: boolean;
  onSave: (input: MealBatchInput, id?: number) => Promise<void>;
  onCancel: () => void;
}) {
  const [date, setDate] = useState(() => source.item?.mealDate ?? source.draft?.mealDate ?? mealDefaults().mealDate);
  const [type, setType] = useState(() => source.item?.mealType ?? source.draft?.mealType ?? mealDefaults().mealType);
  const [foods, setFoods] = useState(() => source.draft
    ? source.draft.items.map((item) => foodValues(undefined, item))
    : source.photo ? [] : [foodValues(source.item)]);
  const [analyzing, setAnalyzing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const pending = useRef(false);
  const locked = busy || analyzing || submitting;

  function changeFood(index: number, key: keyof FoodValues, value: string) {
    setFoods((current) => current.map((food, position) => position === index ? { ...food, [key]: value } : food));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (locked || pending.current || foods.length === 0) return;
    pending.current = true;
    setSubmitting(true);
    try {
      await onSave({
        mealDate: date,
        mealType: type,
        items: foods.map((food) => ({
          foodName: food.foodName.trim(),
          calories: Number(food.calories),
          carbohydrateGrams: optionalNutrient(food.carbohydrateGrams),
          proteinGrams: optionalNutrient(food.proteinGrams),
          fatGrams: optionalNutrient(food.fatGrams),
        })),
      }, source.item?.id);
    } finally {
      pending.current = false;
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={(event) => void submit(event)} className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">{source.item ? "식단 수정" : "식단 등록"}</h1>
        <button type="button" className={buttonClass} disabled={locked} onClick={onCancel}>돌아가기</button>
      </div>
      {!source.item && <MealPhotoInput initialPhoto={source.photo} disabled={locked}
        onBusyChange={setAnalyzing} onAnalyzed={(items) => setFoods(items.map((item) => foodValues(undefined, item)))} />}
      <fieldset disabled={locked} className="min-w-0 space-y-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label className="block min-w-0 text-sm">날짜
            <input className={inputClass} type="date" required max={mealDefaults().mealDate}
              value={date} onChange={(event) => setDate(event.target.value)} />
          </label>
          <label className="block min-w-0 text-sm">식사 구분
            <select className={inputClass} value={type} onChange={(event) => setType(event.target.value as typeof type)}>
              {mealTypes.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
        </div>
        {foods.map((food, index) => (
          <section key={index} className="space-y-3 rounded-2xl border border-zinc-200 bg-white p-4">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold">음식 {index + 1}</h2>
              {!source.item && <button type="button" aria-label={`음식 ${index + 1} 제외`}
                className="text-sm text-zinc-500" onClick={() => setFoods((current) => current.filter((_, position) => position !== index))}>제외</button>}
            </div>
            {food.servingDescription && <p className="text-xs text-zinc-500">{food.servingDescription} 기준 추정값입니다. 먹은 양에 맞춰 수정해주세요.</p>}
            <label className="block text-sm">음식명
              <input className={inputClass} required maxLength={100} value={food.foodName}
                onChange={(event) => changeFood(index, "foodName", event.target.value)} />
            </label>
            <label className="block text-sm">섭취 칼로리 (kcal)
              <input className={inputClass} type="number" required min="0" max="999999.99" step="0.01"
                value={food.calories} onChange={(event) => changeFood(index, "calories", event.target.value)} />
            </label>
            <details className="rounded-xl border border-zinc-200 p-3">
              <summary className="cursor-pointer text-sm font-semibold">영양정보 추가 입력 (선택)</summary>
              <p className="mt-2 text-xs text-zinc-500">모르는 값은 비워두세요. 먹은 양 기준 g입니다.</p>
              {nutrients.slice(1).map(({ key, label, unit }) => (
                <label key={key} className="mt-3 block text-sm">{label} ({unit})
                  <input className={inputClass} type="number" min="0" max="999999.99" step="0.01"
                    value={food[key]} onChange={(event) => changeFood(index, key, event.target.value)} />
                </label>
              ))}
            </details>
          </section>
        ))}
        {!source.item && <button type="button" className={buttonClass} disabled={foods.length >= 20}
          onClick={() => setFoods((current) => [...current, foodValues()])}>음식 추가</button>}
      </fieldset>
      <div className="flex flex-col gap-3 rounded-xl border border-zinc-200 bg-white p-4 sm:flex-row sm:items-center">
        <p className="min-w-0 break-words text-sm font-semibold">합계 {foods.reduce((total, food) => total + Number(food.calories || 0), 0).toLocaleString("ko-KR", { maximumFractionDigits: 2 })} kcal</p>
        <button type="submit" className={buttonClass + " self-start bg-zinc-950 text-white"} disabled={locked || foods.length === 0}>
          {busy || submitting ? "저장 중…" : source.item ? "수정 저장" : `음식 ${foods.length}개 저장`}
        </button>
      </div>
    </form>
  );
}
