"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Dashboard,
  DashboardBodyPoint,
  DashboardExercise,
  dashboardApi,
} from "@/lib/dashboard-api";
import { localDateString } from "@/lib/input-utils";
import {
  ExerciseCategory,
  categoryLabel,
  exerciseCategories,
  exerciseKey,
} from "@/lib/workout-api";

type ChartPoint = {
  label: string;
  value: number;
};

export default function DashboardScreen() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [selectedExerciseKey, setSelectedExerciseKey] =
    useState<string>("");
  const [selectedVolumeCategory, setSelectedVolumeCategory] =
    useState<"ALL" | ExerciseCategory>("ALL");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      setError(null);
      try {
        const result = await dashboardApi.get();
        if (cancelled) return;

        setDashboard(result);
        setSelectedExerciseKey((current) => {
          if (
            current &&
            result.exercises.some(
              (exercise) =>
                exerciseKey(
                  exercise.exerciseType,
                  exercise.exerciseId,
                ) === current,
            )
          ) {
            return current;
          }
          const first = result.exercises[0];
          return first
            ? exerciseKey(first.exerciseType, first.exerciseId)
            : "";
        });
      } catch (caught) {
        if (!cancelled) {
          setError(
            caught instanceof Error
              ? caught.message
              : "Dashboard를 불러오지 못했습니다.",
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
  }, []);

  const selectedExercise = useMemo(
    () =>
      dashboard?.exercises.find(
        (exercise) =>
          exerciseKey(
            exercise.exerciseType,
            exercise.exerciseId,
          ) === selectedExerciseKey,
      ) ?? null,
    [dashboard, selectedExerciseKey],
  );

  if (busy && !dashboard) {
    return (
      <main className="mx-auto min-h-screen w-full max-w-3xl px-4 py-8 sm:px-6">
        <div className="rounded-3xl border border-zinc-200 bg-white px-5 py-12 text-center text-sm text-zinc-400">
          Dashboard를 계산하고 있습니다.
        </div>
      </main>
    );
  }

  if (error && !dashboard) {
    return (
      <main className="mx-auto min-h-screen w-full max-w-3xl px-4 py-8 sm:px-6">
        <div className="rounded-3xl border border-red-200 bg-red-50 px-5 py-5 text-sm text-red-700">
          {error}
        </div>
      </main>
    );
  }

  if (!dashboard) return null;

  const workout = dashboard.workout;
  const body = dashboard.body;

  const selectedVolumeSeries =
    selectedVolumeCategory === "ALL"
      ? workout.dailyVolumes
      : workout.categoryDailyVolumes.find(
          (series) =>
            series.category === selectedVolumeCategory,
        )?.dailyVolumes ?? [];

  const volumePoints: ChartPoint[] = [
    ...selectedVolumeSeries,
  ]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((point) => ({
      label: point.date.slice(5),
      value: point.volume,
    }));

  const selectedVolumeTotal = selectedVolumeSeries.reduce(
    (total, point) => total + point.volume,
    0,
  );
  const selectedVolumeLabel =
    selectedVolumeCategory === "ALL"
      ? "전체"
      : categoryLabel(selectedVolumeCategory);

  return (
    <main className="mx-auto min-h-screen w-full max-w-3xl px-4 py-6 sm:px-6">
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <div className="mt-2 flex items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-zinc-950">
              Dashboard
            </h1>
            <p className="mt-2 text-sm leading-6 text-zinc-500">
              최근 운동량과 신체 변화, 종목별 기록을 한 번에 확인합니다.
            </p>
          </div>
          <span className="shrink-0 rounded-full bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white">
            {dashboard.generatedDate}
          </span>
        </div>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <section>
        <SectionHeading
          title="운동 요약"
          description="완료된 Workout만 집계합니다."
        />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <MetricCard
            label="최근 7일"
            value={workout.last7DaysWorkoutCount + "회"}
            detail="운동 횟수"
          />
          <MetricCard
            label="최근 30일"
            value={workout.last30DaysWorkoutCount + "회"}
            detail="운동 횟수"
          />
          <MetricCard
            label="7일 Volume"
            value={formatVolume(workout.last7DaysVolume)}
            detail={
              changeText(
                workout.last7DaysVolumeChangePercentage,
              ) + " · 직전 7일"
            }
          />
          <MetricCard
            label="30일 Volume"
            value={formatVolume(workout.last30DaysVolume)}
            detail={
              changeText(
                workout.last30DaysVolumeChangePercentage,
              ) + " · 직전 30일"
            }
          />
        </div>

        <div className="mt-3 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
          <div className="flex items-end justify-between gap-3">
            <div>
              <h3 className="font-semibold">최근 30일 Volume</h3>
              <p className="mt-1 text-xs text-zinc-400">
                선택한 카테고리의 중량 × 반복 횟수 일별 합계
              </p>
            </div>
            <span className="shrink-0 text-right text-xs font-medium text-zinc-400">
              {selectedVolumeLabel}
              <br />
              {formatVolume(selectedVolumeTotal)}
            </span>
          </div>

          <div className="mt-4 flex gap-2 overflow-x-auto pb-1">
            <button
              type="button"
              onClick={() => setSelectedVolumeCategory("ALL")}
              className={
                "shrink-0 rounded-full px-3 py-1.5 text-xs font-semibold transition " +
                (selectedVolumeCategory === "ALL"
                  ? "bg-zinc-950 text-white"
                  : "bg-zinc-100 text-zinc-500")
              }
            >
              전체
            </button>
            {exerciseCategories.map((category) => (
              <button
                key={category.value}
                type="button"
                onClick={() =>
                  setSelectedVolumeCategory(category.value)
                }
                className={
                  "shrink-0 rounded-full px-3 py-1.5 text-xs font-semibold transition " +
                  (selectedVolumeCategory === category.value
                    ? "bg-zinc-950 text-white"
                    : "bg-zinc-100 text-zinc-500")
                }
              >
                {category.label}
              </button>
            ))}
          </div>

          <LineChart
            points={volumePoints}
            emptyText={
              "최근 30일 " +
              selectedVolumeLabel +
              " Volume 기록이 없습니다."
            }
            valueFormatter={formatVolume}
          />
        </div>
      </section>

      <section className="mt-8">
        <SectionHeading
          title="Body"
          description="최근 측정값과 바로 이전 측정값의 차이입니다."
        />

        {body.latest ? (
          <>
            <div className="grid grid-cols-3 gap-3">
              <BodyMetricCard
                label="체중"
                value={body.latest.weightKg}
                unit="kg"
                delta={body.changeFromPrevious?.weightKg ?? null}
              />
              <BodyMetricCard
                label="체지방"
                value={body.latest.bodyFatPercentage}
                unit="%"
                delta={
                  body.changeFromPrevious
                    ?.bodyFatPercentage ?? null
                }
              />
              <BodyMetricCard
                label="골격근"
                value={body.latest.skeletalMuscleKg}
                unit="kg"
                delta={
                  body.changeFromPrevious
                    ?.skeletalMuscleKg ?? null
                }
              />
            </div>

            <div className="mt-3 grid gap-3 sm:grid-cols-3">
              <BodyTrendCard
                title="체중 추이"
                points={body.history}
                selector={(point) => point.weightKg}
                unit="kg"
              />
              <BodyTrendCard
                title="체지방 추이"
                points={body.history}
                selector={(point) => point.bodyFatPercentage}
                unit="%"
              />
              <BodyTrendCard
                title="골격근 추이"
                points={body.history}
                selector={(point) => point.skeletalMuscleKg}
                unit="kg"
              />
            </div>
          </>
        ) : (
          <EmptyCard text="아직 Body 기록이 없습니다. Body 탭에서 첫 측정값을 등록해주세요." />
        )}
      </section>

      <section className="mt-8">
        <SectionHeading
          title="종목별 기록"
          description="완료된 전체 기록에서 최고 중량과 Epley 추정 1RM을 계산합니다."
        />

        {dashboard.exercises.length === 0 ? (
          <EmptyCard text="완료된 운동 기록이 쌓이면 종목별 PR과 추이가 표시됩니다." />
        ) : (
          <>
            <div className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
              <label className="text-xs font-medium text-zinc-500">
                운동 종목
                <select
                  value={selectedExerciseKey}
                  onChange={(event) =>
                    setSelectedExerciseKey(event.target.value)
                  }
                  className="mt-2 w-full rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm font-semibold outline-none focus:border-zinc-500"
                >
                  {dashboard.exercises.map((exercise) => (
                    <option
                      key={exerciseKey(
                        exercise.exerciseType,
                        exercise.exerciseId,
                      )}
                      value={exerciseKey(
                        exercise.exerciseType,
                        exercise.exerciseId,
                      )}
                    >
                      {exercise.exerciseName} ·{" "}
                      {categoryLabel(exercise.category)}
                    </option>
                  ))}
                </select>
              </label>

              {selectedExercise && (
                <ExerciseDetail exercise={selectedExercise} />
              )}
            </div>

            <div className="mt-3 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
              <h3 className="font-semibold">최근 사용 종목 PR</h3>
              <div className="mt-3 divide-y divide-zinc-100">
                {dashboard.exercises.slice(0, 6).map((exercise) => (
                  <div
                    key={exerciseKey(
                      exercise.exerciseType,
                      exercise.exerciseId,
                    )}
                    className="flex items-center justify-between gap-4 py-3 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">
                        {exercise.exerciseName}
                      </p>
                      <p className="mt-0.5 text-xs text-zinc-400">
                        {categoryLabel(exercise.category)} · 최근{" "}
                        {exercise.latestWorkoutDate}
                      </p>
                    </div>
                    <div className="shrink-0 text-right">
                      <p className="text-sm font-semibold">
                        {formatDecimal(exercise.maxWeightKg)} kg
                      </p>
                      <p className="text-xs text-zinc-400">
                        1RM{" "}
                        {formatDecimal(
                          exercise.maxEstimatedOneRepMax,
                        )}{" "}
                        kg
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </section>
    </main>
  );
}

function ExerciseDetail({
  exercise,
}: {
  exercise: DashboardExercise;
}) {
  const orderedRecords = [...exercise.recentRecords].sort(
    (a, b) => a.workoutDate.localeCompare(b.workoutDate),
  );
  const maxWeightPoints = orderedRecords.map(
    (record) => ({
      label: record.workoutDate.slice(5),
      value: record.maxWeightKg,
    }),
  );
  const oneRepMaxPoints = orderedRecords.map(
    (record) => ({
      label: record.workoutDate.slice(5),
      value: record.maxEstimatedOneRepMax,
    }),
  );

  return (
    <div className="mt-5">
      <div className="grid grid-cols-2 gap-3">
        <MetricCard
          label="최고 중량"
          value={formatDecimal(exercise.maxWeightKg) + " kg"}
          detail="전체 완료 기록"
        />
        <MetricCard
          label="최고 추정 1RM"
          value={
            formatDecimal(
              exercise.maxEstimatedOneRepMax,
            ) + " kg"
          }
          detail="Epley 공식"
        />
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <ChartCard
          title="최근 최고 중량"
          points={maxWeightPoints}
          unit="kg"
        />
        <ChartCard
          title="최근 추정 1RM"
          points={oneRepMaxPoints}
          unit="kg"
        />
      </div>

      <div className="mt-4">
        <p className="text-xs font-semibold text-zinc-500">
          최근 기록
        </p>
        <div className="mt-2 space-y-2">
          {[...orderedRecords]
            .reverse()
            .slice(0, 5)
            .map((record, index) => (
              <div
                key={record.workoutDate + "-" + index}
                className="grid grid-cols-[1fr_auto_auto] items-center gap-3 rounded-xl bg-zinc-50 px-3 py-2.5 text-xs"
              >
                <span className="font-medium text-zinc-600">
                  {record.workoutDate}
                </span>
                <span className="text-zinc-500">
                  {formatDecimal(record.maxWeightKg)}kg
                </span>
                <span className="font-semibold text-zinc-700">
                  1RM{" "}
                  {formatDecimal(
                    record.maxEstimatedOneRepMax,
                  )}
                </span>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
}

function BodyTrendCard({
  title,
  points,
  selector,
  unit,
}: {
  title: string;
  points: DashboardBodyPoint[];
  selector: (point: DashboardBodyPoint) => number;
  unit: string;
}) {
  const chartPoints = points.map((point) => ({
      label: localDateString(
        new Date(point.measuredAt),
      ).slice(5),
      value: selector(point),
    }));

  return (
    <ChartCard
      title={title}
      points={chartPoints}
      unit={unit}
    />
  );
}

function ChartCard({
  title,
  points,
  unit,
}: {
  title: string;
  points: ChartPoint[];
  unit: string;
}) {
  return (
    <div className="rounded-2xl border border-zinc-200 bg-white p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-semibold">{title}</p>
        {points.length > 0 && (
          <span className="text-xs text-zinc-400">
            {formatDecimal(points.at(-1)?.value ?? 0)} {unit}
          </span>
        )}
      </div>
      <LineChart
        points={points}
        compact
        emptyText="기록 없음"
        valueFormatter={(value) =>
          formatDecimal(value) + unit
        }
      />
    </div>
  );
}

function LineChart({
  points,
  emptyText,
  valueFormatter,
  compact = false,
}: {
  points: ChartPoint[];
  emptyText: string;
  valueFormatter: (value: number) => string;
  compact?: boolean;
}) {
  const [selection, setSelection] = useState<{
    seriesKey: string;
    index: number;
  } | null>(null);
  const seriesKey = points
    .map((point) => point.label + ":" + point.value)
    .join("|");
  const activeIndex =
    selection?.seriesKey === seriesKey
      ? selection.index
      : null;

  const hasValue = points.some((point) => point.value !== 0);
  if (points.length < 2 || !hasValue) {
    return (
      <div
        className={
          "mt-4 flex items-center justify-center rounded-xl bg-zinc-50 text-center text-xs text-zinc-400 " +
          (compact ? "h-24" : "h-40")
        }
      >
        {emptyText}
      </div>
    );
  }

  const width = 600;
  const height = compact ? 120 : 180;
  const paddingX = 18;
  const paddingY = 18;
  const values = points.map((point) => point.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const coordinates = points.map((point, index) => {
    const x =
      paddingX +
      (index / Math.max(1, points.length - 1)) *
        (width - paddingX * 2);
    const y =
      height -
      paddingY -
      ((point.value - min) / range) *
        (height - paddingY * 2);
    return { x, y };
  });

  const polyline = coordinates
    .map((point) => point.x + "," + point.y)
    .join(" ");

  return (
    <div className="mt-4">
      <div className="relative text-zinc-900">
        <svg
          viewBox={"0 0 " + width + " " + height}
          className={compact ? "h-24 w-full" : "h-40 w-full"}
          role="img"
          aria-label="추이 차트"
          preserveAspectRatio="none"
        >
          <line
            x1={paddingX}
            y1={height - paddingY}
            x2={width - paddingX}
            y2={height - paddingY}
            stroke="currentColor"
            strokeOpacity="0.08"
          />
          <line
            x1={paddingX}
            y1={paddingY}
            x2={width - paddingX}
            y2={paddingY}
            stroke="currentColor"
            strokeOpacity="0.05"
          />
          <polyline
            points={polyline}
            fill="none"
            stroke="currentColor"
            strokeWidth="3"
            vectorEffect="non-scaling-stroke"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {coordinates.map((point, index) => (
            <circle
              key={index}
              cx={point.x}
              cy={point.y}
              r={activeIndex === index ? (compact ? 4 : 4.5) : compact ? 2.5 : 3}
              fill="currentColor"
            />
          ))}
        </svg>

        {coordinates.map((coordinate, index) => {
          const point = points[index];
          const active = activeIndex === index;
          const showBelow = coordinate.y < height * 0.32;
          const horizontalAlignment =
            index === 0
              ? "left-1/2"
              : index === coordinates.length - 1
                ? "right-1/2"
                : "left-1/2 -translate-x-1/2";

          return (
            <button
              key={point.label + "-" + index}
              type="button"
              aria-label={
                point.label + " " + valueFormatter(point.value)
              }
              aria-pressed={active}
              onClick={() =>
                setSelection({ seriesKey, index })
              }
              onMouseEnter={() =>
                setSelection({ seriesKey, index })
              }
              onMouseLeave={() =>
                setSelection((current) =>
                  current?.seriesKey === seriesKey &&
                  current.index === index
                    ? null
                    : current,
                )
              }
              onFocus={() =>
                setSelection({ seriesKey, index })
              }
              onBlur={() =>
                setSelection((current) =>
                  current?.seriesKey === seriesKey &&
                  current.index === index
                    ? null
                    : current,
                )
              }
              className="absolute z-10 h-9 w-9 -translate-x-1/2 -translate-y-1/2 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
              style={{
                left: (coordinate.x / width) * 100 + "%",
                top: (coordinate.y / height) * 100 + "%",
              }}
            >
              {active && (
                <span
                  role="tooltip"
                  className={
                    "pointer-events-none absolute z-20 min-w-max rounded-lg bg-zinc-950 px-2.5 py-1.5 text-left text-[11px] font-medium leading-4 text-white shadow-lg " +
                    horizontalAlignment +
                    (showBelow
                      ? " top-[calc(100%+4px)]"
                      : " bottom-[calc(100%+4px)]")
                  }
                >
                  <span className="block text-zinc-300">
                    {point.label}
                  </span>
                  <span className="block text-xs font-bold text-white">
                    {valueFormatter(point.value)}
                  </span>
                </span>
              )}
            </button>
          );
        })}
      </div>

      <div className="mt-1 flex items-center justify-between text-[10px] text-zinc-400">
        <span>{points[0]?.label}</span>
        <span>
          {valueFormatter(max)} / {valueFormatter(min)}
        </span>
        <span>{points.at(-1)?.label}</span>
      </div>
    </div>
  );
}

function SectionHeading({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="mb-3">
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="mt-1 text-xs leading-5 text-zinc-400">
        {description}
      </p>
    </div>
  );
}

function MetricCard({
  label,
  value,
  detail,
}: {
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <div className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-medium text-zinc-400">
        {label}
      </p>
      <p className="mt-2 text-xl font-bold tracking-tight text-zinc-950">
        {value}
      </p>
      <p className="mt-1 text-[11px] leading-4 text-zinc-400">
        {detail}
      </p>
    </div>
  );
}

function BodyMetricCard({
  label,
  value,
  unit,
  delta,
}: {
  label: string;
  value: number;
  unit: string;
  delta: number | null;
}) {
  return (
    <div className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-medium text-zinc-400">
        {label}
      </p>
      <p className="mt-2 text-lg font-bold tracking-tight">
        {formatDecimal(value)}
        <span className="ml-1 text-xs font-medium text-zinc-400">
          {unit}
        </span>
      </p>
      <p className="mt-1 text-[11px] text-zinc-400">
        {delta === null
          ? "이전 기록 없음"
          : signedNumber(delta) + " " + unit}
      </p>
    </div>
  );
}

function EmptyCard({ text }: { text: string }) {
  return (
    <div className="rounded-3xl border border-dashed border-zinc-200 bg-white px-5 py-8 text-center text-sm text-zinc-400">
      {text}
    </div>
  );
}

function formatVolume(value: number) {
  return new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDecimal(value: number) {
  return new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 2,
  }).format(value);
}

function signedNumber(value: number) {
  const prefix = value > 0 ? "+" : "";
  return prefix + formatDecimal(value);
}

function changeText(value: number | null) {
  if (value === null) return "비교 데이터 없음";
  if (value === 0) return "변화 없음";
  return signedNumber(value) + "%";
}
