import { request } from "@/lib/api-client";

export type ExerciseType = "DEFAULT" | "CUSTOM";
export type ExerciseCategory =
  | "CHEST"
  | "SHOULDER"
  | "BACK"
  | "ARM"
  | "ABS"
  | "LEGS";

export type Exercise = {
  id: number;
  type: ExerciseType;
  name: string;
  category: ExerciseCategory;
};

export type WorkoutSet = {
  id: number;
  setNumber: number;
  weightKg: number;
  reps: number;
  durationSeconds: number | null;
  completed: boolean;
};

export type WorkoutExercise = {
  id: number;
  exerciseType: ExerciseType;
  exerciseId: number;
  exerciseName: string;
  category: ExerciseCategory;
  orderIndex: number;
  memo: string | null;
  sets: WorkoutSet[];
};

export type Workout = {
  id: number;
  workoutDate: string;
  status: "IN_PROGRESS" | "COMPLETED";
  memo: string | null;
  startedAt: string;
  completedAt: string | null;
  exercises: WorkoutExercise[];
};

export type PreviousExerciseRecord = {
  workoutId: number;
  workoutDate: string;
  workoutExerciseId: number;
  exerciseType: ExerciseType;
  exerciseId: number;
  exerciseName: string;
  sets: WorkoutSet[];
};

export type WorkoutCalendarDay = {
  date: string;
  workoutCount: number;
  completedCount: number;
  exerciseNames: string[];
};

export type WorkoutSetInput = {
  weightKg: number;
  reps: number;
  durationSeconds?: number | null;
};

export const exerciseCategories: { value: ExerciseCategory; label: string }[] = [
  { value: "CHEST", label: "가슴" },
  { value: "SHOULDER", label: "어깨" },
  { value: "BACK", label: "등" },
  { value: "ARM", label: "팔" },
  { value: "ABS", label: "복근" },
  { value: "LEGS", label: "하체" },
];

export function exerciseKey(type: ExerciseType, id: number) {
  return type + ":" + id;
}

export function categoryLabel(category: ExerciseCategory) {
  return exerciseCategories.find((item) => item.value === category)?.label ?? category;
}

export const workoutApi = {
  getExercises: () => request<Exercise[]>("/api/exercises"),
  createExercise: (name: string, category: ExerciseCategory) =>
    request<Exercise>("/api/exercises/custom", {
      method: "POST",
      body: JSON.stringify({ name, category }),
    }),
  getPreviousRecord: (exerciseType: ExerciseType, exerciseId: number) =>
    request<PreviousExerciseRecord | null>(
      "/api/exercises/" + exerciseType + "/" + exerciseId + "/previous-record",
    ),
  getWorkouts: (from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const query = params.toString();
    return request<Workout[]>("/api/workouts" + (query ? "?" + query : ""));
  },
  getCalendar: (month: string) =>
    request<WorkoutCalendarDay[]>(
      "/api/workouts/calendar?month=" + encodeURIComponent(month),
    ),
  startWorkout: (workoutDate: string, memo?: string) =>
    request<Workout>("/api/workouts", {
      method: "POST",
      body: JSON.stringify({ workoutDate, memo: memo || null }),
    }),
  addExercise: (
    workoutId: number,
    exerciseType: ExerciseType,
    exerciseId: number,
  ) =>
    request<Workout>("/api/workouts/" + workoutId + "/exercises", {
      method: "POST",
      body: JSON.stringify({ exerciseType, exerciseId }),
    }),
  removeExercise: (workoutId: number, workoutExerciseId: number) =>
    request<Workout>(
      "/api/workouts/" + workoutId + "/exercises/" + workoutExerciseId,
      { method: "DELETE" },
    ),
  addSet: (
    workoutId: number,
    workoutExerciseId: number,
    weightKg: number,
    reps: number,
    durationSeconds: number | null,
  ) =>
    request<Workout>(
      "/api/workouts/" + workoutId + "/exercises/" + workoutExerciseId + "/sets",
      {
        method: "POST",
        body: JSON.stringify({
          weightKg,
          reps,
          durationSeconds,
          completed: true,
        }),
      },
    ),
  addSets: (
    workoutId: number,
    workoutExerciseId: number,
    sets: WorkoutSetInput[],
  ) =>
    request<Workout>(
      "/api/workouts/" + workoutId + "/exercises/" + workoutExerciseId + "/sets/batch",
      {
        method: "POST",
        body: JSON.stringify({
          sets: sets.map((set) => ({
            weightKg: set.weightKg,
            reps: set.reps,
            durationSeconds: set.durationSeconds ?? null,
            completed: true,
          })),
        }),
      },
    ),
  updateSet: (
    workoutId: number,
    workoutExerciseId: number,
    setId: number,
    weightKg: number,
    reps: number,
    durationSeconds: number | null,
  ) =>
    request<Workout>(
      "/api/workouts/" + workoutId + "/exercises/" + workoutExerciseId + "/sets/" + setId,
      {
        method: "PATCH",
        body: JSON.stringify({
          weightKg,
          reps,
          durationSeconds,
          completed: true,
        }),
      },
    ),
  removeSet: (workoutId: number, workoutExerciseId: number, setId: number) =>
    request<Workout>(
      "/api/workouts/" + workoutId + "/exercises/" + workoutExerciseId + "/sets/" + setId,
      { method: "DELETE" },
    ),
  completeWorkout: (workoutId: number) =>
    request<Workout>("/api/workouts/" + workoutId + "/complete", {
      method: "PATCH",
    }),
};
