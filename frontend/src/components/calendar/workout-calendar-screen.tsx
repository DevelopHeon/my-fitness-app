"use client";

import { useEffect, useMemo, useState } from "react";
import { localDateString, todayString } from "@/lib/input-utils";
import { WorkoutCalendarDay, workoutApi } from "@/lib/workout-api";

type Props = {
  selectedDate: string;
  onSelectDate: (date: string) => void;
};

const weekdays = ["일", "월", "화", "수", "목", "금", "토"];

function monthString(date: Date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
  ].join("-");
}

function parseMonth(value: string) {
  const [year, month] = value.split("-").map(Number);
  return new Date(year, month - 1, 1);
}

export default function WorkoutCalendarScreen({
  selectedDate,
  onSelectDate,
}: Props) {
  const [month, setMonth] = useState(() =>
    monthString(new Date(selectedDate + "T00:00:00")),
  );
  const [days, setDays] = useState<WorkoutCalendarDay[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      setError(null);
      try {
        const result = await workoutApi.getCalendar(month);
        if (!cancelled) setDays(result);
      } catch (caught) {
        if (!cancelled) {
          setError(
            caught instanceof Error
              ? caught.message
              : "월간 운동 기록을 불러오지 못했습니다.",
          );
        }
      } finally {
        if (!cancelled) setBusy(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [month]);

  const byDate = useMemo(
    () => new Map(days.map((day) => [day.date, day])),
    [days],
  );

  const monthDate = parseMonth(month);
  const totalDays = new Date(
    monthDate.getFullYear(),
    monthDate.getMonth() + 1,
    0,
  ).getDate();
  const leadingBlanks = monthDate.getDay();
  const cells = [
    ...Array.from({ length: leadingBlanks }, () => null),
    ...Array.from({ length: totalDays }, (_, index) => index + 1),
  ];

  function moveMonth(offset: number) {
    const next = new Date(
      monthDate.getFullYear(),
      monthDate.getMonth() + offset,
      1,
    );
    setMonth(monthString(next));
  }

  function selectDay(day: number) {
    const date = new Date(
      monthDate.getFullYear(),
      monthDate.getMonth(),
      day,
    );
    onSelectDate(localDateString(date));
  }

  return (
    <main className="mx-auto min-h-screen w-full max-w-3xl px-4 py-6 sm:px-6">
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-zinc-950">
          Workout Calendar
        </h1>
        <p className="mt-2 text-sm leading-6 text-zinc-500">
          월 단위로 운동한 날짜와 수행 종목을 확인하고 날짜를 선택해 상세 기록으로 이동하세요.
        </p>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="rounded-3xl border border-zinc-200 bg-white p-4 shadow-sm sm:p-5">
        <div className="flex items-center justify-between">
          <button
            type="button"
            disabled={busy}
            onClick={() => moveMonth(-1)}
            className="rounded-xl border border-zinc-200 px-3 py-2 text-sm font-semibold text-zinc-600 disabled:opacity-40"
            aria-label="이전 달"
          >
            ←
          </button>
          <div className="text-center">
            <h2 className="text-lg font-semibold">
              {monthDate.getFullYear()}년 {monthDate.getMonth() + 1}월
            </h2>
            <button
              type="button"
              onClick={() => {
                const today = todayString();
                setMonth(today.slice(0, 7));
              }}
              className="mt-1 text-xs font-medium text-zinc-400 hover:text-zinc-900"
            >
              이번 달
            </button>
          </div>
          <button
            type="button"
            disabled={busy || month >= todayString().slice(0, 7)}
            onClick={() => moveMonth(1)}
            className="rounded-xl border border-zinc-200 px-3 py-2 text-sm font-semibold text-zinc-600 disabled:opacity-40"
            aria-label="다음 달"
          >
            →
          </button>
        </div>

        <div className="mt-5 grid grid-cols-7 gap-1 sm:gap-2">
          {weekdays.map((weekday) => (
            <div
              key={weekday}
              className="py-2 text-center text-[11px] font-semibold text-zinc-400"
            >
              {weekday}
            </div>
          ))}

          {cells.map((day, index) => {
            if (day === null) {
              return <div key={"blank-" + index} className="min-h-24" />;
            }

            const date = localDateString(
              new Date(
                monthDate.getFullYear(),
                monthDate.getMonth(),
                day,
              ),
            );
            const summary = byDate.get(date);
            const selected = selectedDate === date;
            const today = todayString() === date;
            const future = date > todayString();

            return (
              <button
                key={date}
                type="button"
                disabled={future}
                onClick={() => selectDay(day)}
                className={
                  "min-h-24 rounded-xl border p-1.5 text-left align-top transition sm:min-h-28 sm:p-2 " +
                  (selected
                    ? "border-zinc-950 bg-zinc-950 text-white"
                    : future
                      ? "border-zinc-100 bg-zinc-50 text-zinc-300 opacity-50"
                      : summary
                        ? "border-zinc-300 bg-zinc-50 hover:border-zinc-500"
                        : "border-zinc-100 bg-white hover:bg-zinc-50")
                }
              >
                <div className="flex items-start justify-between gap-1">
                  <span
                    className={
                      "text-xs font-semibold " +
                      (selected
                        ? "text-white"
                        : today
                          ? "rounded-full bg-zinc-900 px-1.5 py-0.5 text-white"
                          : "text-zinc-600")
                    }
                  >
                    {day}
                  </span>
                  {summary && (
                    <span
                      className={
                        "text-[9px] font-medium " +
                        (selected ? "text-zinc-300" : "text-zinc-400")
                      }
                    >
                      {summary.completedCount}/{summary.workoutCount}
                    </span>
                  )}
                </div>

                {summary && (
                  <div className="mt-2 space-y-1">
                    {summary.exerciseNames.slice(0, 2).map((name) => (
                      <p
                        key={name}
                        className={
                          "truncate text-[10px] font-medium leading-4 " +
                          (selected ? "text-zinc-200" : "text-zinc-600")
                        }
                      >
                        {name}
                      </p>
                    ))}
                    {summary.exerciseNames.length > 2 && (
                      <p
                        className={
                          "text-[9px] " +
                          (selected ? "text-zinc-400" : "text-zinc-400")
                        }
                      >
                        +{summary.exerciseNames.length - 2}개
                      </p>
                    )}
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </section>

      <section className="mt-4 rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-xs text-zinc-500">
        <p>
          날짜 우측 숫자는 완료 운동 수 / 전체 운동 수입니다. 날짜를 누르면 해당 날짜의 Workout 화면으로 이동합니다.
        </p>
      </section>
    </main>
  );
}
