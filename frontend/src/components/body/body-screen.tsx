"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  BodyRecord,
  BodyRecordInput,
  BodyTrend,
  bodyApi,
} from "@/lib/body-api";
import {
  currentTimeString,
  localDateString,
  localTimeString,
  sanitizeDecimal,
  sanitizeText,
  todayString,
} from "@/lib/input-utils";

type FormState = {
  weightKg: string;
  bodyFatPercentage: string;
  skeletalMuscleKg: string;
  measuredDate: string;
  measuredTime: string;
  memo: string;
};

function emptyForm(): FormState {
  return {
    weightKg: "",
    bodyFatPercentage: "",
    skeletalMuscleKg: "",
    measuredDate: todayString(),
    measuredTime: currentTimeString(),
    memo: "",
  };
}

function changeText(value: number) {
  if (value > 0) return "+" + value.toFixed(2);
  return value.toFixed(2);
}

function formatMeasuredAt(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function BodyScreen() {
  const [trend, setTrend] = useState<BodyTrend>({
    latest: null,
    changeFromPrevious: null,
    records: [],
  });
  const [form, setForm] = useState<FormState>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      try {
        const next = await bodyApi.getTrend(90);
        if (!cancelled) setTrend(next);
      } catch (caught) {
        if (!cancelled) {
          setError(
            caught instanceof Error
              ? caught.message
              : "신체 기록을 불러오지 못했습니다.",
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

  async function run<T>(action: () => Promise<T>) {
    setError(null);
    setBusy(true);
    try {
      return await action();
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "요청 처리 중 오류가 발생했습니다.",
      );
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function refreshTrend() {
    const next = await run(() => bodyApi.getTrend(90));
    if (next) setTrend(next);
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm());
  }

  function toInput(): BodyRecordInput | null {
    const weightKg = Number(form.weightKg);
    const bodyFatPercentage = Number(form.bodyFatPercentage);
    const skeletalMuscleKg = Number(form.skeletalMuscleKg);

    if (!form.measuredDate || !form.measuredTime) {
      setError("측정 날짜와 시간을 입력해주세요.");
      return null;
    }

    if (!Number.isFinite(weightKg) || weightKg <= 0) {
      setError("체중은 0보다 큰 숫자로 입력해주세요.");
      return null;
    }

    if (
      !Number.isFinite(bodyFatPercentage) ||
      bodyFatPercentage < 0 ||
      bodyFatPercentage > 100
    ) {
      setError("체지방률은 0에서 100 사이 숫자로 입력해주세요.");
      return null;
    }

    if (
      !Number.isFinite(skeletalMuscleKg) ||
      skeletalMuscleKg <= 0
    ) {
      setError("골격근량은 0보다 큰 숫자로 입력해주세요.");
      return null;
    }

    const measuredDate = new Date(
      form.measuredDate + "T" + form.measuredTime + ":00",
    );
    if (Number.isNaN(measuredDate.getTime())) {
      setError("측정 일시를 확인해주세요.");
      return null;
    }

    return {
      weightKg,
      bodyFatPercentage,
      skeletalMuscleKg,
      measuredAt: measuredDate.toISOString(),
      memo: form.memo.trim() || null,
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const input = toInput();
    if (!input) return;

    const saved = await run(() =>
      editingId
        ? bodyApi.updateRecord(editingId, input)
        : bodyApi.createRecord(input),
    );
    if (!saved) return;

    resetForm();
    await refreshTrend();
  }

  function handleEdit(record: BodyRecord) {
    const measuredAt = new Date(record.measuredAt);

    setEditingId(record.id);
    setForm({
      weightKg: String(record.weightKg),
      bodyFatPercentage: String(record.bodyFatPercentage),
      skeletalMuscleKg: String(record.skeletalMuscleKg),
      measuredDate: localDateString(measuredAt),
      measuredTime: localTimeString(measuredAt),
      memo: record.memo ?? "",
    });

    requestAnimationFrame(() => {
      document
        .getElementById("body-record-editor")
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
    });
  }

  async function handleDelete(recordId: number) {
    const deleted = await run(async () => {
      await bodyApi.deleteRecord(recordId);
      return true;
    });
    if (!deleted) return;

    if (editingId === recordId) resetForm();
    await refreshTrend();
  }

  const latest = trend.latest;
  const change = trend.changeFromPrevious;

  return (
    <main className="mx-auto min-h-screen w-full max-w-2xl px-4 py-6 sm:px-6">
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <div className="mt-2 flex items-center justify-between gap-3">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-zinc-950">
              Body
            </h1>
            <p className="mt-2 text-sm leading-6 text-zinc-500">
              체중과 체성분을 측정 시각 기준으로 기록하고 변화를 확인하세요.
            </p>
          </div>
          <span className="rounded-full bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white">
            Phase 3
          </span>
        </div>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="grid grid-cols-3 gap-2">
        <MetricCard
          label="체중"
          value={latest ? latest.weightKg.toFixed(2) : "-"}
          unit="kg"
          change={change?.weightKg}
        />
        <MetricCard
          label="체지방률"
          value={latest ? latest.bodyFatPercentage.toFixed(2) : "-"}
          unit="%"
          change={change?.bodyFatPercentage}
        />
        <MetricCard
          label="골격근량"
          value={latest ? latest.skeletalMuscleKg.toFixed(2) : "-"}
          unit="kg"
          change={change?.skeletalMuscleKg}
        />
      </section>

      {latest && (
        <p className="mt-2 text-right text-xs text-zinc-400">
          최근 측정 {formatMeasuredAt(latest.measuredAt)}
        </p>
      )}

      <section
        id="body-record-editor"
        className="mt-6 scroll-mt-20 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold">
              {editingId ? "신체 기록 수정" : "신체 기록 추가"}
            </h2>
            <p className="mt-1 text-xs text-zinc-400">
              같은 날 여러 번 측정해도 각각 저장됩니다.
            </p>
          </div>
          {editingId && (
            <button
              type="button"
              onClick={resetForm}
              className="text-xs font-medium text-zinc-400 hover:text-zinc-900"
            >
              수정 취소
            </button>
          )}
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div className="grid grid-cols-2 gap-2">
            <label className="text-xs font-medium text-zinc-500">
              측정 날짜
              <input
                type="date"
                max={todayString()}
                required
                value={form.measuredDate}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    measuredDate: event.target.value,
                  }))
                }
                className="mt-1.5 w-full rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
              />
            </label>
            <label className="text-xs font-medium text-zinc-500">
              측정 시간
              <input
                type="time"
                required
                value={form.measuredTime}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    measuredTime: event.target.value,
                  }))
                }
                className="mt-1.5 w-full rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
              />
            </label>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <MeasurementInput
              label="체중"
              unit="kg"
              value={form.weightKg}
              onChange={(value) =>
                setForm((current) => ({
                  ...current,
                  weightKg: value,
                }))
              }
            />
            <MeasurementInput
              label="체지방률"
              unit="%"
              value={form.bodyFatPercentage}
              onChange={(value) =>
                setForm((current) => ({
                  ...current,
                  bodyFatPercentage: value,
                }))
              }
            />
            <MeasurementInput
              label="골격근량"
              unit="kg"
              value={form.skeletalMuscleKg}
              onChange={(value) =>
                setForm((current) => ({
                  ...current,
                  skeletalMuscleKg: value,
                }))
              }
            />
          </div>

          <label className="block text-xs font-medium text-zinc-500">
            메모
            <textarea
              value={form.memo}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  memo: sanitizeText(event.target.value, 500),
                }))
              }
              rows={3}
              maxLength={500}
              placeholder="컨디션이나 측정 상황 메모"
              className="mt-1.5 w-full resize-none rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
            />
          </label>

          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
          >
            {editingId ? "기록 수정" : "기록 저장"}
          </button>
        </form>
      </section>

      <section className="mt-6">
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="font-semibold">최근 변화</h2>
            <p className="mt-1 text-xs text-zinc-400">
              최근 90일 측정 기록
            </p>
          </div>
          <span className="text-xs text-zinc-400">
            {trend.records.length}건
          </span>
        </div>

        <div className="space-y-3">
          {trend.records.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-400">
              아직 신체 기록이 없습니다.
            </div>
          ) : (
            trend.records.map((record) => (
              <article
                key={record.id}
                className="rounded-2xl border border-zinc-200 bg-white p-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-zinc-900">
                      {formatMeasuredAt(record.measuredAt)}
                    </p>
                    {record.memo && (
                      <p className="mt-1 text-xs text-zinc-400">
                        {record.memo}
                      </p>
                    )}
                  </div>
                  <div className="flex gap-3 text-xs font-medium">
                    <button
                      type="button"
                      onClick={() => handleEdit(record)}
                      className="text-zinc-500 hover:text-zinc-900"
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => {
                        void handleDelete(record.id);
                      }}
                      className="text-zinc-400 hover:text-zinc-900 disabled:opacity-40"
                    >
                      삭제
                    </button>
                  </div>
                </div>

                <div className="mt-4 grid grid-cols-3 gap-2">
                  <RecordValue
                    label="체중"
                    value={record.weightKg}
                    unit="kg"
                  />
                  <RecordValue
                    label="체지방"
                    value={record.bodyFatPercentage}
                    unit="%"
                  />
                  <RecordValue
                    label="골격근"
                    value={record.skeletalMuscleKg}
                    unit="kg"
                  />
                </div>
              </article>
            ))
          )}
        </div>
      </section>
    </main>
  );
}

function MetricCard({
  label,
  value,
  unit,
  change,
}: {
  label: string;
  value: string;
  unit: string;
  change?: number;
}) {
  return (
    <article className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-medium text-zinc-400">{label}</p>
      <p className="mt-2 text-xl font-bold tracking-tight text-zinc-950">
        {value}
        {value !== "-" && (
          <span className="ml-1 text-xs font-medium text-zinc-400">
            {unit}
          </span>
        )}
      </p>
      <p className="mt-1 text-[11px] text-zinc-400">
        {change === undefined
          ? "이전 기록 없음"
          : "직전 대비 " + changeText(change) + unit}
      </p>
    </article>
  );
}

function MeasurementInput({
  label,
  unit,
  value,
  onChange,
}: {
  label: string;
  unit: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-xs font-medium text-zinc-500">
      {label}
      <div className="relative mt-1.5">
        <input
          type="text"
          inputMode="decimal"
          pattern="[0-9]*[.]?[0-9]*"
          value={value}
          required
          onChange={(event) =>
            onChange(sanitizeDecimal(event.target.value, 2))
          }
          className="w-full rounded-xl border border-zinc-200 px-3 py-3 pr-10 text-sm outline-none focus:border-zinc-500"
        />
        <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs text-zinc-400">
          {unit}
        </span>
      </div>
    </label>
  );
}

function RecordValue({
  label,
  value,
  unit,
}: {
  label: string;
  value: number;
  unit: string;
}) {
  return (
    <div className="rounded-xl bg-zinc-50 px-3 py-3">
      <p className="text-[11px] text-zinc-400">{label}</p>
      <p className="mt-1 text-sm font-semibold">
        {value.toFixed(2)}
        <span className="ml-1 text-[11px] font-normal text-zinc-400">
          {unit}
        </span>
      </p>
    </div>
  );
}
