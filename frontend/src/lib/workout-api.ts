import { request } from "@/lib/api-client";

export type Exercise = {
  id: number;
  name: string;
  category: string | null;
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
  exerciseId: number;
  exerciseName: string;
  category: string | null;
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
  exerciseId: number;
  exerciseName: string;
  sets: WorkoutSet[];
};

export const workoutApi = {
  getExercises: () => request<Exercise[]>("/api/exercises"),
  createExercise: (name: string, category?: string) =>
    request<Exercise>("/api/exercises", {
      method: "POST",
      body: JSON.stringify({ name, category: category || null }),
    }),
  getPreviousRecord: (exerciseId: number) =>
    request<PreviousExerciseRecord | null>(
      `/api/exercises/${exerciseId}/previous-record`,
    ),
  getWorkouts: () => request<Workout[]>("/api/workouts"),
  startWorkout: (memo?: string) =>
    request<Workout>("/api/workouts", {
      method: "POST",
      body: JSON.stringify({ memo: memo || null }),
    }),
  addExercise: (workoutId: number, exerciseId: number) =>
    request<Workout>(`/api/workouts/${workoutId}/exercises`, {
      method: "POST",
      body: JSON.stringify({ exerciseId }),
    }),
  removeExercise: (workoutId: number, workoutExerciseId: number) =>
    request<Workout>(
      `/api/workouts/${workoutId}/exercises/${workoutExerciseId}`,
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
      `/api/workouts/${workoutId}/exercises/${workoutExerciseId}/sets`,
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
  updateSet: (
    workoutId: number,
    workoutExerciseId: number,
    setId: number,
    weightKg: number,
    reps: number,
    durationSeconds: number | null,
  ) =>
    request<Workout>(
      `/api/workouts/${workoutId}/exercises/${workoutExerciseId}/sets/${setId}`,
      {
        method: "PATCH",
        body: JSON.stringify({ weightKg, reps, durationSeconds, completed: true }),
      },
    ),
  removeSet: (
    workoutId: number,
    workoutExerciseId: number,
    setId: number,
  ) =>
    request<Workout>(
      `/api/workouts/${workoutId}/exercises/${workoutExerciseId}/sets/${setId}`,
      { method: "DELETE" },
    ),
  completeWorkout: (workoutId: number) =>
    request<Workout>(`/api/workouts/${workoutId}/complete`, {
      method: "PATCH",
    }),
};
