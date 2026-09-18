"use client";

import { useState } from "react";
import RoutineScreen from "@/components/routine/routine-screen";
import WorkoutScreen from "@/components/workout/workout-screen";
import { RoutineWorkoutStart } from "@/lib/routine-api";

type View = "workout" | "routine";

export default function AppShell() {
  const [view, setView] = useState<View>("workout");
  const [startedWorkout, setStartedWorkout] =
    useState<RoutineWorkoutStart | null>(null);

  function openWorkout() {
    setStartedWorkout(null);
    setView("workout");
  }

  function handleWorkoutStarted(result: RoutineWorkoutStart) {
    setStartedWorkout(result);
    setView("workout");
  }

  return (
    <div className="min-h-screen bg-zinc-50 text-zinc-950">
      <nav className="sticky top-0 z-20 border-b border-zinc-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-2xl gap-2 px-4 py-3 sm:px-6">
          <button
            type="button"
            onClick={openWorkout}
            className={
              "flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold " +
              (view === "workout"
                ? "bg-zinc-950 text-white"
                : "text-zinc-500 hover:bg-zinc-100")
            }
          >
            Workout
          </button>
          <button
            type="button"
            onClick={() => setView("routine")}
            className={
              "flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold " +
              (view === "routine"
                ? "bg-zinc-950 text-white"
                : "text-zinc-500 hover:bg-zinc-100")
            }
          >
            Routine
          </button>
        </div>
      </nav>

      {view === "workout" ? (
        <WorkoutScreen
          key={startedWorkout?.workout.id ?? "workout"}
          initialWorkout={startedWorkout?.workout ?? null}
          initialPreviousRecords={startedWorkout?.previousRecords ?? []}
        />
      ) : (
        <RoutineScreen onWorkoutStarted={handleWorkoutStarted} />
      )}
    </div>
  );
}
