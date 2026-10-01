"use client";

import { useEffect, useState } from "react";
import { DailyNutrition, MealFood, MealInput, NutritionGoalInput, nutritionApi } from "@/lib/nutrition-api";
import { MealDraft, mealDefaults } from "@/lib/meal-input";
import MealRecordForm, { MealFormSource } from "./meal-record-form";
import NutritionGoalForm from "./nutrition-goal-form";
import NutritionSummary from "./nutrition-summary";
import { buttonClass, inputClass, mealTypes } from "./nutrition-fields";

type Props = {
  onSelectedDateChange?: (date: string) => void;
  initialDraft?: MealDraft | null;
  onDraftConsumed?: () => void;
  onAnalyzePhoto: () => void;
};

export default function NutritionScreen({ onSelectedDateChange, initialDraft, onDraftConsumed, onAnalyzePhoto }: Props) {
  const [date, setDate] = useState(() => initialDraft?.mealDate ?? mealDefaults().mealDate);
  const [daily, setDaily] = useState<DailyNutrition | null>(null);
  const [form, setForm] = useState<MealFormSource | null>(() => initialDraft ? { draft: initialDraft } : null);
  const [formKey, setFormKey] = useState(0);
  const [goalOpen, setGoalOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let cancelled = false;
    onSelectedDateChange?.(date);

    async function loadDaily() {
      try {
        const result = await nutritionApi.getDaily(date);
        if (!cancelled) setDaily(result);
      } catch (caught) {
        if (!cancelled) {
          setDaily(null);
          setError(errorMessage(caught));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadDaily();
    return () => { cancelled = true; };
  }, [date, revision, onSelectedDateChange]);

  function selectDate(nextDate: string) {
    if (!nextDate) return;
    setDate(nextDate);
    setLoading(true);
    setError(null);
  }

  function openForm(item?: MealFood) {
    setFormKey((key) => key + 1);
    setError(null);
    setForm({ item });
  }

  function closeForm() {
    setForm(null);
    onDraftConsumed?.();
  }

  async function saveMeal(input: MealInput, id?: number) {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      if (id !== undefined) {
        await nutritionApi.updateMealItem(id, input);
      } else {
        await nutritionApi.addMealItem(input);
      }
      setDate(input.mealDate);
      setRevision((value) => value + 1);
      closeForm();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function removeMeal(item: MealFood) {
    if (busy || !window.confirm("이 식단 기록을 삭제할까요?")) return;
    setBusy(true);
    try {
      await nutritionApi.deleteMealItem(item.id);
      setRevision((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function saveGoal(input: NutritionGoalInput) {
    if (busy) return;
    setBusy(true);
    try {
      await nutritionApi.upsertGoal(input);
      setGoalOpen(false);
      setRevision((value) => value + 1);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto max-w-3xl space-y-5 px-4 py-6 pb-24 sm:px-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-xl font-bold">식단 기록</h1>
        <div className="flex flex-wrap justify-end gap-2">
          <button type="button" className={buttonClass} disabled={busy} onClick={onAnalyzePhoto}>
            사진으로 식단 입력
          </button>
          <button className={buttonClass + " bg-zinc-950 text-white"} disabled={busy} onClick={() => openForm()}>
            식단 추가
          </button>
        </div>
      </div>
      <label className="block text-sm">
        조회 날짜
        <input
          type="date"
          value={date}
          max={mealDefaults().mealDate}
          className={inputClass}
          onChange={(event) => selectDate(event.target.value)}
        />
      </label>
      {error && <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {loading ? <p role="status">식단을 불러오는 중이에요.</p> : daily && (
        <>
          <NutritionSummary daily={daily} busy={busy} onEditGoal={() => setGoalOpen(!goalOpen)} />
          {daily.meals.map((meal) => (
            <section key={meal.mealType} className="rounded-2xl border border-zinc-200 bg-white p-4">
              <h2 className="font-bold">{mealTypes.find((type) => type.value === meal.mealType)?.label}</h2>
              <p className="mt-1 text-sm text-zinc-500">{meal.total.calories} kcal</p>
              {meal.items.length === 0 ? (
                <p className="mt-3 text-sm text-zinc-400">등록한 식단이 없어요.</p>
              ) : meal.items.map((item) => (
                <div key={item.id} className="mt-3 flex items-center gap-2 border-t border-zinc-100 pt-3">
                  <div className="min-w-0 flex-1">
                    <p className="break-words font-medium">{item.foodName}</p>
                    <p className="text-sm text-zinc-500">{item.calories} kcal</p>
                  </div>
                  <button className={buttonClass} disabled={busy} onClick={() => openForm(item)}>수정</button>
                  <button className={buttonClass + " text-red-600"} disabled={busy} onClick={() => void removeMeal(item)}>삭제</button>
                </div>
              ))}
            </section>
          ))}
        </>
      )}
      {goalOpen && <NutritionGoalForm goal={daily?.goal ?? null} busy={busy} onSave={saveGoal} />}
      {form && <MealRecordForm key={formKey} source={form} busy={busy} onSave={saveMeal} onCancel={closeForm} />}
    </main>
  );
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "요청을 처리하지 못했어요.";
}
