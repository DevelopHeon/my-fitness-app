import { request } from "@/lib/api-client";
import {
  PreviousExerciseRecord,
  Workout,
} from "@/lib/workout-api";

export type RoutineExercise = {
  id: number;
  exerciseId: number;
  exerciseName: string;
  category: string | null;
  orderIndex: number;
};

export type Routine = {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
  exercises: RoutineExercise[];
};

export type RoutineWorkoutStart = {
  workout: Workout;
  previousRecords: PreviousExerciseRecord[];
};

export const routineApi = {
  getRoutines: () => request<Routine[]>("/api/routines"),
  createRoutine: (name: string, exerciseIds: number[]) =>
    request<Routine>("/api/routines", {
      method: "POST",
      body: JSON.stringify({ name, exerciseIds }),
    }),
  updateRoutine: (routineId: number, name: string, exerciseIds: number[]) =>
    request<Routine>("/api/routines/" + routineId, {
      method: "PUT",
      body: JSON.stringify({ name, exerciseIds }),
    }),
  deleteRoutine: (routineId: number) =>
    request<void>("/api/routines/" + routineId, { method: "DELETE" }),
  startWorkout: (routineId: number) =>
    request<RoutineWorkoutStart>("/api/routines/" + routineId + "/workouts", {
      method: "POST",
      body: JSON.stringify({}),
    }),
};
