"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import CustomExerciseForm from "@/components/exercise/custom-exercise-form";
import ExercisePicker from "@/components/exercise/exercise-picker";
import {
  sanitizeText,
  todayString,
} from "@/lib/input-utils";
import {
  Routine,
  RoutineExerciseRequest,
  RoutineWorkoutStart,
  routineApi,
} from "@/lib/routine-api";
import {
  categoryLabel,
  Exercise,
  ExerciseCategory,
  exerciseKey,
  workoutApi,
} from "@/lib/workout-api";

type Props = {
  onWorkoutStarted: (result: RoutineWorkoutStart) => void;
};

export default function RoutineScreen({
  onWorkoutStarted,
}: Props) {
  const [routines, setRoutines] = useState<Routine[]>([]);
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [name, setName] = useState("");
  const [selectedExercises, setSelectedExercises] =
    useState<RoutineExerciseRequest[]>([]);
  const [editingRoutineId, setEditingRoutineId] =
    useState<number | null>(null);
  const [editorVersion, setEditorVersion] = useState(0);
  const [workoutDate, setWorkoutDate] = useState(todayString);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const exerciseByKey = useMemo(
    () =>
      new Map(
        exercises.map((exercise) => [
          exerciseKey(exercise.type, exercise.id),
          exercise,
        ]),
      ),
    [exercises],
  );

  const excludedKeys = useMemo(
    () =>
      new Set(
        selectedExercises.map((exercise) =>
          exerciseKey(
            exercise.exerciseType,
            exercise.exerciseId,
          ),
        ),
      ),
    [selectedExercises],
  );

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      setError(null);
      try {
        const [routineList, exerciseList] =
          await Promise.all([
            routineApi.getRoutines(),
            workoutApi.getExercises(),
          ]);
        if (cancelled) return;
        setRoutines(routineList);
        setExercises(exerciseList);
      } catch (caught) {
        if (!cancelled) {
          setError(
            caught instanceof Error
              ? caught.message
              : "요청 처리 중 오류가 발생했습니다.",
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
  }, []);

  async function run<T>(action: () => Promise<T>) {
    setError(null);
    setBusy(true);
    try {
      return await action();
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "요청 처리 중 오류가 발생했습니다.",
      );
      return null;
    } finally {
      setBusy(false);
    }
  }

  function resetEditor() {
    setName("");
    setSelectedExercises([]);
    setEditingRoutineId(null);
    setEditorVersion((current) => current + 1);
    setError(null);
  }

  function handleSelectExercise(exercise: Exercise) {
    const key = exerciseKey(exercise.type, exercise.id);
    if (excludedKeys.has(key)) return;

    setSelectedExercises((current) => [
      ...current,
      {
        exerciseType: exercise.type,
        exerciseId: exercise.id,
      },
    ]);
  }

  async function handleCreateCustomExercise(
    exerciseName: string,
    category: ExerciseCategory,
  ) {
    const exercise = await run(() =>
      workoutApi.createExercise(exerciseName, category),
    );
    if (!exercise) return false;

    setExercises((current) => [...current, exercise]);
    setSelectedExercises((current) => [
      ...current,
      {
        exerciseType: exercise.type,
        exerciseId: exercise.id,
      },
    ]);
    return true;
  }

  function moveExercise(
    index: number,
    offset: -1 | 1,
  ) {
    const target = index + offset;
    if (
      target < 0 ||
      target >= selectedExercises.length
    ) {
      return;
    }

    setSelectedExercises((current) => {
      const next = [...current];
      [next[index], next[target]] = [
        next[target],
        next[index],
      ];
      return next;
    });
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    const trimmedName = name.trim();

    if (
      !trimmedName ||
      selectedExercises.length === 0
    ) {
      setError(
        "루틴 이름과 하나 이상의 운동 종목이 필요합니다.",
      );
      return;
    }

    const routine = await run(() =>
      editingRoutineId
        ? routineApi.updateRoutine(
            editingRoutineId,
            trimmedName,
            selectedExercises,
          )
        : routineApi.createRoutine(
            trimmedName,
            selectedExercises,
          ),
    );
    if (!routine) return;

    setRoutines((current) =>
      editingRoutineId
        ? current.map((item) =>
            item.id === routine.id ? routine : item,
          )
        : [routine, ...current],
    );
    resetEditor();
  }

  function handleEdit(routine: Routine) {
    setEditingRoutineId(routine.id);
    setEditorVersion((current) => current + 1);
    setName(routine.name);
    setSelectedExercises(
      routine.exercises.map((entry) => ({
        exerciseType: entry.exerciseType,
        exerciseId: entry.exerciseId,
      })),
    );

    requestAnimationFrame(() => {
      document
        .getElementById("routine-editor")
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
    });
  }

  async function handleDelete(routineId: number) {
    const result = await run(async () => {
      await routineApi.deleteRoutine(routineId);
      return true;
    });
    if (!result) return;

    setRoutines((current) =>
      current.filter(
        (routine) => routine.id !== routineId,
      ),
    );
    if (editingRoutineId === routineId) {
      resetEditor();
    }
  }

  async function handleStart(routineId: number) {
    if (!workoutDate) {
      setError("운동 날짜를 선택해주세요.");
      return;
    }

    const result = await run(() =>
      routineApi.startWorkout(
        routineId,
        workoutDate,
      ),
    );
    if (result) onWorkoutStarted(result);
  }

  return (
    <main className="mx-auto min-h-screen w-full max-w-2xl px-4 py-6 sm:px-6">
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <div className="mt-2 flex items-center justify-between gap-3">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-zinc-950">
              Routine
            </h1>
            <p className="mt-2 text-sm leading-6 text-zinc-500">
              카테고리별 운동을 골라 자주 쓰는 조합과 순서를 저장하세요.
            </p>
          </div>
          <span className="rounded-full bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white">
            Phase 2
          </span>
        </div>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <section
        id="routine-editor"
        className="scroll-mt-20 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
      >
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">
            {editingRoutineId
              ? "루틴 수정"
              : "새 루틴"}
          </h2>
          {editingRoutineId && (
            <button
              type="button"
              onClick={resetEditor}
              className="text-xs font-medium text-zinc-400 hover:text-zinc-900"
            >
              취소
            </button>
          )}
        </div>

        <form
          onSubmit={handleSave}
          className="mt-4 space-y-4"
        >
          <input
            type="text"
            inputMode="text"
            value={name}
            onChange={(event) =>
              setName(
                sanitizeText(
                  event.target.value,
                  100,
                ),
              )
            }
            placeholder="루틴 이름 (예: Push, Pull, Legs)"
            maxLength={100}
            className="w-full rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
          />

          <ExercisePicker
            key={"routine-picker-" + editorVersion}
            exercises={exercises}
            excludedKeys={excludedKeys}
            busy={busy}
            onSelect={handleSelectExercise}
          />

          <div className="space-y-2">
            {selectedExercises.length === 0 ? (
              <div className="rounded-xl border border-dashed border-zinc-200 px-3 py-5 text-center text-sm text-zinc-400">
                카테고리를 선택하고 운동 종목을 순서대로 추가하세요.
              </div>
            ) : (
              selectedExercises.map(
                (selected, index) => {
                  const key = exerciseKey(
                    selected.exerciseType,
                    selected.exerciseId,
                  );
                  const exercise =
                    exerciseByKey.get(key);

                  return (
                    <div
                      key={key}
                      className="flex items-center gap-2 rounded-xl bg-zinc-50 px-3 py-2.5"
                    >
                      <span className="w-6 text-xs font-semibold text-zinc-400">
                        {index + 1}
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">
                          {exercise?.name ??
                            "운동 종목"}
                        </p>
                        {exercise && (
                          <p className="mt-0.5 text-[11px] text-zinc-400">
                            {categoryLabel(
                              exercise.category,
                            )}
                            {exercise.type ===
                            "CUSTOM"
                              ? " · 내 운동"
                              : ""}
                          </p>
                        )}
                      </div>
                      <button
                        type="button"
                        disabled={index === 0}
                        onClick={() =>
                          moveExercise(index, -1)
                        }
                        className="px-1 text-sm text-zinc-400 disabled:opacity-20"
                        aria-label="위로 이동"
                      >
                        ↑
                      </button>
                      <button
                        type="button"
                        disabled={
                          index ===
                          selectedExercises.length - 1
                        }
                        onClick={() =>
                          moveExercise(index, 1)
                        }
                        className="px-1 text-sm text-zinc-400 disabled:opacity-20"
                        aria-label="아래로 이동"
                      >
                        ↓
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          setSelectedExercises(
                            (current) =>
                              current.filter(
                                (item) =>
                                  exerciseKey(
                                    item.exerciseType,
                                    item.exerciseId,
                                  ) !== key,
                              ),
                          )
                        }
                        className="ml-1 text-xs font-medium text-zinc-400 hover:text-zinc-900"
                      >
                        삭제
                      </button>
                    </div>
                  );
                },
              )
            )}
          </div>

          <button
            type="submit"
            disabled={
              busy ||
              !name.trim() ||
              selectedExercises.length === 0
            }
            className="w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
          >
            {editingRoutineId
              ? "루틴 저장"
              : "루틴 만들기"}
          </button>
        </form>

        <CustomExerciseForm
          key={"routine-custom-" + editorVersion}
          busy={busy}
          onCreate={handleCreateCustomExercise}
        />
      </section>

      <section className="mt-6">
        <div className="mb-3 flex items-end justify-between gap-4">
          <div>
            <h2 className="font-semibold">
              저장된 루틴
            </h2>
            <span className="text-xs text-zinc-400">
              {routines.length}개
            </span>
          </div>
          <label className="text-xs font-medium text-zinc-500">
            운동 날짜
            <input
              type="date"
              value={workoutDate}
              onChange={(event) =>
                setWorkoutDate(
                  event.target.value,
                )
              }
              className="mt-1 block rounded-xl border border-zinc-200 px-3 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </label>
        </div>

        <div className="space-y-3">
          {routines.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-400">
              아직 저장된 루틴이 없습니다.
            </div>
          ) : (
            routines.map((routine) => (
              <article
                key={routine.id}
                className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-semibold">
                      {routine.name}
                    </h3>
                    <p className="mt-1 text-xs text-zinc-400">
                      {routine.exercises.length}개 종목
                    </p>
                  </div>
                  <div className="flex gap-3 text-xs font-medium">
                    <button
                      type="button"
                      onClick={() =>
                        handleEdit(routine)
                      }
                      className="text-zinc-500 hover:text-zinc-900"
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() =>
                        handleDelete(routine.id)
                      }
                      className="text-zinc-400 hover:text-zinc-900 disabled:opacity-40"
                    >
                      삭제
                    </button>
                  </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-2">
                  {routine.exercises.map(
                    (entry) => (
                      <span
                        key={entry.id}
                        className="rounded-full bg-zinc-100 px-3 py-1.5 text-xs font-medium text-zinc-600"
                      >
                        {entry.orderIndex}.{" "}
                        {entry.exerciseName}
                      </span>
                    ),
                  )}
                </div>

                <button
                  type="button"
                  disabled={busy || !workoutDate}
                  onClick={() =>
                    handleStart(routine.id)
                  }
                  className="mt-5 w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
                >
                  이 루틴으로 운동 시작
                </button>
              </article>
            ))
          )}
        </div>
      </section>
    </main>
  );
}
