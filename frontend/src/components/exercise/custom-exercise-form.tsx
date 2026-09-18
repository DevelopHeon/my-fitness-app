"use client";

import { FormEvent, useState } from "react";
import { sanitizeText } from "@/lib/input-utils";
import {
  ExerciseCategory,
  exerciseCategories,
} from "@/lib/workout-api";

type Props = {
  busy?: boolean;
  onCreate: (
    name: string,
    category: ExerciseCategory,
  ) => Promise<boolean>;
};

export default function CustomExerciseForm({
  busy = false,
  onCreate,
}: Props) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [category, setCategory] =
    useState<ExerciseCategory>("CHEST");

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;

    const created = await onCreate(trimmed, category);
    if (created) {
      setName("");
      setCategory("CHEST");
      setOpen(false);
    }
  }

  return (
    <div className="mt-4 border-t border-zinc-100 pt-4">
      <button
        type="button"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className="flex w-full items-center justify-between rounded-xl px-1 py-2 text-left"
      >
        <span className="text-xs font-semibold text-zinc-600">
          내 운동 종목 추가
        </span>
        <span className="text-xs font-medium text-zinc-400">
          {open ? "접기 ↑" : "열기 ↓"}
        </span>
      </button>

      {open && (
        <form
          onSubmit={handleSubmit}
          className="mt-2 rounded-2xl bg-zinc-50 p-3"
        >
          <div className="flex flex-wrap gap-2">
            {exerciseCategories.map((item) => (
              <button
                key={item.value}
                type="button"
                onClick={() => setCategory(item.value)}
                className={
                  "rounded-full px-3 py-1.5 text-xs font-medium " +
                  (category === item.value
                    ? "bg-zinc-900 text-white"
                    : "bg-white text-zinc-500")
                }
              >
                {item.label}
              </button>
            ))}
          </div>

          <div className="mt-3 flex gap-2">
            <input
              type="text"
              inputMode="text"
              value={name}
              onChange={(event) =>
                setName(sanitizeText(event.target.value))
              }
              placeholder="커스텀 운동 이름"
              maxLength={100}
              className="min-w-0 flex-1 rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm outline-none focus:ring-2 focus:ring-zinc-300"
            />
            <button
              type="submit"
              disabled={busy || !name.trim()}
              className="rounded-xl border border-zinc-200 bg-white px-4 py-3 text-sm font-semibold disabled:opacity-40"
            >
              등록
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
