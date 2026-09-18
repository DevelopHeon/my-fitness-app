"use client";

import { useMemo, useState } from "react";
import {
  Exercise,
  ExerciseCategory,
  exerciseCategories,
  exerciseKey,
} from "@/lib/workout-api";

type Props = {
  exercises: Exercise[];
  excludedKeys?: Set<string>;
  busy?: boolean;
  onSelect: (exercise: Exercise) => void;
};

export default function ExercisePicker({
  exercises,
  excludedKeys = new Set<string>(),
  busy = false,
  onSelect,
}: Props) {
  const [category, setCategory] = useState<ExerciseCategory>("CHEST");

  const visibleExercises = useMemo(
    () =>
      exercises.filter(
        (exercise) =>
          exercise.category === category &&
          !excludedKeys.has(exerciseKey(exercise.type, exercise.id)),
      ),
    [category, excludedKeys, exercises],
  );

  return (
    <div>
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
        {exerciseCategories.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setCategory(item.value)}
            className={
              "rounded-xl px-3 py-2.5 text-sm font-semibold transition " +
              (category === item.value
                ? "bg-zinc-950 text-white"
                : "border border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50")
            }
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
        {visibleExercises.length === 0 ? (
          <div className="col-span-full rounded-xl border border-dashed border-zinc-200 px-4 py-5 text-center text-sm text-zinc-400">
            이 카테고리에 추가할 수 있는 운동이 없습니다.
          </div>
        ) : (
          visibleExercises.map((exercise) => (
            <button
              key={exerciseKey(exercise.type, exercise.id)}
              type="button"
              disabled={busy}
              onClick={() => onSelect(exercise)}
              className="flex items-center justify-between rounded-xl border border-zinc-200 bg-white px-3 py-3 text-left text-sm hover:border-zinc-400 disabled:opacity-40"
            >
              <span className="font-medium">{exercise.name}</span>
              <span className="ml-2 shrink-0 text-[11px] font-medium text-zinc-400">
                {exercise.type === "DEFAULT" ? "기본" : "내 운동"}
              </span>
            </button>
          ))
        )}
      </div>
    </div>
  );
}
