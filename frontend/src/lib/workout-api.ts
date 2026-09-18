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
  exerciseName: string;
  sets: WorkoutSet[];
};

const USER_ID = "1";

function apiBase() {
  if (process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }
  if (typeof window !== "undefined" && window.location.port === "3000") {
    return "http://localhost:8080";
  }
  return "";
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": USER_ID,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.message ?? "요청 처리 중 오류가 발생했습니다.");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

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
  ) =>
    request<Workout>(
      `/api/workouts/${workoutId}/exercises/${workoutExerciseId}/sets`,
      {
        method: "POST",
        body: JSON.stringify({
          weightKg,
          reps,
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
  ) =>
    request<Workout>(
      `/api/workouts/${workoutId}/exercises/${workoutExerciseId}/sets/${setId}`,
      {
        method: "PATCH",
        body: JSON.stringify({ weightKg, reps, completed: true }),
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
