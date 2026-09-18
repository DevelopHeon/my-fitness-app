"use client";

import { useEffect, useMemo, useState } from "react";
import CustomExerciseForm from "@/components/exercise/custom-exercise-form";
import ExercisePicker from "@/components/exercise/exercise-picker";
import {
  sanitizeDecimal,
  sanitizeInteger,
  todayString,
} from "@/lib/input-utils";
import {
  categoryLabel,
  Exercise,
  ExerciseCategory,
  exerciseKey,
  ExerciseType,
  PreviousExerciseRecord,
  Workout,
  WorkoutSet,
  workoutApi,
} from "@/lib/workout-api";

type SetDraft = {
  weightKg: string;
  reps: string;
  durationSeconds: string;
};

type Props = {
  initialWorkout?: Workout | null;
  initialPreviousRecords?: PreviousExerciseRecord[];
};

export default function WorkoutScreen({
  initialWorkout = null,
  initialPreviousRecords = [],
}: Props) {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [recentWorkouts, setRecentWorkouts] = useState<Workout[]>([]);
  const [activeWorkout, setActiveWorkout] =
    useState<Workout | null>(initialWorkout);
  const [workoutDate, setWorkoutDate] = useState(todayString);
  const [previousRecords, setPreviousRecords] = useState<
    Record<string, PreviousExerciseRecord | null>
  >(() =>
    Object.fromEntries(
      initialPreviousRecords.map((record) => [
        exerciseKey(record.exerciseType, record.exerciseId),
        record,
      ]),
    ),
  );
  const [setDrafts, setSetDrafts] =
    useState<Record<number, SetDraft>>({});
  const [editingSet, setEditingSet] = useState<{
    workoutExerciseId: number;
    setId: number;
  } | null>(null);
  const [expandedWorkoutIds, setExpandedWorkoutIds] =
    useState<Set<number>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const excludedExerciseKeys = useMemo(
    () =>
      new Set(
        activeWorkout?.exercises.map((entry) =>
          exerciseKey(entry.exerciseType, entry.exerciseId),
        ) ?? [],
      ),
    [activeWorkout],
  );

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
        if (!inProgress) {
          setActiveWorkout(null);
          return;
        }

        setActiveWorkout(inProgress);
        const entries = await Promise.all(
          inProgress.exercises.map(async (entry) => {
            const record = await workoutApi
              .getPreviousRecord(entry.exerciseType, entry.exerciseId)
              .catch(() => null);
            return [
              exerciseKey(entry.exerciseType, entry.exerciseId),
              record,
            ] as const;
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
        caught instanceof Error
          ? caught.message
          : "요청 처리 중 오류가 발생했습니다.",
      );
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function handleStartWorkout() {
    const workout = await run(() =>
      workoutApi.startWorkout(workoutDate),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    setRecentWorkouts((current) => [workout, ...current]);
  }

  function scrollToExerciseCard(
    workout: Workout,
    exerciseType: ExerciseType,
    exerciseId: number,
  ) {
    const entry = workout.exercises.find(
      (workoutExercise) =>
        workoutExercise.exerciseType === exerciseType &&
        workoutExercise.exerciseId === exerciseId,
    );
    if (!entry) return;

    requestAnimationFrame(() => {
      document
        .getElementById("workout-exercise-" + entry.id)
        ?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }

  async function handleCreateCustomExercise(
    name: string,
    category: ExerciseCategory,
  ) {
    const exercise = await run(() =>
      workoutApi.createExercise(name, category),
    );
    if (!exercise) return false;

    setExercises((current) => [...current, exercise]);

    if (!activeWorkout) return true;

    const workout = await run(() =>
      workoutApi.addExercise(
        activeWorkout.id,
        exercise.type,
        exercise.id,
      ),
    );
    if (!workout) return true;

    setActiveWorkout(workout);
    setPreviousRecords((current) => ({
      ...current,
      [exerciseKey(exercise.type, exercise.id)]: null,
    }));
    scrollToExerciseCard(workout, exercise.type, exercise.id);
    return true;
  }

  async function handleAddExercise(exercise: Exercise) {
    if (!activeWorkout) return;

    const workout = await run(() =>
      workoutApi.addExercise(
        activeWorkout.id,
        exercise.type,
        exercise.id,
      ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    const previous = await workoutApi
      .getPreviousRecord(exercise.type, exercise.id)
      .catch(() => null);
    setPreviousRecords((current) => ({
      ...current,
      [exerciseKey(exercise.type, exercise.id)]: previous,
    }));
    scrollToExerciseCard(workout, exercise.type, exercise.id);
  }

  async function handleSaveSet(workoutExerciseId: number) {
    if (!activeWorkout) return;

    const draft = setDrafts[workoutExerciseId] ?? {
      weightKg: "",
      reps: "",
      durationSeconds: "",
    };
    const weightKg = draft.weightKg ? Number(draft.weightKg) : 0;
    const reps = draft.reps ? Number(draft.reps) : 0;
    const durationSeconds = draft.durationSeconds
      ? Number(draft.durationSeconds)
      : null;

    if (
      !Number.isFinite(weightKg) ||
      weightKg < 0 ||
      !Number.isInteger(reps) ||
      reps < 0 ||
      (durationSeconds !== null &&
        (!Number.isInteger(durationSeconds) ||
          durationSeconds < 0)) ||
      (reps === 0 && (!durationSeconds || durationSeconds === 0))
    ) {
      setError("중량, 반복 횟수 또는 운동 시간을 확인해주세요.");
      return;
    }

    const target =
      editingSet?.workoutExerciseId === workoutExerciseId
        ? editingSet
        : null;
    const workout = await run(() =>
      target
        ? workoutApi.updateSet(
            activeWorkout.id,
            workoutExerciseId,
            target.setId,
            weightKg,
            reps,
            durationSeconds,
          )
        : workoutApi.addSet(
            activeWorkout.id,
            workoutExerciseId,
            weightKg,
            reps,
            durationSeconds,
          ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    setEditingSet(null);
    setSetDrafts((current) => ({
      ...current,
      [workoutExerciseId]: {
        weightKg: draft.weightKg,
        reps: "",
        durationSeconds: "",
      },
    }));
  }

  function handleEditSet(
    workoutExerciseId: number,
    set: WorkoutSet,
  ) {
    setEditingSet({ workoutExerciseId, setId: set.id });
    setSetDrafts((current) => ({
      ...current,
      [workoutExerciseId]: {
        weightKg: String(set.weightKg),
        reps: set.reps > 0 ? String(set.reps) : "",
        durationSeconds:
          set.durationSeconds && set.durationSeconds > 0
            ? String(set.durationSeconds)
            : "",
      },
    }));
  }

  async function handleRemoveSet(
    workoutExerciseId: number,
    setId: number,
  ) {
    if (!activeWorkout) return;

    const workout = await run(() =>
      workoutApi.removeSet(
        activeWorkout.id,
        workoutExerciseId,
        setId,
      ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    if (editingSet?.setId === setId) {
      setEditingSet(null);
      setSetDrafts((current) => ({
        ...current,
        [workoutExerciseId]: {
          weightKg: "",
          reps: "",
          durationSeconds: "",
        },
      }));
    }
  }

  async function handleRemoveExercise(
    workoutExerciseId: number,
  ) {
    if (!activeWorkout) return;

    const workout = await run(() =>
      workoutApi.removeExercise(
        activeWorkout.id,
        workoutExerciseId,
      ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    if (editingSet?.workoutExerciseId === workoutExerciseId) {
      setEditingSet(null);
    }
    setSetDrafts((current) => {
      const next = { ...current };
      delete next[workoutExerciseId];
      return next;
    });
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
    setExpandedWorkoutIds((current) => {
      const next = new Set(current);
      next.add(completed.id);
      return next;
    });
    setActiveWorkout(null);
    setPreviousRecords({});
    setSetDrafts({});
    setEditingSet(null);
  }

  function toggleWorkout(workoutId: number) {
    setExpandedWorkoutIds((current) => {
      const next = new Set(current);
      if (next.has(workoutId)) {
        next.delete(workoutId);
      } else {
        next.add(workoutId);
      }
      return next;
    });
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
            카테고리에서 운동을 고르고 이전 기록을 보며 세트를 입력하세요.
          </p>
        </div>
        <span className="rounded-full bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white">
          Phase 2
        </span>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!activeWorkout ? (
        <section className="rounded-3xl border border-zinc-200 bg-white p-6 shadow-sm">
          <p className="text-sm font-medium text-zinc-500">
            오늘 운동
          </p>
          <h2 className="mt-2 text-xl font-semibold">
            새 운동을 시작할까요?
          </h2>
          <label className="mt-5 block text-xs font-medium text-zinc-500">
            운동 날짜
            <input
              type="date"
              value={workoutDate}
              onChange={(event) =>
                setWorkoutDate(event.target.value)
              }
              className="mt-2 w-full rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
            />
          </label>
          <button
            type="button"
            disabled={busy || !workoutDate}
            onClick={handleStartWorkout}
            className="mt-4 w-full rounded-2xl bg-zinc-950 px-4 py-3.5 text-sm font-semibold text-white disabled:opacity-50"
          >
            운동 시작
          </button>
        </section>
      ) : (
        <div className="space-y-5">
          <section className="rounded-3xl bg-zinc-950 p-5 text-white shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-zinc-400">
                  진행 중
                </p>
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
            <p className="mt-1 text-xs text-zinc-400">
              카테고리를 선택한 뒤 수행할 운동을 누르세요.
            </p>
            <div className="mt-4">
              <ExercisePicker
                exercises={exercises}
                excludedKeys={excludedExerciseKeys}
                busy={busy}
                onSelect={(exercise) => {
                  void handleAddExercise(exercise);
                }}
              />
            </div>
            <CustomExerciseForm
              busy={busy}
              onCreate={handleCreateCustomExercise}
            />
          </section>

          {activeWorkout.exercises.length === 0 && (
            <div className="rounded-2xl border border-dashed border-zinc-200 bg-white px-4 py-6 text-center text-sm text-zinc-400">
              운동 종목을 추가하면 세트 기록 입력란이 여기에 표시됩니다.
            </div>
          )}

          {activeWorkout.exercises.map((entry) => {
            const previous =
              previousRecords[
                exerciseKey(
                  entry.exerciseType,
                  entry.exerciseId,
                )
              ];
            const draft = setDrafts[entry.id] ?? {
              weightKg: "",
              reps: "",
              durationSeconds: "",
            };
            const isEditingSet =
              editingSet?.workoutExerciseId === entry.id;

            return (
              <section
                id={"workout-exercise-" + entry.id}
                key={entry.id}
                className="scroll-mt-4 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-medium text-zinc-400">
                      {categoryLabel(entry.category)}
                      {entry.exerciseType === "CUSTOM"
                        ? " · 내 운동"
                        : ""}
                    </p>
                    <h2 className="mt-1 text-lg font-semibold">
                      {entry.exerciseName}
                    </h2>
                    <p className="mt-1 text-xs font-medium text-zinc-400">
                      세트 기록
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    {previous && (
                      <div className="text-right text-xs text-zinc-400">
                        <p>이전 {previous.workoutDate}</p>
                        <p className="mt-1 font-medium text-zinc-600">
                          {previous.sets
                            .map((set) =>
                              [
                                set.weightKg > 0
                                  ? set.weightKg + "kg"
                                  : null,
                                set.reps > 0
                                  ? set.reps + "회"
                                  : null,
                                set.durationSeconds
                                  ? set.durationSeconds + "초"
                                  : null,
                              ]
                                .filter(Boolean)
                                .join(" × "),
                            )
                            .join(" · ")}
                        </p>
                      </div>
                    )}
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() =>
                        handleRemoveExercise(entry.id)
                      }
                      className="text-xs font-medium text-zinc-400 hover:text-zinc-900 disabled:opacity-40"
                    >
                      종목 삭제
                    </button>
                  </div>
                </div>

                <div className="mt-5 space-y-2">
                  {entry.sets.map((set) => (
                    <div
                      key={set.id}
                      className="grid grid-cols-[36px_1fr_1fr_auto] items-center gap-2 rounded-xl bg-zinc-50 px-3 py-2.5 text-sm"
                    >
                      <span className="text-zinc-400">
                        {set.setNumber}
                      </span>
                      <span className="font-medium">
                        {set.weightKg} kg
                      </span>
                      <span className="font-medium">
                        {set.reps > 0
                          ? set.reps + " reps"
                          : ""}
                        {set.durationSeconds
                          ? (set.reps > 0 ? " · " : "") +
                            set.durationSeconds +
                            "s"
                          : ""}
                      </span>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() =>
                            handleEditSet(entry.id, set)
                          }
                          className="text-xs font-medium text-zinc-500 hover:text-zinc-900 disabled:opacity-40"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          disabled={busy}
                          onClick={() =>
                            handleRemoveSet(
                              entry.id,
                              set.id,
                            )
                          }
                          className="text-xs font-medium text-zinc-400 hover:text-zinc-900 disabled:opacity-40"
                        >
                          삭제
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-[1fr_1fr_1fr_auto]">
                  <input
                    type="text"
                    inputMode="decimal"
                    pattern="[0-9]*[.]?[0-9]*"
                    value={draft.weightKg}
                    onChange={(event) =>
                      setSetDrafts((current) => ({
                        ...current,
                        [entry.id]: {
                          ...draft,
                          weightKg: sanitizeDecimal(
                            event.target.value,
                          ),
                        },
                      }))
                    }
                    aria-label="중량 kg"
                    placeholder="중량 kg"
                    className="min-w-0 rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
                  />
                  <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={draft.reps}
                    onChange={(event) =>
                      setSetDrafts((current) => ({
                        ...current,
                        [entry.id]: {
                          ...draft,
                          reps: sanitizeInteger(
                            event.target.value,
                          ),
                        },
                      }))
                    }
                    aria-label="반복 횟수"
                    placeholder="횟수"
                    className="min-w-0 rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
                  />
                  <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={draft.durationSeconds}
                    onChange={(event) =>
                      setSetDrafts((current) => ({
                        ...current,
                        [entry.id]: {
                          ...draft,
                          durationSeconds:
                            sanitizeInteger(
                              event.target.value,
                            ),
                        },
                      }))
                    }
                    aria-label="운동 시간 초"
                    placeholder="시간(초)"
                    className="min-w-0 rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
                  />
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() =>
                      handleSaveSet(entry.id)
                    }
                    className="rounded-xl bg-zinc-900 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
                  >
                    {isEditingSet
                      ? "수정 저장"
                      : "저장"}
                  </button>
                </div>

                {isEditingSet && (
                  <button
                    type="button"
                    onClick={() => {
                      setEditingSet(null);
                      setSetDrafts((current) => ({
                        ...current,
                        [entry.id]: {
                          weightKg: "",
                          reps: "",
                          durationSeconds: "",
                        },
                      }));
                    }}
                    className="mt-2 text-xs font-medium text-zinc-400 hover:text-zinc-900"
                  >
                    수정 취소
                  </button>
                )}
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
          <span className="text-xs text-zinc-400">
            최근 30일
          </span>
        </div>

        <div className="space-y-2">
          {recentWorkouts.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 px-4 py-6 text-center text-sm text-zinc-400">
              아직 기록된 운동이 없습니다.
            </div>
          ) : (
            recentWorkouts.slice(0, 5).map((workout) => {
              const expanded =
                expandedWorkoutIds.has(workout.id);
              const completed =
                workout.status === "COMPLETED";

              return (
                <article
                  key={workout.id}
                  className="overflow-hidden rounded-2xl border border-zinc-200 bg-white"
                >
                  <button
                    type="button"
                    disabled={!completed}
                    aria-expanded={
                      completed ? expanded : undefined
                    }
                    onClick={() =>
                      completed &&
                      toggleWorkout(workout.id)
                    }
                    className="flex w-full items-center justify-between px-4 py-3 text-left disabled:cursor-default"
                  >
                    <div>
                      <p className="text-sm font-semibold">
                        {workout.workoutDate}
                      </p>
                      <p className="mt-1 text-xs text-zinc-400">
                        {workout.exercises.length}개 종목
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span
                        className={
                          "rounded-full px-2.5 py-1 text-xs font-medium " +
                          (completed
                            ? "bg-zinc-100 text-zinc-600"
                            : "bg-emerald-50 text-emerald-700")
                        }
                      >
                        {completed
                          ? "완료"
                          : "진행 중"}
                      </span>
                      {completed && (
                        <span className="text-xs text-zinc-400">
                          {expanded ? "접기" : "보기"}
                        </span>
                      )}
                    </div>
                  </button>

                  {completed && expanded && (
                    <div className="border-t border-zinc-100 px-4 py-4">
                      {workout.exercises.length === 0 ? (
                        <p className="text-sm text-zinc-400">
                          수행한 운동 종목이 없습니다.
                        </p>
                      ) : (
                        <div className="space-y-4">
                          {workout.exercises.map(
                            (entry) => (
                              <div key={entry.id}>
                                <div className="flex items-baseline justify-between gap-3">
                                  <p className="text-sm font-semibold">
                                    {entry.exerciseName}
                                  </p>
                                  <p className="text-xs text-zinc-400">
                                    {categoryLabel(
                                      entry.category,
                                    )}
                                  </p>
                                </div>
                                <div className="mt-2 flex flex-wrap gap-2">
                                  {entry.sets.length ===
                                  0 ? (
                                    <span className="text-xs text-zinc-400">
                                      기록된 세트 없음
                                    </span>
                                  ) : (
                                    entry.sets.map(
                                      (set) => (
                                        <span
                                          key={set.id}
                                          className="rounded-lg bg-zinc-50 px-2.5 py-1.5 text-xs text-zinc-600"
                                        >
                                          {set.setNumber}세트 ·{" "}
                                          {set.weightKg}kg
                                          {set.reps > 0
                                            ? " × " +
                                              set.reps +
                                              "회"
                                            : ""}
                                          {set.durationSeconds
                                            ? " · " +
                                              set.durationSeconds +
                                              "초"
                                            : ""}
                                        </span>
                                      ),
                                    )
                                  )}
                                </div>
                              </div>
                            ),
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </article>
              );
            })
          )}
        </div>
      </section>
    </main>
  );
}
