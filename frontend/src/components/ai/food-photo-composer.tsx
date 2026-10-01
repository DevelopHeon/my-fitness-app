"use client";

import { ChangeEvent, useEffect, useState } from "react";

type Props = {
  selection: ReturnType<typeof useFoodPhotoSelection>;
  disabled: boolean;
  onSelect: () => void;
  onAnalyze: (photo: File) => Promise<boolean>;
};

export function useFoodPhotoSelection(enabled: boolean) {
  const [selection, setSelection] = useState<{ photo: File; preview: string } | null>(null);

  // 메뉴를 벗어나면 이전 파일을 다음 식단 화면에 다시 노출하지 않는다.
  if (!enabled && selection !== null) {
    setSelection(null);
  }

  const photo = enabled ? selection?.photo ?? null : null;
  const preview = enabled ? selection?.preview ?? null : null;

  useEffect(() => {
    return () => {
      if (preview) URL.revokeObjectURL(preview);
    };
  }, [preview]);

  function selectPhoto(selected: File | null) {
    if (!enabled || selected === null) {
      setSelection(null);
      return;
    }
    setSelection({ photo: selected, preview: URL.createObjectURL(selected) });
  }

  function clearPhoto() {
    setSelection(null);
  }

  return { photo, preview, selectPhoto, clearPhoto };
}

export default function FoodPhotoComposer({ selection, disabled, onSelect, onAnalyze }: Props) {
  const { photo, preview, selectPhoto, clearPhoto } = selection;

  function selectFile(event: ChangeEvent<HTMLInputElement>) {
    selectPhoto(event.target.files?.[0] ?? null);
    onSelect();
    event.target.value = "";
  }

  async function analyze() {
    if (!photo || disabled) return;
    if (await onAnalyze(photo)) clearPhoto();
  }

  return (
    <div className="mb-3 space-y-2">
      <label className="block text-xs font-semibold text-zinc-600">
        음식 사진 (JPG/PNG · 5 MiB 이하)
        <input
          type="file"
          accept="image/jpeg,image/png"
          disabled={disabled}
          className="mt-1 block w-full text-xs"
          onChange={selectFile}
        />
      </label>
      {photo && (
        <div className="flex items-center gap-2">
          {/* 사용자 선택 파일의 일시적인 object URL이다. */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          {preview && <img src={preview} alt="선택한 음식 사진" className="h-16 w-16 rounded-lg object-cover" />}
          <button
            type="button"
            disabled={disabled}
            onClick={() => void analyze()}
            className="rounded-xl bg-zinc-950 px-3 py-2 text-xs text-white disabled:opacity-50"
          >
            사진 분석
          </button>
          <button type="button" disabled={disabled} onClick={clearPhoto} className="px-2 py-2 text-xs">
            취소
          </button>
        </div>
      )}
      <p className="text-[11px] text-zinc-400">
        분석 시 사진을 OpenAI에 전송합니다. 앱은 원본 사진을 보관하지 않습니다.
      </p>
    </div>
  );
}
