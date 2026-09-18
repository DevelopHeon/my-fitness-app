import { request } from "@/lib/api-client";
import {
  ExerciseCategory,
  ExerciseType,
} from "@/lib/workout-api";

export type DashboardDailyVolume = {
  date: string;
  volume: number;
};

export type DashboardBodyPoint = {
  measuredAt: string;
  weightKg: number;
  bodyFatPercentage: number;
  skeletalMuscleKg: number;
};

export type DashboardBodyChange = {
  weightKg: number;
  bodyFatPercentage: number;
  skeletalMuscleKg: number;
};

export type DashboardExerciseRecord = {
  workoutDate: string;
  volume: number;
  maxWeightKg: number;
  maxEstimatedOneRepMax: number;
};

export type DashboardExercise = {
  exerciseType: ExerciseType;
  exerciseId: number;
  exerciseName: string;
  category: ExerciseCategory;
  maxWeightKg: number;
  maxEstimatedOneRepMax: number;
  latestWorkoutDate: string;
  recentRecords: DashboardExerciseRecord[];
};

export type Dashboard = {
  generatedDate: string;
  workout: {
    last7DaysWorkoutCount: number;
    last30DaysWorkoutCount: number;
    last7DaysVolume: number;
    previous7DaysVolume: number;
    last7DaysVolumeChangePercentage: number | null;
    last30DaysVolume: number;
    previous30DaysVolume: number;
    last30DaysVolumeChangePercentage: number | null;
    dailyVolumes: DashboardDailyVolume[];
  };
  body: {
    latest: DashboardBodyPoint | null;
    changeFromPrevious: DashboardBodyChange | null;
    history: DashboardBodyPoint[];
  };
  exercises: DashboardExercise[];
};

export const dashboardApi = {
  get: () => request<Dashboard>("/api/dashboard"),
};
