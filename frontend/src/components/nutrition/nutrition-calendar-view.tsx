"use client";

import { useEffect, useState } from "react";
import { NutritionCalendarDay, nutritionApi } from "@/lib/nutrition-api";
import { mealDefaults } from "@/lib/meal-input";
import { localDateString } from "@/lib/input-utils";
import { buttonClass, mealTypes } from "./nutrition-fields";

export default function NutritionCalendarView({ selectedDate, month, onMonthChange, revision, onSelectDate }: {
  selectedDate: string;
  month: string;
  onMonthChange: (month: string) => void;
  revision: number;
  onSelectDate: (date: string) => void;
}) {
  const [result, setResult] = useState<{
    month: string; revision: number; days: NutritionCalendarDay[]; error: string | null;
  } | null>(null);
  const loaded = result?.month === month && result.revision === revision;
  const days = loaded ? result.days : [];
  const error = loaded ? result.error : null;
  const loading = !loaded;
  const today = mealDefaults().mealDate;

  useEffect(() => {
    let cancelled = false;
    nutritionApi.getCalendar(month).then((days) => {
      if (!cancelled) setResult({ month, revision, days, error: null });
    }).catch((caught) => {
      if (!cancelled) setResult({ month, revision, days: [],
        error: caught instanceof Error ? caught.message : "월간 식단을 불러오지 못했어요." });
    });
    return () => { cancelled = true; };
  }, [month, revision]);

  const [year, monthNumber] = month.split("-").map(Number);
  const first = new Date(year, monthNumber - 1, 1);
  const count = new Date(year, monthNumber, 0).getDate();
  const byDate = new Map(days.map((day) => [day.date, day]));
  const cells = [...Array.from({ length: first.getDay() }, () => null), ...Array.from({ length: count }, (_, index) => index + 1)];

  function moveMonth(offset: number) {
    onMonthChange(localDateString(new Date(year, monthNumber - 1 + offset, 1)).slice(0, 7));
  }

  return (
    <section className="space-y-4 rounded-2xl border border-zinc-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <button className={buttonClass} aria-label="이전 달" disabled={loading} onClick={() => moveMonth(-1)}>←</button>
        <div className="text-center">
          <h2 className="font-bold">{year}년 {monthNumber}월</h2>
          <button className="text-xs text-zinc-500" onClick={() => onMonthChange(today.slice(0, 7))}>이번 달</button>
        </div>
        <button className={buttonClass} aria-label="다음 달" disabled={loading || month >= today.slice(0, 7)} onClick={() => moveMonth(1)}>→</button>
      </div>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {loading && <p role="status" className="text-sm text-zinc-500">월간 식단을 불러오는 중이에요.</p>}
      <div className="grid grid-cols-7 gap-1">
        {["일", "월", "화", "수", "목", "금", "토"].map((label) => <div key={label} className="py-2 text-center text-xs text-zinc-500">{label}</div>)}
        {cells.map((day, index) => {
          if (day === null) return <div key={`blank-${index}`} />;
          const date = `${month}-${String(day).padStart(2, "0")}`;
          const summary = byDate.get(date);
          const selected = date === selectedDate;
          return (
            <button key={date} type="button" disabled={date > today || loading}
              aria-label={`${date}${summary ? ` ${summary.calories} kcal` : " 기록 없음"}`}
              aria-pressed={selected} onClick={() => onSelectDate(date)}
              className={`min-h-24 min-w-0 rounded-xl border p-1 sm:p-1.5 text-left disabled:opacity-40 ${selected ? "border-zinc-950 bg-zinc-950 text-white" : "border-zinc-200"}`}>
              <span className={`text-xs font-semibold ${date === today && !selected ? "underline" : ""}`}>{day}</span>
              {summary && <>
                <p className="mt-2 break-all text-[10px]"><span className="block">{summary.calories.toLocaleString()}</span>kcal</p>
                <p className="mt-1 text-[10px]">{mealTypes.filter((type) => summary.mealTypes.includes(type.value)).map((type) => <span key={type.value} className="block">{type.label}</span>)}</p>
              </>}
            </button>
          );
        })}
      </div>
      <p className="text-xs text-zinc-500">날짜를 선택하면 일별 식단으로 이동합니다.</p>
    </section>
  );
}
