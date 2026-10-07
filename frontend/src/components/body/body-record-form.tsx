"use client";

import { FormEvent, useState } from "react";
import { BodyRecord, BodyRecordInput } from "@/lib/body-api";
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

type Props = {
  record?: BodyRecord;
  busy: boolean;
  onSave: (input: BodyRecordInput) => Promise<void>;
  onCancel: () => void;
};

export default function BodyRecordForm({ record, busy, onSave, onCancel }: Props) {
  const [form, setForm] = useState<FormState>(() => {
    if (!record) return emptyForm();
    const measuredAt = new Date(record.measuredAt);
    return {
      weightKg: String(record.weightKg),
      bodyFatPercentage: String(record.bodyFatPercentage),
      skeletalMuscleKg: String(record.skeletalMuscleKg),
      measuredDate: localDateString(measuredAt),
      measuredTime: localTimeString(measuredAt),
      memo: record.memo ?? "",
    };
  });
  const [validationError, setValidationError] = useState<string | null>(null);

  function toInput(): BodyRecordInput | null {
    const weightKg = Number(form.weightKg);
    const bodyFatPercentage = Number(form.bodyFatPercentage);
    const skeletalMuscleKg = Number(form.skeletalMuscleKg);

    if (!form.measuredDate || !form.measuredTime) {
      setValidationError("측정 날짜와 시간을 입력해주세요.");
      return null;
    }

    if (!Number.isFinite(weightKg) || weightKg <= 0) {
      setValidationError("체중은 0보다 큰 숫자로 입력해주세요.");
      return null;
    }

    if (
      !Number.isFinite(bodyFatPercentage) ||
      bodyFatPercentage < 0 ||
      bodyFatPercentage > 100
    ) {
      setValidationError("체지방률은 0에서 100 사이 숫자로 입력해주세요.");
      return null;
    }

    if (
      !Number.isFinite(skeletalMuscleKg) ||
      skeletalMuscleKg <= 0
    ) {
      setValidationError("골격근량은 0보다 큰 숫자로 입력해주세요.");
      return null;
    }

    const measuredDate = new Date(
      form.measuredDate + "T" + form.measuredTime + ":00",
    );
    if (Number.isNaN(measuredDate.getTime())) {
      setValidationError("측정 일시를 확인해주세요.");
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
    if (busy) return;
    setValidationError(null);
    const input = toInput();
    if (input) await onSave(input);
  }

  return (
    <section className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="font-semibold">
            {record ? "신체 기록 수정" : "신체 기록 추가"}
          </h1>
          <p className="mt-1 text-xs text-zinc-400">
            같은 날 여러 번 기록할 수 있지만 동일한 측정 일시는 중복 저장할 수 없습니다.
          </p>
        </div>
        <button
          type="button"
          onClick={onCancel}
          disabled={busy}
          className="shrink-0 text-xs font-medium text-zinc-400 hover:text-zinc-900 disabled:opacity-40"
        >
          취소
        </button>
      </div>

      {validationError && (
        <p role="alert" className="mt-4 text-sm text-red-700">{validationError}</p>
      )}

      <form onSubmit={handleSubmit} className="mt-5 space-y-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <label className="block min-w-0 text-xs font-medium text-zinc-500">
            측정 날짜
            <input
              disabled={busy}
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
              className="mt-1.5 block min-w-0 max-w-full w-full appearance-none rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
            />
          </label>
          <label className="block min-w-0 text-xs font-medium text-zinc-500">
            측정 시간
            <input
              disabled={busy}
              type="time"
              required
              value={form.measuredTime}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  measuredTime: event.target.value,
                }))
              }
              className="mt-1.5 block min-w-0 max-w-full w-full appearance-none rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
            />
          </label>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <MeasurementInput
            label="체중"
            unit="kg"
            disabled={busy}
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
            disabled={busy}
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
            disabled={busy}
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
            disabled={busy}
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
          {record ? "기록 수정" : "기록 저장"}
        </button>
      </form>
    </section>
  );
}

function MeasurementInput({
  label,
  unit,
  value,
  onChange,
  disabled,
}: {
  label: string;
  unit: string;
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
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
          disabled={disabled}
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
