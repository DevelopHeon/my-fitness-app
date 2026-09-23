"use client";

import { useEffect, useMemo, useState } from "react";
import WorkoutCalendarScreen from "@/components/calendar/workout-calendar-screen";
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

type SetGroupDraft = {
  weightKg: string;
  reps: string;
  setCount: string;
};

type SetPlan = {
  drafts: SetGroupDraft[];
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

function emptySetGroup(): SetGroupDraft {
  return {
    weightKg: "",
    reps: "",
    setCount: "",
  };
}

function createPlan(): SetPlan {
  return {
    drafts: [emptySetGroup()],
  };
}

function totalPlannedSets(plan: SetPlan) {
  return plan.drafts.reduce(
    (total, draft) => total + (Number(draft.setCount) || 0),
    0,
  );
}

function workoutDraftStorageKey(workoutId: number) {
  return "my-fitness:workout-draft:" + workoutId;
}

function hasDraftInput(plan: SetPlan) {
  return plan.drafts.some(
    (draft) =>
      draft.weightKg.trim() !== "" ||
      draft.reps.trim() !== "" ||
      draft.setCount.trim() !== "",
  );
}

function restoreSetPlans(workout: Workout): Record<number, SetPlan> {
  const fallback = Object.fromEntries(
    workout.exercises.map((entry) => [entry.id, createPlan()]),
  );
  if (typeof window === "undefined") return fallback;

  try {
    const raw = window.localStorage.getItem(
      workoutDraftStorageKey(workout.id),
    );
    if (!raw) return fallback;

    const parsed = JSON.parse(raw) as {
      setPlans?: Record<string, SetPlan>;
    };
    if (!parsed.setPlans) return fallback;

    return Object.fromEntries(
      workout.exercises.map((entry) => {
        const stored = parsed.setPlans?.[String(entry.id)];
        const drafts = stored?.drafts
          ?.filter((draft) => draft && typeof draft === "object")
          .map((draft) => ({
            weightKg:
              typeof draft.weightKg === "string"
                ? draft.weightKg
                : "",
            reps:
              typeof draft.reps === "string" ? draft.reps : "",
            setCount:
              typeof draft.setCount === "string"
                ? draft.setCount
                : "",
          }));

        return [
          entry.id,
          drafts && drafts.length > 0
            ? { drafts }
            : createPlan(),
        ];
      }),
    );
  } catch {
    return fallback;
  }
}

function persistSetPlans(
  workoutId: number,
  setPlans: Record<number, SetPlan>,
) {
  if (typeof window === "undefined") return;

  window.localStorage.setItem(
    workoutDraftStorageKey(workoutId),
    JSON.stringify({
      setPlans,
      savedAt: new Date().toISOString(),
    }),
  );
}

function clearStoredWorkoutDraft(workoutId: number) {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(
    workoutDraftStorageKey(workoutId),
  );
}

export default function WorkoutScreen({
  selectedDate,
  onSelectedDateChange,
  initialWorkout = null,
  initialPreviousRecords = EMPTY_PREVIOUS_RECORDS,
}: Props) {
  const [workoutView, setWorkoutView] =
    useState<"daily" | "calendar">("daily");
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
  const [expandedExerciseIds, setExpandedExerciseIds] =
    useState<Set<number>>(new Set());
  const [expandedWorkoutIds, setExpandedWorkoutIds] =
    useState<Set<number>>(new Set());
  const [draftReadyWorkoutId, setDraftReadyWorkoutId] =
    useState<number | null>(null);
  const [draftSavedMessage, setDraftSavedMessage] =
    useState<string | null>(null);
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
    if (
      !activeWorkout ||
      draftReadyWorkoutId !== activeWorkout.id
    ) {
      return;
    }

    persistSetPlans(activeWorkout.id, setPlans);
  }, [activeWorkout, draftReadyWorkoutId, setPlans]);

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
          setExpandedExerciseIds(new Set());
          setDraftReadyWorkoutId(null);
          setDraftSavedMessage(null);
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
        setSetPlans(restoreSetPlans(inProgress));
        setExpandedExerciseIds(
          new Set(
            inProgress.exercises
              .slice(0, 1)
              .map((entry) => entry.id),
          ),
        );
        setDraftReadyWorkoutId(inProgress.id);
        setDraftSavedMessage(null);
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

  function clearRecordingInputs() {
    setPreviousRecords({});
    setSetPlans({});
    setEditingSet(null);
    setExpandedExerciseIds(new Set());
    setDraftReadyWorkoutId(null);
    setDraftSavedMessage(null);
    setError(null);
  }

  function handleTemporarySave() {
    if (!activeWorkout) return;

    persistSetPlans(activeWorkout.id, setPlans);
    setDraftSavedMessage(
      "임시 저장됨 · " +
        new Date().toLocaleTimeString("ko-KR", {
          hour: "2-digit",
          minute: "2-digit",
        }),
    );
  }

  function toggleExercise(workoutExerciseId: number) {
    setExpandedExerciseIds((current) => {
      const next = new Set(current);
      if (next.has(workoutExerciseId)) {
        next.delete(workoutExerciseId);
      } else {
        next.add(workoutExerciseId);
      }
      return next;
    });
  }

  async function openWorkoutForEditing(
    workout: Workout,
    reopenCompleted: boolean,
  ) {
    const prepared = await run(async () => {
      const editableWorkout = reopenCompleted
        ? await workoutApi.reopenWorkout(workout.id)
        : workout;
      const previousEntries = await Promise.all(
        editableWorkout.exercises.map(async (entry) => {
          const previous = await workoutApi
            .getPreviousRecord(
              entry.exerciseType,
              entry.exerciseId,
            )
            .catch(() => null);
          return [
            exerciseKey(
              entry.exerciseType,
              entry.exerciseId,
            ),
            previous,
          ] as const;
        }),
      );

      return {
        workout: editableWorkout,
        previousMap: Object.fromEntries(previousEntries),
      };
    });
    if (!prepared) return;

    if (reopenCompleted) {
      clearStoredWorkoutDraft(prepared.workout.id);
    }
    const plans = reopenCompleted
      ? Object.fromEntries(
          prepared.workout.exercises.map((entry) => [
            entry.id,
            createPlan(),
          ]),
        )
      : restoreSetPlans(prepared.workout);

    replaceWorkout(prepared.workout);
    setActiveWorkout(prepared.workout);
    setPreviousRecords(prepared.previousMap);
    setSetPlans(plans);
    setExpandedExerciseIds(
      new Set(
        prepared.workout.exercises
          .slice(0, 1)
          .map((entry) => entry.id),
      ),
    );
    setDraftReadyWorkoutId(prepared.workout.id);
    setDraftSavedMessage(null);
    setEditingSet(null);
    setWorkoutView("daily");
  }

  function handleBackFromRecording() {
    if (activeWorkout) {
      persistSetPlans(activeWorkout.id, setPlans);
    }
    setActiveWorkout(null);
    clearRecordingInputs();
    setWorkoutView("daily");
  }

  async function handleResumeWorkout(workout: Workout) {
    await openWorkoutForEditing(workout, false);
  }

  async function handleEditCompletedWorkout(workout: Workout) {
    await openWorkoutForEditing(workout, true);
  }

  async function handleStartWorkout() {
    const workout = await run(() =>
      workoutApi.startWorkout(selectedDate),
    );
    if (!workout) return;

    clearStoredWorkoutDraft(workout.id);
    clearRecordingInputs();
    setActiveWorkout(workout);
    setDraftReadyWorkoutId(workout.id);
    replaceWorkout(workout);
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

    clearStoredWorkoutDraft(result.workout.id);
    setActiveWorkout(result.workout);
    replaceWorkout(result.workout);
    setPreviousRecords(previousMap);
    setEditingSet(null);
    setError(null);
    setDraftSavedMessage(null);
    setSetPlans(
      Object.fromEntries(
        result.workout.exercises.map((entry) => [
          entry.id,
          createPlan(),
        ]),
      ),
    );
    setExpandedExerciseIds(
      new Set(
        result.workout.exercises
          .slice(0, 1)
          .map((entry) => entry.id),
      ),
    );
    setDraftReadyWorkoutId(result.workout.id);
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
        [entry.id]: createPlan(),
      }));
      setExpandedExerciseIds((current) => {
        const next = new Set(current);
        next.add(entry.id);
        return next;
      });
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

  async function handleUpdateCustomExercise(
    exercise: Exercise,
    name: string,
    category: ExerciseCategory,
  ) {
    const updated = await run(() =>
      workoutApi.updateExercise(exercise.id, name, category),
    );
    if (!updated) return false;

    setExercises((current) =>
      current.map((item) =>
        item.type === "CUSTOM" && item.id === updated.id
          ? updated
          : item,
      ),
    );
    return true;
  }

  async function handleDeleteCustomExercise(exercise: Exercise) {
    const deleted = await run(async () => {
      await workoutApi.deleteExercise(exercise.id);
      return true;
    });
    if (!deleted) return false;

    setExercises((current) =>
      current.filter(
        (item) =>
          !(
            item.type === "CUSTOM" &&
            item.id === exercise.id
          ),
      ),
    );
    return true;
  }

  async function handleAddExercise(exercise: Exercise) {
    await attachExercise(exercise);
  }

  function addSetGroup(workoutExerciseId: number) {
    setSetPlans((current) => ({
      ...current,
      [workoutExerciseId]: {
        drafts: [
          ...(current[workoutExerciseId]?.drafts ?? []),
          emptySetGroup(),
        ],
      },
    }));
  }

  function updateDraft(
    workoutExerciseId: number,
    draftIndex: number,
    field: keyof SetGroupDraft,
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
          drafts,
        },
      };
    });
  }

  function removePlannedSetDraft(
    workoutExerciseId: number,
    draftIndex: number,
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
          drafts:
            drafts.length > 0 ? drafts : [emptySetGroup()],
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

    const groups = plan.drafts.map((draft) => ({
      weightKg: draft.weightKg ? Number(draft.weightKg) : 0,
      reps: draft.reps ? Number(draft.reps) : 0,
      setCount: draft.setCount ? Number(draft.setCount) : 0,
    }));

    const invalid = groups.some(
      (group) =>
        !Number.isFinite(group.weightKg) ||
        group.weightKg < 0 ||
        !Number.isInteger(group.reps) ||
        group.reps <= 0 ||
        !Number.isInteger(group.setCount) ||
        group.setCount <= 0,
    );
    const totalSetCount = groups.reduce(
      (total, group) => total + group.setCount,
      0,
    );
    if (invalid || totalSetCount > 20) {
      setError(
        "중량, 횟수, 세트 수를 확인해주세요. 한 번에 최대 20세트까지 저장할 수 있습니다.",
      );
      return;
    }

    const sets = groups.flatMap((group) =>
      Array.from({ length: group.setCount }, () => ({
        weightKg: group.weightKg,
        reps: group.reps,
        durationSeconds: null,
      })),
    );

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
          drafts: [emptySetGroup()],
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
      setSetPlans((current) => ({
        ...current,
        [entry.id]: createPlan(),
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
    setExpandedExerciseIds((current) => {
      const next = new Set(current);
      next.delete(workoutExerciseId);
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

    const hasUnsavedInput = Object.values(setPlans).some(
      hasDraftInput,
    );
    if (hasUnsavedInput) {
      setError(
        "아직 저장하지 않은 세트 입력이 있습니다. 세트를 저장한 뒤 운동을 완료해주세요.",
      );
      return;
    }

    const completed = await run(() =>
      workoutApi.completeWorkout(activeWorkout.id),
    );
    if (!completed) return;

    clearStoredWorkoutDraft(completed.id);
    replaceWorkout(completed);
    setExpandedWorkoutIds((current) => {
      const next = new Set(current);
      next.add(completed.id);
      return next;
    });
    setActiveWorkout(null);
    clearRecordingInputs();
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

  const pausedWorkout = dailyWorkouts.find(
    (workout) => workout.status === "IN_PROGRESS",
  );

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
          {workoutView === "daily" && (
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
          )}
        </div>
      </header>

      <div className="mb-6 grid grid-cols-2 rounded-2xl bg-zinc-100 p-1">
        <button
          type="button"
          onClick={() => setWorkoutView("daily")}
          className={
            "rounded-xl px-4 py-2.5 text-sm font-semibold transition " +
            (workoutView === "daily"
              ? "bg-white text-zinc-950 shadow-sm"
              : "text-zinc-500")
          }
        >
          일별 조회
        </button>
        <button
          type="button"
          onClick={() => {
            if (activeWorkout) handleBackFromRecording();
            setWorkoutView("calendar");
          }}
          className={
            "rounded-xl px-4 py-2.5 text-sm font-semibold transition " +
            (workoutView === "calendar"
              ? "bg-white text-zinc-950 shadow-sm"
              : "text-zinc-500")
          }
        >
          캘린더 조회
        </button>
      </div>

      {workoutView === "calendar" ? (
        <WorkoutCalendarScreen
          selectedDate={selectedDate}
          onSelectDate={(date) => {
            setWorkoutView("daily");
            onSelectedDateChange(date);
          }}
        />
      ) : (
        <>
      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {!activeWorkout ? (
        <div className="space-y-4">
          {pausedWorkout && (
            <section className="rounded-3xl border border-emerald-200 bg-emerald-50 p-5 shadow-sm">
              <p className="text-xs font-semibold text-emerald-700">
                진행 중인 운동
              </p>
              <h2 className="mt-1 text-lg font-semibold text-zinc-950">
                작성 중인 Workout이 있습니다.
              </h2>
              <p className="mt-1 text-xs leading-5 text-zinc-500">
                이전 화면으로 나와도 기록은 유지됩니다. 이어서 작성하거나 완료해주세요.
              </p>
              <button
                type="button"
                disabled={busy}
                onClick={() => {
                  void handleResumeWorkout(pausedWorkout);
                }}
                className="mt-4 w-full rounded-xl bg-emerald-700 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
              >
                운동 기록 이어서 작성
              </button>
            </section>
          )}

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
                    disabled={
                      busy || !selectedDate || Boolean(pausedWorkout)
                    }
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
              disabled={
                busy || !selectedDate || Boolean(pausedWorkout)
              }
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
            <button
              type="button"
              onClick={handleBackFromRecording}
              className="mb-4 text-xs font-semibold text-zinc-300 hover:text-white"
            >
              ← 이전 화면
            </button>
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
              exercises={exercises}
              onCreate={handleCreateCustomExercise}
              onUpdate={handleUpdateCustomExercise}
              onDelete={handleDeleteCustomExercise}
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
              createPlan();
            const exerciseExpanded =
              expandedExerciseIds.has(entry.id);
            const pendingSetCount =
              totalPlannedSets(plan);

            return (
              <section
                id={"workout-exercise-" + entry.id}
                key={entry.id}
                className="scroll-mt-4 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
              >
                <div className="flex items-start justify-between gap-3">
                  <button
                    type="button"
                    aria-expanded={exerciseExpanded}
                    onClick={() => toggleExercise(entry.id)}
                    className="min-w-0 flex-1 text-left"
                  >
                    <p className="text-xs font-medium text-zinc-400">
                      {categoryLabel(entry.category)}
                      {entry.exerciseType === "CUSTOM"
                        ? " · 내 운동"
                        : ""}
                    </p>
                    <h2 className="mt-1 text-lg font-semibold">
                      {entry.exerciseName}
                    </h2>
                    <p className="mt-1 text-xs text-zinc-400">
                      저장 {entry.sets.length}세트
                      {pendingSetCount > 0
                        ? " · 입력 중 " + pendingSetCount + "세트"
                        : ""}
                    </p>
                  </button>
                  <div className="flex shrink-0 items-center gap-3">
                    <button
                      type="button"
                      onClick={() => toggleExercise(entry.id)}
                      className="text-xs font-semibold text-zinc-500 hover:text-zinc-900"
                    >
                      {exerciseExpanded ? "접기 ↑" : "열기 ↓"}
                    </button>
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

                {exerciseExpanded && (
                  <>
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
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-zinc-900">
                        세트 묶음 입력
                      </p>
                      <p className="mt-1 text-xs leading-5 text-zinc-400">
                        같은 중량과 횟수는 세트 수로 묶어 한 줄에 입력하세요.
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => addSetGroup(entry.id)}
                      className="shrink-0 rounded-xl border border-zinc-200 px-3 py-2 text-xs font-semibold text-zinc-600 hover:bg-zinc-50"
                    >
                      + 로우 추가
                    </button>
                  </div>

                  <div className="mt-4 space-y-2">
                    {plan.drafts.map((draft, index) => (
                      <div
                        key={index}
                        className="grid grid-cols-[1fr_1fr_72px_auto] items-end gap-2"
                      >
                        <label className="min-w-0 text-[11px] font-medium text-zinc-400">
                          무게
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
                            aria-label={(index + 1) + "번째 묶음 중량 kg"}
                            placeholder="40"
                            className="mt-1 w-full min-w-0 rounded-xl border border-zinc-200 px-3 py-2.5 text-sm outline-none focus:border-zinc-500"
                          />
                        </label>
                        <label className="min-w-0 text-[11px] font-medium text-zinc-400">
                          횟수
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
                            aria-label={(index + 1) + "번째 묶음 반복 횟수"}
                            placeholder="10"
                            className="mt-1 w-full min-w-0 rounded-xl border border-zinc-200 px-3 py-2.5 text-sm outline-none focus:border-zinc-500"
                          />
                        </label>
                        <label className="min-w-0 text-[11px] font-medium text-zinc-400">
                          세트
                          <input
                            type="text"
                            inputMode="numeric"
                            pattern="[0-9]*"
                            value={draft.setCount}
                            onChange={(event) =>
                              updateDraft(
                                entry.id,
                                index,
                                "setCount",
                                event.target.value,
                              )
                            }
                            aria-label={(index + 1) + "번째 묶음 세트 수"}
                            placeholder="3"
                            className="mt-1 w-full min-w-0 rounded-xl border border-zinc-200 px-2 py-2.5 text-sm outline-none focus:border-zinc-500"
                          />
                        </label>
                        <button
                          type="button"
                          onClick={() =>
                            removePlannedSetDraft(
                              entry.id,
                              index,
                            )
                          }
                          aria-label={(index + 1) + "번째 세트 묶음 삭제"}
                          className="mb-0.5 rounded-lg px-2 py-2.5 text-xs font-medium text-zinc-400 hover:bg-zinc-100 hover:text-zinc-900"
                        >
                          삭제
                        </button>
                      </div>
                    ))}
                  </div>

                  <div className="mt-4 rounded-xl bg-zinc-50 px-3 py-2.5 text-xs text-zinc-500">
                    입력 예정: 총 {totalPlannedSets(plan)}세트
                  </div>

                  <button
                    type="button"
                    disabled={
                      busy ||
                      totalPlannedSets(plan) === 0
                    }
                    onClick={() => {
                      void handleSavePlannedSets(entry);
                    }}
                    className="mt-3 w-full rounded-xl bg-zinc-900 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
                  >
                    총 {totalPlannedSets(plan)}세트 저장
                  </button>
                </div>
                  </>
                )}
              </section>
            );
          })}

          <section className="rounded-3xl border border-zinc-200 bg-white p-4 shadow-sm">
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                disabled={busy}
                onClick={handleTemporarySave}
                className="rounded-2xl border border-zinc-300 bg-white px-4 py-3.5 text-sm font-semibold text-zinc-700 disabled:opacity-50"
              >
                임시 저장
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={handleComplete}
                className="rounded-2xl bg-zinc-950 px-4 py-3.5 text-sm font-semibold text-white disabled:opacity-50"
              >
                운동 완료
              </button>
            </div>
            <p className="mt-2 text-center text-xs leading-5 text-zinc-400">
              작성 중 입력값은 화면을 이동해도 자동 임시 저장됩니다.
              {draftSavedMessage
                ? " " + draftSavedMessage
                : ""}
            </p>
          </section>
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
                  <div className="flex items-center gap-2 px-4 py-3">
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
                      className="min-w-0 flex-1 text-left disabled:cursor-default"
                    >
                      <p className="text-sm font-semibold">
                        Workout #{workout.id}
                      </p>
                      <p className="mt-1 truncate text-xs text-zinc-400">
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
                    </button>
                    <span
                      className={
                        "shrink-0 rounded-full px-2.5 py-1 text-xs font-medium " +
                        (completed
                          ? "bg-zinc-100 text-zinc-600"
                          : "bg-emerald-50 text-emerald-700")
                      }
                    >
                      {completed ? "완료" : "진행 중"}
                    </span>
                    {completed ? (
                      <>
                        <button
                          type="button"
                          onClick={() =>
                            toggleWorkout(workout.id)
                          }
                          className="shrink-0 text-xs font-medium text-zinc-400 hover:text-zinc-900"
                        >
                          {expanded ? "접기" : "보기"}
                        </button>
                        <button
                          type="button"
                          disabled={busy || Boolean(pausedWorkout)}
                          onClick={() => {
                            void handleEditCompletedWorkout(workout);
                          }}
                          className="shrink-0 text-xs font-semibold text-zinc-600 hover:text-zinc-950 disabled:opacity-30"
                        >
                          수정
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => {
                          void handleResumeWorkout(workout);
                        }}
                        className="shrink-0 text-xs font-semibold text-emerald-700 hover:text-emerald-900 disabled:opacity-40"
                      >
                        이어하기
                      </button>
                    )}
                  </div>

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
        </>
      )}
    </main>
  );
}
