"use client";

import { ChangeEvent, useEffect, useState } from "react";
import { DailyNutrition, MealBatchInput, MealFood, NutritionGoalInput, nutritionApi } from "@/lib/nutrition-api";
import { MealDraft, mealDefaults } from "@/lib/meal-input";
import MealRecordForm, { MealFormSource } from "./meal-record-form";
import NutritionGoalForm from "./nutrition-goal-form";
import NutritionDailyView from "./nutrition-daily-view";
import NutritionCalendarView from "./nutrition-calendar-view";
import { buttonClass, inputClass } from "./nutrition-fields";

type Props = {
  onSelectedDateChange?: (date: string) => void;
  initialDraft?: MealDraft | null;
  onDraftConsumed?: () => void;
};

export default function NutritionScreen({ onSelectedDateChange, initialDraft, onDraftConsumed }: Props) {
  const [date, setDate] = useState(() => mealDefaults().mealDate);
  const [calendarMonth, setCalendarMonth] = useState(() => mealDefaults().mealDate.slice(0, 7));
  const [view, setView] = useState<"daily" | "calendar">("daily");
  const [daily, setDaily] = useState<DailyNutrition | null>(null);
  const [form, setForm] = useState<MealFormSource | null>(() => initialDraft ? { draft: initialDraft } : null);
  const [formKey, setFormKey] = useState(0);
  const [goalOpen, setGoalOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    if (view !== "daily" || form !== null) return;
    let cancelled = false;
    onSelectedDateChange?.(date);
    async function loadDaily() {
      setLoading(true);
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
  }, [date, view, form, revision, onSelectedDateChange]);

  function selectDate(nextDate: string) {
    if (!nextDate) return;
    setDate(nextDate);
    setCalendarMonth(nextDate.slice(0, 7));
    setView("daily");
    setLoading(true);
    setError(null);
  }

  function openForm(source: MealFormSource = {}) {
    setFormKey((key) => key + 1);
    setError(null);
    setForm(source);
  }

  function selectPhoto(event: ChangeEvent<HTMLInputElement>) {
    const photo = event.target.files?.[0];
    event.target.value = "";
    if (photo) openForm({ photo });
  }

  function closeForm() {
    setForm(null);
    setError(null);
    onDraftConsumed?.();
  }

  async function saveMeal(input: MealBatchInput, id?: number) {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      if (id !== undefined) {
        await nutritionApi.updateMealItem(id, { mealDate: input.mealDate, mealType: input.mealType, ...input.items[0] });
      } else {
        await nutritionApi.addMealItems(input);
      }
      setDate(input.mealDate);
      setCalendarMonth(input.mealDate.slice(0, 7));
      setView("daily");
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
    setError(null);
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
    setError(null);
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

  const errorNotice = error && <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>;
  const pageClass = "mx-auto max-w-3xl space-y-5 px-4 py-6 pb-24 sm:px-6";

  if (form) return (
    <main className={pageClass}>
      {errorNotice}
      <MealRecordForm key={formKey} source={form} busy={busy} onSave={saveMeal} onCancel={closeForm} />
    </main>
  );

  if (goalOpen) return (
    <main className={pageClass}>
      <button type="button" className={buttonClass} disabled={busy} onClick={() => { setGoalOpen(false); setError(null); }}>돌아가기</button>
      {errorNotice}
      <NutritionGoalForm goal={daily?.goal ?? null} busy={busy} onSave={saveGoal} />
    </main>
  );

  return (
    <main className={pageClass}>
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-xl font-bold">식단 기록</h1>
        <div className="flex flex-wrap justify-end gap-2">
          <label className={buttonClass + " cursor-pointer"}>
            사진으로 식단 입력
            <input type="file" accept="image/jpeg,image/png" disabled={busy} className="sr-only" onChange={selectPhoto} />
          </label>
          <button type="button" className={buttonClass + " bg-zinc-950 text-white"} disabled={busy} onClick={() => openForm()}>식단 추가</button>
        </div>
      </div>
      <div className="grid grid-cols-2 rounded-2xl bg-zinc-100 p-1">
        {(["daily", "calendar"] as const).map((next) => (
          <button key={next} type="button" aria-pressed={view === next}
            onClick={() => { setView(next); setError(null); }}
            className={`rounded-xl px-4 py-2.5 text-sm font-semibold ${view === next ? "bg-white shadow-sm" : "text-zinc-500"}`}>
            {next === "daily" ? "일별 조회" : "캘린더 조회"}
          </button>
        ))}
      </div>
      {errorNotice}
      {view === "calendar" ? <NutritionCalendarView selectedDate={date} month={calendarMonth} onMonthChange={setCalendarMonth} revision={revision} onSelectDate={selectDate} /> : <>
        <label className="block text-sm">조회 날짜
          <input type="date" value={date} max={mealDefaults().mealDate} className={inputClass}
            onChange={(event) => selectDate(event.target.value)} />
        </label>
        {loading ? <p role="status">식단을 불러오는 중이에요.</p> : daily && <NutritionDailyView
          daily={daily} busy={busy} onEditGoal={() => setGoalOpen(true)}
          onEdit={(item) => openForm({ item })} onRemove={(item) => void removeMeal(item)} />}
      </>}
    </main>
  );
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "요청을 처리하지 못했어요.";
}
