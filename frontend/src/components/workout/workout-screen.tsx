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
  Routine,
  routineApi,
} from "@/lib/routine-api";
import {
  categoryLabel,
  Exercise,
  ExerciseCategory,
  exerciseKey,
  ExerciseType,
  PreviousExerciseRecord,
  Workout,
  WorkoutExercise,
  WorkoutSet,
  workoutApi,
} from "@/lib/workout-api";

const EMPTY_PREVIOUS_RECORDS: PreviousExerciseRecord[] = [];

type BatchSetDraft = {
  weightKg: string;
  reps: string;
};

type SetPlan = {
  count: string;
  drafts: BatchSetDraft[];
};

type EditingSet = {
  workoutExerciseId: number;
  setId: number;
  weightKg: string;
  reps: string;
  durationSeconds: number | null;
};

type Props = {
  selectedDate: string;
  onSelectedDateChange: (date: string) => void;
  initialWorkout?: Workout | null;
  initialPreviousRecords?: PreviousExerciseRecord[];
};

function previousKey(record: PreviousExerciseRecord) {
  return exerciseKey(record.exerciseType, record.exerciseId);
}

function createPlan(
  entry: WorkoutExercise,
  previous: PreviousExerciseRecord | null,
): SetPlan {
  const targetCount = Math.max(
    entry.sets.length,
    previous?.sets.length ?? 3,
  );
  const drafts = Array.from(
    { length: Math.max(0, targetCount - entry.sets.length) },
    (_, offset) => {
      const previousSet = previous?.sets[entry.sets.length + offset];
      return {
        weightKg: previousSet ? String(previousSet.weightKg) : "",
        reps:
          previousSet && previousSet.reps > 0
            ? String(previousSet.reps)
            : "",
      };
    },
  );

  return {
    count: String(targetCount),
    drafts,
  };
}

export default function WorkoutScreen({
  selectedDate,
  onSelectedDateChange,
  initialWorkout = null,
  initialPreviousRecords = EMPTY_PREVIOUS_RECORDS,
}: Props) {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [routines, setRoutines] = useState<Routine[]>([]);
  const [dailyWorkouts, setDailyWorkouts] = useState<Workout[]>([]);
  const [activeWorkout, setActiveWorkout] = useState<Workout | null>(
    initialWorkout?.workoutDate === selectedDate ? initialWorkout : null,
  );
  const [previousRecords, setPreviousRecords] = useState<
    Record<string, PreviousExerciseRecord | null>
  >(() =>
    Object.fromEntries(
      initialPreviousRecords.map((record) => [
        previousKey(record),
        record,
      ]),
    ),
  );
  const [setPlans, setSetPlans] = useState<Record<number, SetPlan>>({});
  const [editingSet, setEditingSet] = useState<EditingSet | null>(null);
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
        const [exerciseList, routineList, workouts] =
          await Promise.all([
            workoutApi.getExercises(),
            routineApi.getRoutines(),
            workoutApi.getWorkouts(selectedDate, selectedDate),
          ]);
        if (cancelled) return;

        const initialForDate =
          initialWorkout?.workoutDate === selectedDate
            ? initialWorkout
            : null;
        const mergedWorkouts =
          initialForDate &&
          !workouts.some((workout) => workout.id === initialForDate.id)
            ? [initialForDate, ...workouts]
            : workouts;

        setExercises(exerciseList);
        setRoutines(routineList);
        setDailyWorkouts(mergedWorkouts);

        const inProgress =
          initialForDate?.status === "IN_PROGRESS"
            ? initialForDate
            : mergedWorkouts.find(
                (workout) => workout.status === "IN_PROGRESS",
              ) ?? null;

        setActiveWorkout(inProgress);

        if (!inProgress) {
          setPreviousRecords({});
          setSetPlans({});
          return;
        }

        const previousEntries = await Promise.all(
          inProgress.exercises.map(async (entry) => {
            const key = exerciseKey(
              entry.exerciseType,
              entry.exerciseId,
            );
            const initial = initialPreviousRecords.find(
              (record) => previousKey(record) === key,
            );
            const previous =
              initial ??
              (await workoutApi
                .getPreviousRecord(
                  entry.exerciseType,
                  entry.exerciseId,
                )
                .catch(() => null));
            return [key, previous] as const;
          }),
        );
        if (cancelled) return;

        const previousMap = Object.fromEntries(previousEntries);
        setPreviousRecords(previousMap);
        setSetPlans(
          Object.fromEntries(
            inProgress.exercises.map((entry) => [
              entry.id,
              createPlan(
                entry,
                previousMap[
                  exerciseKey(
                    entry.exerciseType,
                    entry.exerciseId,
                  )
                ],
              ),
            ]),
          ),
        );
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
  }, [
    initialPreviousRecords,
    initialWorkout,
    selectedDate,
  ]);

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

  function replaceWorkout(next: Workout) {
    setDailyWorkouts((current) => {
      const exists = current.some((workout) => workout.id === next.id);
      if (!exists) return [next, ...current];
      return current.map((workout) =>
        workout.id === next.id ? next : workout,
      );
    });
  }

  async function handleStartWorkout() {
    const workout = await run(() =>
      workoutApi.startWorkout(selectedDate),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    replaceWorkout(workout);
    setPreviousRecords({});
    setSetPlans({});
  }

  async function handleStartRoutine(routine: Routine) {
    const result = await run(() =>
      routineApi.startWorkout(routine.id, selectedDate),
    );
    if (!result) return;

    const previousMap = Object.fromEntries(
      result.previousRecords.map((record) => [
        previousKey(record),
        record,
      ]),
    );

    setActiveWorkout(result.workout);
    replaceWorkout(result.workout);
    setPreviousRecords(previousMap);
    setSetPlans(
      Object.fromEntries(
        result.workout.exercises.map((entry) => [
          entry.id,
          createPlan(
            entry,
            previousMap[
              exerciseKey(
                entry.exerciseType,
                entry.exerciseId,
              )
            ] ?? null,
          ),
        ]),
      ),
    );
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
        ?.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
    });
  }

  async function attachExercise(exercise: Exercise) {
    if (!activeWorkout) return null;

    const workout = await run(() =>
      workoutApi.addExercise(
        activeWorkout.id,
        exercise.type,
        exercise.id,
      ),
    );
    if (!workout) return null;

    setActiveWorkout(workout);
    replaceWorkout(workout);

    const previous = await workoutApi
      .getPreviousRecord(exercise.type, exercise.id)
      .catch(() => null);
    const key = exerciseKey(exercise.type, exercise.id);
    setPreviousRecords((current) => ({
      ...current,
      [key]: previous,
    }));

    const entry = workout.exercises.find(
      (item) =>
        item.exerciseType === exercise.type &&
        item.exerciseId === exercise.id,
    );
    if (entry) {
      setSetPlans((current) => ({
        ...current,
        [entry.id]: createPlan(entry, previous),
      }));
    }

    scrollToExerciseCard(
      workout,
      exercise.type,
      exercise.id,
    );
    return workout;
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
    if (activeWorkout) {
      await attachExercise(exercise);
    }
    return true;
  }

  async function handleAddExercise(exercise: Exercise) {
    await attachExercise(exercise);
  }

  function updatePlanCount(
    workoutExerciseId: number,
    value: string,
  ) {
    setSetPlans((current) => ({
      ...current,
      [workoutExerciseId]: {
        ...(current[workoutExerciseId] ?? {
          count: "",
          drafts: [],
        }),
        count: sanitizeInteger(value).slice(0, 2),
      },
    }));
  }

  function applySetCount(
    entry: WorkoutExercise,
    previous: PreviousExerciseRecord | null,
  ) {
    const currentPlan =
      setPlans[entry.id] ?? createPlan(entry, previous);
    const targetCount = Number(currentPlan.count);

    if (
      !Number.isInteger(targetCount) ||
      targetCount < 1 ||
      targetCount > 20
    ) {
      setError("세트 수는 1~20 사이 숫자로 입력해주세요.");
      return;
    }

    if (targetCount < entry.sets.length) {
      setError(
        "이미 저장된 " +
          entry.sets.length +
          "세트보다 적게 설정할 수 없습니다.",
      );
      return;
    }

    const drafts = Array.from(
      { length: targetCount - entry.sets.length },
      (_, offset) => {
        const existing = currentPlan.drafts[offset];
        if (existing) return existing;

        const previousSet =
          previous?.sets[entry.sets.length + offset];
        return {
          weightKg: previousSet
            ? String(previousSet.weightKg)
            : "",
          reps:
            previousSet && previousSet.reps > 0
              ? String(previousSet.reps)
              : "",
        };
      },
    );

    setSetPlans((current) => ({
      ...current,
      [entry.id]: {
        count: String(targetCount),
        drafts,
      },
    }));
    setError(null);
  }

  function updateDraft(
    workoutExerciseId: number,
    draftIndex: number,
    field: keyof BatchSetDraft,
    value: string,
  ) {
    setSetPlans((current) => {
      const plan = current[workoutExerciseId];
      if (!plan) return current;

      const drafts = [...plan.drafts];
      drafts[draftIndex] = {
        ...drafts[draftIndex],
        [field]:
          field === "weightKg"
            ? sanitizeDecimal(value, 2)
            : sanitizeInteger(value),
      };

      return {
        ...current,
        [workoutExerciseId]: {
          ...plan,
          drafts,
        },
      };
    });
  }

  function removePlannedSetDraft(
    workoutExerciseId: number,
    draftIndex: number,
    savedSetCount: number,
  ) {
    setSetPlans((current) => {
      const plan = current[workoutExerciseId];
      if (!plan) return current;

      const drafts = plan.drafts.filter(
        (_, index) => index !== draftIndex,
      );

      return {
        ...current,
        [workoutExerciseId]: {
          count: String(savedSetCount + drafts.length),
          drafts,
        },
      };
    });
  }

  async function handleSavePlannedSets(entry: WorkoutExercise) {
    if (!activeWorkout) return;

    const plan = setPlans[entry.id];
    if (!plan || plan.drafts.length === 0) {
      setError("추가할 세트가 없습니다. 세트 수를 늘려주세요.");
      return;
    }

    const sets = plan.drafts.map((draft) => ({
      weightKg: draft.weightKg ? Number(draft.weightKg) : 0,
      reps: draft.reps ? Number(draft.reps) : 0,
      durationSeconds: null,
    }));

    const invalid = sets.some(
      (set) =>
        !Number.isFinite(set.weightKg) ||
        set.weightKg < 0 ||
        !Number.isInteger(set.reps) ||
        set.reps <= 0,
    );
    if (invalid) {
      setError("각 세트의 중량과 반복 횟수를 확인해주세요.");
      return;
    }

    const workout = await run(() =>
      workoutApi.addSets(
        activeWorkout.id,
        entry.id,
        sets,
      ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    replaceWorkout(workout);

    const updatedEntry = workout.exercises.find(
      (item) => item.id === entry.id,
    );
    if (updatedEntry) {
      setSetPlans((current) => ({
        ...current,
        [entry.id]: {
          count: String(updatedEntry.sets.length),
          drafts: [],
        },
      }));
    }
  }

  function handleEditSet(
    workoutExerciseId: number,
    set: WorkoutSet,
  ) {
    setEditingSet({
      workoutExerciseId,
      setId: set.id,
      weightKg: String(set.weightKg),
      reps: set.reps > 0 ? String(set.reps) : "",
      durationSeconds: set.durationSeconds,
    });
  }

  async function handleSaveEditedSet() {
    if (!activeWorkout || !editingSet) return;

    const weightKg = editingSet.weightKg
      ? Number(editingSet.weightKg)
      : 0;
    const reps = editingSet.reps
      ? Number(editingSet.reps)
      : 0;

    if (
      !Number.isFinite(weightKg) ||
      weightKg < 0 ||
      !Number.isInteger(reps) ||
      reps < 0 ||
      (reps === 0 && !editingSet.durationSeconds)
    ) {
      setError("수정할 세트의 중량과 반복 횟수를 확인해주세요.");
      return;
    }

    const workout = await run(() =>
      workoutApi.updateSet(
        activeWorkout.id,
        editingSet.workoutExerciseId,
        editingSet.setId,
        weightKg,
        reps,
        editingSet.durationSeconds,
      ),
    );
    if (!workout) return;

    setActiveWorkout(workout);
    replaceWorkout(workout);
    setEditingSet(null);
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
    replaceWorkout(workout);
    if (editingSet?.setId === setId) {
      setEditingSet(null);
    }

    const entry = workout.exercises.find(
      (item) => item.id === workoutExerciseId,
    );
    if (entry) {
      const previous =
        previousRecords[
          exerciseKey(
            entry.exerciseType,
            entry.exerciseId,
          )
        ];
      setSetPlans((current) => ({
        ...current,
        [entry.id]: createPlan(entry, previous),
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
    replaceWorkout(workout);
    setSetPlans((current) => {
      const next = { ...current };
      delete next[workoutExerciseId];
      return next;
    });
    if (
      editingSet?.workoutExerciseId === workoutExerciseId
    ) {
      setEditingSet(null);
    }
  }

  async function handleComplete() {
    if (!activeWorkout) return;

    const completed = await run(() =>
      workoutApi.completeWorkout(activeWorkout.id),
    );
    if (!completed) return;

    replaceWorkout(completed);
    setExpandedWorkoutIds((current) => {
      const next = new Set(current);
      next.add(completed.id);
      return next;
    });
    setActiveWorkout(null);
    setPreviousRecords({});
    setSetPlans({});
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
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <div className="mt-2 flex items-start justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-zinc-950">
              Workout
            </h1>
            <p className="mt-2 text-sm leading-6 text-zinc-500">
              선택한 날짜의 운동만 기록하고 확인합니다.
            </p>
          </div>
          <label className="shrink-0 text-xs font-medium text-zinc-500">
            날짜
            <input
              type="date"
              max={todayString()}
              value={selectedDate}
              onChange={(event) =>
                onSelectedDateChange(event.target.value)
              }
              className="mt-1 block rounded-xl border border-zinc-200 px-2.5 py-2 text-sm outline-none focus:border-zinc-500"
            />
          </label>
        </div>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!activeWorkout ? (
        <div className="space-y-4">
          <section className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
            <div className="flex items-end justify-between gap-3">
              <div>
                <p className="text-xs font-medium text-zinc-400">
                  {selectedDate} 운동
                </p>
                <h2 className="mt-1 text-xl font-semibold">
                  루틴으로 시작
                </h2>
                <p className="mt-1 text-xs leading-5 text-zinc-400">
                  저장한 루틴을 선택하면 운동 종목과 순서를 그대로 불러옵니다.
                </p>
              </div>
              <span className="text-xs text-zinc-400">
                {routines.length}개
              </span>
            </div>

            {routines.length === 0 ? (
              <div className="mt-4 rounded-2xl border border-dashed border-zinc-200 px-4 py-5 text-center text-sm text-zinc-400">
                저장된 루틴이 없습니다. Routine 탭에서 먼저 루틴을 만들어주세요.
              </div>
            ) : (
              <div className="mt-4 space-y-2">
                {routines.map((routine) => (
                  <button
                    key={routine.id}
                    type="button"
                    disabled={busy || !selectedDate}
                    onClick={() => {
                      void handleStartRoutine(routine);
                    }}
                    className="w-full rounded-2xl border border-zinc-200 px-4 py-3.5 text-left transition hover:border-zinc-400 hover:bg-zinc-50 disabled:opacity-40"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="font-semibold text-zinc-900">
                          {routine.name}
                        </p>
                        <p className="mt-1 truncate text-xs text-zinc-400">
                          {routine.exercises
                            .map((entry) => entry.exerciseName)
                            .join(" · ")}
                        </p>
                      </div>
                      <span className="shrink-0 text-xs font-semibold text-zinc-500">
                        {routine.exercises.length}종목 →
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
            <p className="text-xs font-medium text-zinc-400">
              직접 구성
            </p>
            <h2 className="mt-1 text-lg font-semibold">
              빈 Workout으로 시작
            </h2>
            <p className="mt-1 text-xs leading-5 text-zinc-400">
              루틴 없이 카테고리에서 운동 종목을 하나씩 추가합니다.
            </p>
            <button
              type="button"
              disabled={busy || !selectedDate}
              onClick={handleStartWorkout}
              className="mt-4 w-full rounded-2xl bg-zinc-950 px-4 py-3.5 text-sm font-semibold text-white disabled:opacity-50"
            >
              직접 운동 시작
            </button>
          </section>
        </div>
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
              ] ?? null;
            const plan =
              setPlans[entry.id] ??
              createPlan(entry, previous);

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
                  </div>
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

                {previous && (
                  <div className="mt-4 rounded-xl bg-zinc-50 px-3 py-3">
                    <p className="text-xs font-medium text-zinc-400">
                      이전 기록 · {previous.workoutDate}
                    </p>
                    <p className="mt-1 text-xs font-medium text-zinc-600">
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

                <div className="mt-5 space-y-2">
                  {entry.sets.map((set) => {
                    const editing =
                      editingSet?.setId === set.id &&
                      editingSet.workoutExerciseId === entry.id;

                    return (
                      <div
                        key={set.id}
                        className="rounded-xl bg-zinc-50 px-3 py-3"
                      >
                        {editing && editingSet ? (
                          <div className="grid grid-cols-[36px_1fr_1fr_auto] items-center gap-2">
                            <span className="text-sm text-zinc-400">
                              {set.setNumber}
                            </span>
                            <input
                              type="text"
                              inputMode="decimal"
                              pattern="[0-9]*[.]?[0-9]*"
                              value={editingSet.weightKg}
                              onChange={(event) =>
                                setEditingSet({
                                  ...editingSet,
                                  weightKg: sanitizeDecimal(
                                    event.target.value,
                                    2,
                                  ),
                                })
                              }
                              aria-label="수정 중량 kg"
                              className="min-w-0 rounded-lg border border-zinc-200 bg-white px-2 py-2 text-sm"
                            />
                            <input
                              type="text"
                              inputMode="numeric"
                              pattern="[0-9]*"
                              value={editingSet.reps}
                              onChange={(event) =>
                                setEditingSet({
                                  ...editingSet,
                                  reps: sanitizeInteger(
                                    event.target.value,
                                  ),
                                })
                              }
                              aria-label="수정 반복 횟수"
                              className="min-w-0 rounded-lg border border-zinc-200 bg-white px-2 py-2 text-sm"
                            />
                            <div className="flex gap-2 text-xs">
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() => {
                                  void handleSaveEditedSet();
                                }}
                                className="font-semibold text-zinc-900"
                              >
                                저장
                              </button>
                              <button
                                type="button"
                                onClick={() => setEditingSet(null)}
                                className="text-zinc-400"
                              >
                                취소
                              </button>
                            </div>
                          </div>
                        ) : (
                          <div className="grid grid-cols-[36px_1fr_1fr_auto] items-center gap-2 text-sm">
                            <span className="text-zinc-400">
                              {set.setNumber}
                            </span>
                            <span className="font-medium">
                              {set.weightKg} kg
                            </span>
                            <span className="font-medium">
                              {set.reps > 0
                                ? set.reps + "회"
                                : set.durationSeconds
                                  ? set.durationSeconds + "초"
                                  : "-"}
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
                                onClick={() => {
                                  void handleRemoveSet(
                                    entry.id,
                                    set.id,
                                  );
                                }}
                                className="text-xs font-medium text-zinc-400 hover:text-zinc-900 disabled:opacity-40"
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

                <div className="mt-4 rounded-2xl border border-zinc-200 p-4">
                  <div className="flex items-end gap-2">
                    <label className="min-w-0 flex-1 text-xs font-medium text-zinc-500">
                      총 세트 수
                      <input
                        type="text"
                        inputMode="numeric"
                        pattern="[0-9]*"
                        value={plan.count}
                        onChange={(event) =>
                          updatePlanCount(
                            entry.id,
                            event.target.value,
                          )
                        }
                        className="mt-1.5 w-full rounded-xl border border-zinc-200 px-3 py-2.5 text-sm outline-none focus:border-zinc-500"
                      />
                    </label>
                    <button
                      type="button"
                      onClick={() =>
                        applySetCount(entry, previous)
                      }
                      className="rounded-xl border border-zinc-200 px-4 py-2.5 text-sm font-semibold"
                    >
                      적용
                    </button>
                  </div>

                  {plan.drafts.length > 0 ? (
                    <div className="mt-4 space-y-2">
                      {plan.drafts.map((draft, index) => {
                        const setNumber =
                          entry.sets.length + index + 1;
                        return (
                          <div
                            key={setNumber}
                            className="grid grid-cols-[36px_1fr_1fr_auto] items-center gap-2"
                          >
                            <span className="text-center text-sm font-medium text-zinc-400">
                              {setNumber}
                            </span>
                            <input
                              type="text"
                              inputMode="decimal"
                              pattern="[0-9]*[.]?[0-9]*"
                              value={draft.weightKg}
                              onChange={(event) =>
                                updateDraft(
                                  entry.id,
                                  index,
                                  "weightKg",
                                  event.target.value,
                                )
                              }
                              aria-label={
                                setNumber + "세트 중량 kg"
                              }
                              placeholder="중량 kg"
                              className="min-w-0 rounded-xl border border-zinc-200 px-3 py-2.5 text-sm outline-none focus:border-zinc-500"
                            />
                            <input
                              type="text"
                              inputMode="numeric"
                              pattern="[0-9]*"
                              value={draft.reps}
                              onChange={(event) =>
                                updateDraft(
                                  entry.id,
                                  index,
                                  "reps",
                                  event.target.value,
                                )
                              }
                              aria-label={
                                setNumber + "세트 반복 횟수"
                              }
                              placeholder="횟수"
                              className="min-w-0 rounded-xl border border-zinc-200 px-3 py-2.5 text-sm outline-none focus:border-zinc-500"
                            />
                            <button
                              type="button"
                              onClick={() =>
                                removePlannedSetDraft(
                                  entry.id,
                                  index,
                                  entry.sets.length,
                                )
                              }
                              aria-label={
                                setNumber + "세트 입력 행 삭제"
                              }
                              className="rounded-lg px-2 py-2 text-xs font-medium text-zinc-400 hover:bg-zinc-100 hover:text-zinc-900"
                            >
                              삭제
                            </button>
                          </div>
                        );
                      })}

                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => {
                          void handleSavePlannedSets(entry);
                        }}
                        className="mt-2 w-full rounded-xl bg-zinc-900 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
                      >
                        {plan.drafts.length}세트 한 번에 저장
                      </button>
                    </div>
                  ) : (
                    <p className="mt-3 text-xs text-zinc-400">
                      현재 {entry.sets.length}세트가 저장되어 있습니다. 더 추가하려면 총 세트 수를 늘려주세요.
                    </p>
                  )}
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
            {selectedDate} 운동 완료
          </button>
        </div>
      )}

      <section className="mt-8">
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="font-semibold">
              {selectedDate} 운동 기록
            </h2>
            <p className="mt-1 text-xs text-zinc-400">
              이 날짜에 기록한 Workout만 표시합니다.
            </p>
          </div>
          <span className="text-xs text-zinc-400">
            {dailyWorkouts.length}건
          </span>
        </div>

        <div className="space-y-2">
          {dailyWorkouts.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 px-4 py-6 text-center text-sm text-zinc-400">
              이 날짜에는 아직 운동 기록이 없습니다.
            </div>
          ) : (
            dailyWorkouts.map((workout) => {
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
                        Workout #{workout.id}
                      </p>
                      <p className="mt-1 text-xs text-zinc-400">
                        {workout.exercises
                          .map((entry) => entry.exerciseName)
                          .slice(0, 3)
                          .join(" · ") ||
                          "운동 종목 없음"}
                        {workout.exercises.length > 3
                          ? " +" +
                            (workout.exercises.length - 3)
                          : ""}
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
                        {completed ? "완료" : "진행 중"}
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
                          {workout.exercises.map((entry) => (
                            <div key={entry.id}>
                              <div className="flex items-baseline justify-between gap-3">
                                <p className="text-sm font-semibold">
                                  {entry.exerciseName}
                                </p>
                                <p className="text-xs text-zinc-400">
                                  {categoryLabel(entry.category)}
                                </p>
                              </div>
                              <div className="mt-2 flex flex-wrap gap-2">
                                {entry.sets.length === 0 ? (
                                  <span className="text-xs text-zinc-400">
                                    기록된 세트 없음
                                  </span>
                                ) : (
                                  entry.sets.map((set) => (
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
                                  ))
                                )}
                              </div>
                            </div>
                          ))}
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
