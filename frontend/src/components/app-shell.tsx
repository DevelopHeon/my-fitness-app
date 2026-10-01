"use client";

import { useState } from "react";
import AiCoach from "@/components/ai/ai-coach";
import BodyScreen from "@/components/body/body-screen";
import DashboardScreen from "@/components/dashboard/dashboard-screen";
import NutritionScreen from "@/components/nutrition/nutrition-screen";
import RoutineScreen from "@/components/routine/routine-screen";
import WorkoutScreen from "@/components/workout/workout-screen";
import { todayString } from "@/lib/input-utils";
import { RoutineWorkoutStart } from "@/lib/routine-api";
import { MealDraft, mealDefaults } from "@/lib/meal-input";
import { CurrentUser } from "@/lib/auth-api";

type View =
  | "dashboard"
  | "workout"
  | "routine"
  | "body"
  | "nutrition";

export default function AppShell({
  user,
  loggingOut,
  onLogout,
}: {
  user: CurrentUser;
  loggingOut: boolean;
  onLogout: () => void;
}) {
  const [view, setView] = useState<View>("dashboard");
  const [workoutDate, setWorkoutDate] = useState(todayString);
  const [nutritionDate, setNutritionDate] = useState(todayString);
  const [startedWorkout, setStartedWorkout] =
    useState<RoutineWorkoutStart | null>(null);

  const [nutritionDraft, setNutritionDraft] = useState<MealDraft | null>(null);
  const [nutritionInstance, setNutritionInstance] = useState(0);
  const [aiCoachOpen, setAiCoachOpen] = useState(false);

  function openWorkout(date = todayString()) {
    setNutritionDraft(null);
    setStartedWorkout(null);
    setWorkoutDate(date);
    setView("workout");
  }

  function handleWorkoutStarted(result: RoutineWorkoutStart) {
    setNutritionDraft(null);
    setStartedWorkout(result);
    setWorkoutDate(result.workout.workoutDate);
    setView("workout");
  }

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-950">
      <header className="border-b border-zinc-200 bg-white">
        <div className="mx-auto flex w-full max-w-3xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-zinc-400">
              My Fitness
            </p>
            <p className="truncate text-sm font-semibold text-zinc-900">
              {user.displayName || user.email || "사용자"}
            </p>
          </div>
          <button
            type="button"
            onClick={onLogout}
            disabled={loggingOut}
            className="shrink-0 rounded-lg border border-zinc-200 px-3 py-2 text-xs font-semibold text-zinc-600 transition hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loggingOut ? "로그아웃 중..." : "로그아웃"}
          </button>
        </div>
      </header>

      <nav className="sticky top-0 z-20 border-b border-zinc-200 bg-white/95 backdrop-blur">
        <div className="mx-auto grid w-full max-w-3xl grid-cols-5 gap-1 px-3 py-3 sm:px-6">
          <NavButton
            active={view === "dashboard"}
            label="Dashboard"
            onClick={() => { setNutritionDraft(null); setView("dashboard"); }}
          />
          <NavButton
            active={view === "workout"}
            label="Workout"
            onClick={() => openWorkout()}
          />
          <NavButton
            active={view === "routine"}
            label="Routine"
            onClick={() => { setNutritionDraft(null); setView("routine"); }}
          />
          <NavButton
            active={view === "body"}
            label="Body"
            onClick={() => { setNutritionDraft(null); setView("body"); }}
          />
          <NavButton
            active={view === "nutrition"}
            label="Nutrition"
            onClick={() => { setNutritionDraft(null); setView("nutrition"); }}
          />
        </div>
      </nav>

      {view === "dashboard" ? (
        <DashboardScreen />
      ) : view === "workout" ? (
        <WorkoutScreen
          key={
            (startedWorkout?.workout.id ?? "workout") +
            "-" +
            workoutDate
          }
          selectedDate={workoutDate}
          onSelectedDateChange={openWorkout}
          initialWorkout={startedWorkout?.workout ?? null}
          initialPreviousRecords={
            startedWorkout?.previousRecords
          }
        />
      ) : view === "routine" ? (
        <RoutineScreen onWorkoutStarted={handleWorkoutStarted} />
      ) : view === "body" ? (
        <BodyScreen />
      ) : (
        <NutritionScreen
          key={nutritionInstance}
          onSelectedDateChange={setNutritionDate}
          initialDraft={nutritionDraft}
          onDraftConsumed={() => setNutritionDraft(null)}
        />
      )}

      <AiCoach
        open={aiCoachOpen}
        onOpenChange={setAiCoachOpen}
        onRecordFoods={(items) => {
          setNutritionDraft({ items, ...mealDefaults() });
          setNutritionInstance((value) => value + 1);
          setView("nutrition");
        }}
        currentView={view}
        selectedDate={
          view === "workout"
            ? workoutDate
            : view === "nutrition"
              ? nutritionDate
              : undefined
        }
      />
    </div>
  );
}

function NavButton({
  active,
  label,
  onClick,
}: {
  active: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "min-h-11 min-w-0 rounded-xl px-0.5 py-2.5 text-[10px] font-semibold sm:px-2 sm:text-sm " +
        (active
          ? "bg-zinc-950 text-white"
          : "text-zinc-500 hover:bg-zinc-100")
      }
    >
      {label}
    </button>
  );
}
