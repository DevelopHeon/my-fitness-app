"use client";

import { FormEvent, useMemo, useState } from "react";
import { sanitizeText } from "@/lib/input-utils";
import {
  categoryLabel,
  Exercise,
  ExerciseCategory,
  exerciseCategories,
} from "@/lib/workout-api";

type Props = {
  busy?: boolean;
  exercises: Exercise[];
  onCreate: (
    name: string,
    category: ExerciseCategory,
  ) => Promise<boolean>;
  onUpdate: (
    exercise: Exercise,
    name: string,
    category: ExerciseCategory,
  ) => Promise<boolean>;
  onDelete: (exercise: Exercise) => Promise<boolean>;
};

export default function CustomExerciseForm({
  busy = false,
  exercises,
  onCreate,
  onUpdate,
  onDelete,
}: Props) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [category, setCategory] =
    useState<ExerciseCategory>("CHEST");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingName, setEditingName] = useState("");
  const [editingCategory, setEditingCategory] =
    useState<ExerciseCategory>("CHEST");

  const customExercises = useMemo(
    () =>
      exercises
        .filter(
          (exercise) =>
            exercise.type === "CUSTOM" &&
            exercise.category === category,
        )
        .sort((left, right) =>
          left.name.localeCompare(right.name, "ko"),
        ),
    [category, exercises],
  );

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;

    const created = await onCreate(trimmed, category);
    if (created) {
      setName("");
    }
  }

  function beginEdit(exercise: Exercise) {
    setEditingId(exercise.id);
    setEditingName(exercise.name);
    setEditingCategory(exercise.category);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditingName("");
    setEditingCategory("CHEST");
  }

  async function saveEdit(exercise: Exercise) {
    const trimmed = editingName.trim();
    if (!trimmed) return;

    const updated = await onUpdate(
      exercise,
      trimmed,
      editingCategory,
    );
    if (updated) {
      setCategory(editingCategory);
      cancelEdit();
    }
  }

  async function removeExercise(exercise: Exercise) {
    const confirmed = window.confirm(
      `"${exercise.name}" 종목을 삭제할까요?\n\n새 운동 선택 목록에서는 제거되지만, 기존 운동 기록과 저장된 루틴의 종목 정보는 유지됩니다.`,
    );
    if (!confirmed) return;

    const deleted = await onDelete(exercise);
    if (deleted && editingId === exercise.id) {
      cancelEdit();
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
          내 운동 종목 관리
        </span>
        <span className="text-xs font-medium text-zinc-400">
          {open ? "접기 ↑" : "열기 ↓"}
        </span>
      </button>

      {open && (
        <div className="mt-2 space-y-3 rounded-2xl bg-zinc-50 p-3">
          <div>
            <p className="mb-2 text-xs font-semibold text-zinc-600">
              카테고리
            </p>
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-6">
              {exerciseCategories.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  disabled={busy}
                  onClick={() => {
                    setCategory(item.value);
                    cancelEdit();
                  }}
                  className={
                    "rounded-xl px-3 py-2 text-xs font-semibold transition " +
                    (category === item.value
                      ? "bg-zinc-900 text-white"
                      : "border border-zinc-200 bg-white text-zinc-500")
                  }
                >
                  {item.label}
                </button>
              ))}
            </div>
            <p className="mt-2 text-[11px] leading-5 text-zinc-400">
              선택한 카테고리로 새 종목이 등록되고, 같은 카테고리의 내 운동만 표시됩니다.
            </p>
          </div>

          <div className="border-t border-zinc-200 pt-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold text-zinc-600">
                {categoryLabel(category)} · 등록한 종목
              </p>
              <span className="text-[11px] text-zinc-400">
                {customExercises.length}개
              </span>
            </div>

            {customExercises.length === 0 ? (
              <p className="mt-2 rounded-xl border border-dashed border-zinc-200 bg-white px-3 py-4 text-center text-xs text-zinc-400">
                {categoryLabel(category)} 카테고리에 등록한 운동이 없습니다.
              </p>
            ) : (
              <div className="mt-2 max-h-72 space-y-2 overflow-y-auto overscroll-contain pr-1">
                {customExercises.map((exercise) => {
                  const editing = editingId === exercise.id;

                  return (
                    <div
                      key={exercise.id}
                      className="rounded-xl border border-zinc-200 bg-white p-3"
                    >
                      {editing ? (
                        <div className="space-y-3">
                          <div className="flex flex-wrap gap-1.5">
                            {exerciseCategories.map((item) => (
                              <button
                                key={item.value}
                                type="button"
                                disabled={busy}
                                onClick={() =>
                                  setEditingCategory(item.value)
                                }
                                className={
                                  "rounded-full px-2.5 py-1 text-[11px] font-medium " +
                                  (editingCategory === item.value
                                    ? "bg-zinc-900 text-white"
                                    : "bg-zinc-100 text-zinc-500")
                                }
                              >
                                {item.label}
                              </button>
                            ))}
                          </div>
                          <input
                            type="text"
                            value={editingName}
                            disabled={busy}
                            onChange={(event) =>
                              setEditingName(
                                sanitizeText(event.target.value),
                              )
                            }
                            maxLength={100}
                            className="w-full rounded-xl border border-zinc-200 px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-zinc-300 disabled:opacity-50"
                          />
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              disabled={busy}
                              onClick={cancelEdit}
                              className="px-2 py-1 text-xs font-medium text-zinc-400 disabled:opacity-40"
                            >
                              취소
                            </button>
                            <button
                              type="button"
                              disabled={busy || !editingName.trim()}
                              onClick={() => {
                                void saveEdit(exercise);
                              }}
                              className="rounded-lg bg-zinc-900 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-40"
                            >
                              저장
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="flex items-center justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-zinc-800">
                              {exercise.name}
                            </p>
                            <p className="mt-0.5 text-[11px] text-zinc-400">
                              {categoryLabel(exercise.category)}
                            </p>
                          </div>
                          <div className="flex shrink-0 gap-3 text-xs font-medium">
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => beginEdit(exercise)}
                              className="text-zinc-500 hover:text-zinc-900 disabled:opacity-40"
                            >
                              수정
                            </button>
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => {
                                void removeExercise(exercise);
                              }}
                              className="text-red-500 hover:text-red-700 disabled:opacity-40"
                            >
                              삭제
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <form
            onSubmit={handleSubmit}
            className="border-t border-zinc-200 pt-3"
          >
            <p className="mb-2 text-xs font-semibold text-zinc-600">
              {categoryLabel(category)} 종목 추가
            </p>

            <div className="flex gap-2">
              <input
                type="text"
                inputMode="text"
                value={name}
                disabled={busy}
                onChange={(event) =>
                  setName(sanitizeText(event.target.value))
                }
                placeholder="커스텀 운동 이름"
                maxLength={100}
                className="min-w-0 flex-1 rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm outline-none focus:ring-2 focus:ring-zinc-300 disabled:opacity-50"
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
        </div>
      )}
    </div>
  );
}
