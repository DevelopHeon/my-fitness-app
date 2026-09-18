"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Exercise,
  PreviousExerciseRecord,
  Workout,
  WorkoutSet,
  workoutApi,
} from "@/lib/workout-api";

type SetDraft = {
  weightKg: string;
  reps: string;
};

export default function WorkoutScreen() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [recentWorkouts, setRecentWorkouts] = useState<Workout[]>([]);
  const [activeWorkout, setActiveWorkout] = useState<Workout | null>(null);
  const [previousRecords, setPreviousRecords] = useState<
    Record<number, PreviousExerciseRecord | null>
  >({});
  const [setDrafts, setSetDrafts] = useState<Record<number, SetDraft>>({});
  const [newExerciseName, setNewExerciseName] = useState("");
  const [selectedExerciseId, setSelectedExerciseId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const availableExercises = useMemo(() => {
    const selectedIds = new Set(
      activeWorkout?.exercises.map((entry) => entry.exerciseId) ?? [],
    );
    return exercises.filter((exercise) => !selectedIds.has(exercise.id));
  }, [activeWorkout, exercises]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      setError(null);

      try {
        const [exerciseList, workouts] = await Promise.all([
          workoutApi.getExercises(),
          workoutApi.getWorkouts(),
        ]);

        if (cancelled) return;

        setExercises(exerciseList);
        setRecentWorkouts(workouts);

        const inProgress = workouts.find(
          (workout) => workout.status === "IN_PROGRESS",
        );
        if (!inProgress) return;

        setActiveWorkout(inProgress);
        const entries = await Promise.all(
          inProgress.exercises.map(async (entry) => {
            const record = await workoutApi
              .getPreviousRecord(entry.exerciseId)
              .catch(() => null);
            return [entry.exerciseId, record] as const;
          }),
        );

        if (!cancelled) {
          setPreviousRecords(Object.fromEntries(entries));
        }
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
        caught instanceof Error ? caught.message : "요청 처리 중 오류가 발생했습니다.",
      );
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function handleStartWorkout() {
    const workout = await run(() => workoutApi.startWorkout());
    if (!workout) return;

    setActiveWorkout(workout);
    setRecentWorkouts((current) => [workout, ...current]);
  }

  async function handleCreateExercise(event: FormEvent) {
    event.preventDefault();
    const name = newExerciseName.trim();
    if (!name) return;

    const exercise = await run(() => workoutApi.createExercise(name));
    if (!exercise) return;

    setExercises((current) => [...current, exercise]);
    setSelectedExerciseId(exercise.id);
    setNewExerciseName("");
  }

  async function handleAddExercise() {
    if (!activeWorkout || !selectedExerciseId) return;

    const workout = await run(() =>
      workoutApi.addExercise(activeWorkout.id, selectedExerciseId),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    const previous = await workoutApi
      .getPreviousRecord(selectedExerciseId)
      .catch(() => null);
    setPreviousRecords((current) => ({
      ...current,
      [selectedExerciseId]: previous,
    }));
    setSelectedExerciseId(null);
  }

  async function handleAddSet(workoutExerciseId: number) {
    if (!activeWorkout) return;

    const draft = setDrafts[workoutExerciseId] ?? {
      weightKg: "",
      reps: "",
    };
    const weightKg = Number(draft.weightKg);
    const reps = Number(draft.reps);

    if (!Number.isFinite(weightKg) || !Number.isInteger(reps) || reps <= 0) {
      setError("중량과 반복 횟수를 확인해주세요.");
      return;
    }

    const workout = await run(() =>
      workoutApi.addSet(
        activeWorkout.id,
        workoutExerciseId,
        weightKg,
        reps,
      ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    setSetDrafts((current) => ({
      ...current,
      [workoutExerciseId]: { weightKg: draft.weightKg, reps: "" },
    }));
  }

  async function handleRemoveSet(workoutExerciseId: number, setId: number) {
    if (!activeWorkout) return;

    const workout = await run(() =>
      workoutApi.removeSet(activeWorkout.id, workoutExerciseId, setId),
    );
    if (workout) setActiveWorkout(workout);
  }

  async function handleComplete() {
    if (!activeWorkout) return;

    const completed = await run(() =>
      workoutApi.completeWorkout(activeWorkout.id),
    );
    if (!completed) return;

    setRecentWorkouts((current) =>
      current.map((workout) =>
        workout.id === completed.id ? completed : workout,
      ),
    );
    setActiveWorkout(null);
    setPreviousRecords({});
    setSetDrafts({});
  }

  return (
    <main className="mx-auto min-h-screen w-full max-w-2xl px-4 py-6 sm:px-6">
      <header className="mb-8 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
            MY FITNESS
          </p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-zinc-950">
            Workout
          </h1>
          <p className="mt-2 text-sm leading-6 text-zinc-500">
            이전 기록을 보면서 오늘의 중량과 반복 횟수를 빠르게 남겨보세요.
          </p>
        </div>
        <span className="rounded-full bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white">
          Phase 1
        </span>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!activeWorkout ? (
        <section className="rounded-3xl border border-zinc-200 bg-white p-6 shadow-sm">
          <p className="text-sm font-medium text-zinc-500">오늘 운동</p>
          <h2 className="mt-2 text-xl font-semibold">새 운동을 시작할까요?</h2>
          <p className="mt-2 text-sm leading-6 text-zinc-500">
            운동을 시작한 뒤 종목을 추가하고 세트별 기록을 입력할 수 있습니다.
          </p>
          <button
            type="button"
            disabled={busy}
            onClick={handleStartWorkout}
            className="mt-6 w-full rounded-2xl bg-zinc-950 px-4 py-3.5 text-sm font-semibold text-white disabled:opacity-50"
          >
            운동 시작
          </button>
        </section>
      ) : (
        <div className="space-y-5">
          <section className="rounded-3xl bg-zinc-950 p-5 text-white shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-zinc-400">진행 중</p>
                <h2 className="mt-1 text-xl font-semibold">
                  {activeWorkout.workoutDate}
                </h2>
              </div>
              <p className="text-sm text-zinc-400">
                {activeWorkout.exercises.length}개 종목
              </p>
            </div>
          </section>

          <section className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
            <h2 className="font-semibold">운동 종목 추가</h2>
            <div className="mt-4 flex gap-2">
              <select
                value={selectedExerciseId ?? ""}
                onChange={(event) =>
                  setSelectedExerciseId(
                    event.target.value ? Number(event.target.value) : null,
                  )
                }
                className="min-w-0 flex-1 rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm outline-none focus:border-zinc-500"
              >
                <option value="">종목 선택</option>
                {availableExercises.map((exercise) => (
                  <option key={exercise.id} value={exercise.id}>
                    {exercise.name}
                  </option>
                ))}
              </select>
              <button
                type="button"
                disabled={!selectedExerciseId || busy}
                onClick={handleAddExercise}
                className="rounded-xl bg-zinc-900 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
              >
                추가
              </button>
            </div>

            <form
              onSubmit={handleCreateExercise}
              className="mt-3 flex gap-2 border-t border-zinc-100 pt-3"
            >
              <input
                value={newExerciseName}
                onChange={(event) => setNewExerciseName(event.target.value)}
                placeholder="새 운동 종목"
                className="min-w-0 flex-1 rounded-xl bg-zinc-100 px-3 py-3 text-sm outline-none focus:ring-2 focus:ring-zinc-300"
              />
              <button
                type="submit"
                disabled={!newExerciseName.trim() || busy}
                className="rounded-xl border border-zinc-200 px-4 py-3 text-sm font-semibold disabled:opacity-40"
              >
                등록
              </button>
            </form>
          </section>

          {activeWorkout.exercises.map((entry) => {
            const previous = previousRecords[entry.exerciseId];
            const draft = setDrafts[entry.id] ?? {
              weightKg: "",
              reps: "",
            };

            return (
              <section
                key={entry.id}
                className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-medium text-zinc-400">
                      {entry.category ?? "EXERCISE"}
                    </p>
                    <h2 className="mt-1 text-lg font-semibold">
                      {entry.exerciseName}
                    </h2>
                  </div>
                  {previous && (
                    <div className="text-right text-xs text-zinc-400">
                      <p>이전 {previous.workoutDate}</p>
                      <p className="mt-1 font-medium text-zinc-600">
                        {previous.sets
                          .map((set) => `${set.weightKg}kg × ${set.reps}`)
                          .join(" · ")}
                      </p>
                    </div>
                  )}
                </div>

                <div className="mt-5 space-y-2">
                  {entry.sets.map((set) => (
                    <div
                      key={set.id}
                      className="grid grid-cols-[36px_1fr_1fr_40px] items-center gap-2 rounded-xl bg-zinc-50 px-3 py-2.5 text-sm"
                    >
                      <span className="text-zinc-400">{set.setNumber}</span>
                      <span className="font-medium">{set.weightKg} kg</span>
                      <span className="font-medium">{set.reps} reps</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveSet(entry.id, set.id)}
                        className="text-xs font-medium text-zinc-400 hover:text-zinc-900"
                      >
                        삭제
                      </button>
                    </div>
                  ))}
                </div>

                <div className="mt-4 grid grid-cols-[1fr_1fr_auto] gap-2">
                  <input
                    inputMode="decimal"
                    value={draft.weightKg}
                    onChange={(event) =>
                      setSetDrafts((current) => ({
                        ...current,
                        [entry.id]: {
                          ...draft,
                          weightKg: event.target.value,
                        },
                      }))
                    }
                    placeholder="kg"
                    className="min-w-0 rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
                  />
                  <input
                    inputMode="numeric"
                    value={draft.reps}
                    onChange={(event) =>
                      setSetDrafts((current) => ({
                        ...current,
                        [entry.id]: {
                          ...draft,
                          reps: event.target.value,
                        },
                      }))
                    }
                    placeholder="reps"
                    className="min-w-0 rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
                  />
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => handleAddSet(entry.id)}
                    className="rounded-xl bg-zinc-900 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
                  >
                    저장
                  </button>
                </div>
              </section>
            );
          })}

          <button
            type="button"
            disabled={busy}
            onClick={handleComplete}
            className="w-full rounded-2xl border border-zinc-300 bg-white px-4 py-3.5 text-sm font-semibold text-zinc-900 disabled:opacity-50"
          >
            오늘 운동 완료
          </button>
        </div>
      )}

      <section className="mt-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-semibold">최근 운동</h2>
          <span className="text-xs text-zinc-400">최근 30일</span>
        </div>
        <div className="space-y-2">
          {recentWorkouts.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 px-4 py-6 text-center text-sm text-zinc-400">
              아직 기록된 운동이 없습니다.
            </div>
          ) : (
            recentWorkouts.slice(0, 5).map((workout) => (
              <article
                key={workout.id}
                className="flex items-center justify-between rounded-2xl border border-zinc-200 bg-white px-4 py-3"
              >
                <div>
                  <p className="text-sm font-semibold">{workout.workoutDate}</p>
                  <p className="mt-1 text-xs text-zinc-400">
                    {workout.exercises.length}개 종목
                  </p>
                </div>
                <span
                  className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                    workout.status === "COMPLETED"
                      ? "bg-zinc-100 text-zinc-600"
                      : "bg-emerald-50 text-emerald-700"
                  }`}
                >
                  {workout.status === "COMPLETED" ? "완료" : "진행 중"}
                </span>
              </article>
            ))
          )}
        </div>
      </section>
    </main>
  );
}
