import { request } from "@/lib/api-client";
import {
  ExerciseType,
  PreviousExerciseRecord,
  Workout,
} from "@/lib/workout-api";

export type RoutineExercise = {
  id: number;
  exerciseType: ExerciseType;
  exerciseId: number;
  exerciseName: string;
  category: string;
  orderIndex: number;
};

export type Routine = {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
  exercises: RoutineExercise[];
};

export type RoutineExerciseRequest = {
  exerciseType: ExerciseType;
  exerciseId: number;
};

export type RoutineWorkoutStart = {
  workout: Workout;
  previousRecords: PreviousExerciseRecord[];
};

export const routineApi = {
  getRoutines: () => request<Routine[]>("/api/routines"),
  createRoutine: (name: string, exercises: RoutineExerciseRequest[]) =>
    request<Routine>("/api/routines", {
      method: "POST",
      body: JSON.stringify({ name, exercises }),
    }),
  updateRoutine: (
    routineId: number,
    name: string,
    exercises: RoutineExerciseRequest[],
  ) =>
    request<Routine>("/api/routines/" + routineId, {
      method: "PUT",
      body: JSON.stringify({ name, exercises }),
    }),
  deleteRoutine: (routineId: number) =>
    request<void>("/api/routines/" + routineId, { method: "DELETE" }),
  startWorkout: (routineId: number, workoutDate: string) =>
    request<RoutineWorkoutStart>("/api/routines/" + routineId + "/workouts", {
      method: "POST",
      body: JSON.stringify({ workoutDate }),
    }),
};
