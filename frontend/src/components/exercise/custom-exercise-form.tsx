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
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="mt-4 border-t border-zinc-100 pt-4"
    >
      <p className="text-xs font-medium text-zinc-500">
        내 운동 종목 추가
      </p>

      <div className="mt-2 flex flex-wrap gap-2">
        {exerciseCategories.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setCategory(item.value)}
            className={
              "rounded-full px-3 py-1.5 text-xs font-medium " +
              (category === item.value
                ? "bg-zinc-900 text-white"
                : "bg-zinc-100 text-zinc-500")
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
          className="min-w-0 flex-1 rounded-xl bg-zinc-100 px-3 py-3 text-sm outline-none focus:ring-2 focus:ring-zinc-300"
        />
        <button
          type="submit"
          disabled={busy || !name.trim()}
          className="rounded-xl border border-zinc-200 px-4 py-3 text-sm font-semibold disabled:opacity-40"
        >
          등록
        </button>
      </div>
    </form>
  );
}
