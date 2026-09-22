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

type View =
  | "dashboard"
  | "workout"
  | "routine"
  | "body"
  | "nutrition";

export default function AppShell() {
  const [view, setView] = useState<View>("dashboard");
  const [workoutDate, setWorkoutDate] = useState(todayString);
  const [nutritionDate, setNutritionDate] = useState(todayString);
  const [startedWorkout, setStartedWorkout] =
    useState<RoutineWorkoutStart | null>(null);

  function openWorkout(date = todayString()) {
    setStartedWorkout(null);
    setWorkoutDate(date);
    setView("workout");
  }

  function handleWorkoutStarted(result: RoutineWorkoutStart) {
    setStartedWorkout(result);
    setWorkoutDate(result.workout.workoutDate);
    setView("workout");
  }

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-950">
      <nav className="sticky top-0 z-20 border-b border-zinc-200 bg-white/95 backdrop-blur">
        <div className="mx-auto grid w-full max-w-3xl grid-cols-5 gap-1 px-3 py-3 sm:px-6">
          <NavButton
            active={view === "dashboard"}
            label="Dashboard"
            onClick={() => setView("dashboard")}
          />
          <NavButton
            active={view === "workout"}
            label="Workout"
            onClick={() => openWorkout()}
          />
          <NavButton
            active={view === "routine"}
            label="Routine"
            onClick={() => setView("routine")}
          />
          <NavButton
            active={view === "body"}
            label="Body"
            onClick={() => setView("body")}
          />
          <NavButton
            active={view === "nutrition"}
            label="Nutrition"
            onClick={() => setView("nutrition")}
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
        <NutritionScreen onSelectedDateChange={setNutritionDate} />
      )}

      <AiCoach
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
        "rounded-xl px-2 py-2.5 text-xs font-semibold sm:text-sm " +
        (active
          ? "bg-zinc-950 text-white"
          : "text-zinc-500 hover:bg-zinc-100")
      }
    >
      {label}
    </button>
  );
}
