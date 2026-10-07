"use client";

import { useEffect, useState } from "react";
import { BodyRecord, BodyRecordInput, BodyTrend, bodyApi } from "@/lib/body-api";
import BodyRecordForm from "./body-record-form";

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
  const [formSource, setFormSource] = useState<{ record?: BodyRecord } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(true);
  const [loadState, setLoadState] = useState<"loading" | "loaded" | "error">("loading");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      try {
        const next = await bodyApi.getTrend(90);
        if (!cancelled) {
          setTrend(next);
          setLoadState("loaded");
        }
      } catch (caught) {
        if (!cancelled) {
          setLoadState("error");
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
    setLoadState("loading");
    const next = await run(() => bodyApi.getTrend(90));
    if (next) {
      setTrend(next);
      setLoadState("loaded");
    } else {
      setLoadState("error");
    }
  }

  function openForm(record?: BodyRecord) {
    if (busy) return;
    setError(null);
    setFormSource({ record });
    window.scrollTo({ top: 0, behavior: "auto" });
  }

  function closeForm() {
    setFormSource(null);
    setError(null);
    window.scrollTo({ top: 0, behavior: "auto" });
  }

  async function saveRecord(input: BodyRecordInput) {
    if (busy) return;
    const record = formSource?.record;
    const saved = await run(() => record
      ? bodyApi.updateRecord(record.id, input)
      : bodyApi.createRecord(input));
    if (!saved) return;
    closeForm();
    await refreshTrend();
  }

  async function handleDelete(recordId: number) {
    if (busy) return;
    const deleted = await run(async () => {
      await bodyApi.deleteRecord(recordId);
      return true;
    });
    if (!deleted) return;

    await refreshTrend();
  }

  const latest = trend.latest;
  const change = trend.changeFromPrevious;

  const pageClass = "mx-auto min-h-screen w-full max-w-2xl px-4 py-6 pb-24 sm:px-6";
  const errorNotice = error && (
    <div role="alert" className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {error}
    </div>
  );

  if (formSource) return (
    <main className={pageClass}>
      {errorNotice}
      <BodyRecordForm record={formSource.record} busy={busy} onSave={saveRecord} onCancel={closeForm} />
    </main>
  );

  return (
    <main className={pageClass}>
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-zinc-950">
              Body
            </h1>
            <p className="mt-2 text-sm leading-6 text-zinc-500">
              체중과 체성분을 측정 시각 기준으로 기록하고 변화를 확인하세요.
            </p>
          </div>
          <button
            type="button"
            disabled={busy}
            onClick={() => openForm()}
            className="rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
          >
            기록 추가
          </button>
        </div>
      </header>

      {errorNotice}

      <h2 className="mb-3 font-semibold">가장 최근 기록</h2>
      {loadState === "loading" && (
        <p role="status" className="mb-3 text-sm text-zinc-500">
          신체 기록을 불러오는 중...
        </p>
      )}
      <section aria-label="가장 최근 기록" className="grid grid-cols-3 gap-2">
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
        <div className="mt-2 flex flex-wrap items-center justify-end gap-3">
          <p className="text-xs text-zinc-400">최근 측정 {formatMeasuredAt(latest.measuredAt)}</p>
          <button type="button" disabled={busy} onClick={() => openForm(latest)}
            className="text-xs font-medium text-zinc-500 disabled:opacity-40">
            최신 기록 수정
          </button>
        </div>
      )}

      <section className="mt-6">
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="font-semibold">최근 90일 기록</h2>
            <p className="mt-1 text-xs text-zinc-400">
              최근 90일 측정 기록
            </p>
          </div>
          <span className="text-xs text-zinc-400">
            {trend.records.length}건
          </span>
        </div>

        <div className="space-y-3">
          {loadState === "loading" ? null : loadState === "error" && trend.records.length === 0 ? (
            <div className="rounded-2xl border border-zinc-200 bg-white px-4 py-6 text-center">
              <button
                type="button"
                onClick={() => { void refreshTrend(); }}
                className="rounded-xl border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700"
              >
                다시 불러오기
              </button>
            </div>
          ) : trend.records.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-zinc-200 bg-white px-4 py-8 text-center text-sm text-zinc-400">
              최근 90일 동안 등록한 신체 기록이 없습니다.
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
                      disabled={busy}
                      onClick={() => openForm(record)}
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
    <article className="min-w-0 rounded-2xl border border-zinc-200 bg-white p-3 shadow-sm sm:p-4">
      <p className="text-xs font-medium text-zinc-400">{label}</p>
      <p className="mt-2 text-lg font-bold tracking-tight text-zinc-950 sm:text-xl">
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
