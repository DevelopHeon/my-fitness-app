"use client";

import { ChangeEvent, useCallback, useEffect, useRef, useState } from "react";
import { FoodPhotoItem, aiApi } from "@/lib/ai-api";
import { prepareFoodPhoto } from "@/lib/food-photo";
import { buttonClass } from "./nutrition-fields";

export default function MealPhotoInput({ initialPhoto, disabled, onAnalyzed, onBusyChange }: {
  initialPhoto?: File;
  disabled: boolean;
  onAnalyzed: (items: FoodPhotoItem[]) => void;
  onBusyChange: (busy: boolean) => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const pending = useRef(false);
  const mounted = useRef(true);
  const initialRequested = useRef(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const analysisDialog = useRef<HTMLDialogElement>(null);

  const analyze = useCallback(async (photo: File) => {
    if (pending.current || disabled) return;
    pending.current = true;
    setAnalyzing(true);
    setError(null);
    onBusyChange(true);
    try {
      const image = await prepareFoodPhoto(photo);
      if (!mounted.current) return;
      const conversations = await aiApi.listConversations();
      if (!mounted.current) return;
      const conversation = conversations[0] ?? await aiApi.createConversation();
      if (!mounted.current) return;
      const result = await aiApi.sendFoodPhoto(conversation.id, image);
      if (!mounted.current) return;
      const analysis = result.assistantMessage.foodPhotoResult;
      if (analysis?.status === "FOOD") {
        onAnalyzed(analysis.items);
      } else {
        setError(analysis?.status === "NOT_FOOD"
          ? "음식 사진이 아니에요. 음식 사진을 선택하거나 직접 입력해주세요."
          : "음식을 확실하게 구분하지 못했어요. 다른 사진을 선택하거나 직접 입력해주세요.");
      }
    } catch (caught) {
      if (mounted.current) setError(caught instanceof Error ? caught.message : "사진을 분석하지 못했어요.");
    } finally {
      pending.current = false;
      if (mounted.current) {
        setAnalyzing(false);
        onBusyChange(false);
      }
    }
  }, [disabled, onAnalyzed, onBusyChange]);

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  useEffect(() => {
    if (initialPhoto && !initialRequested.current) {
      initialRequested.current = true;
      void analyze(initialPhoto);
    }
  }, [initialPhoto, analyze]);

  useEffect(() => {
    const dialog = analysisDialog.current;
    if (!analyzing || !dialog) return;
    dialog.showModal();
    return () => dialog.close();
  }, [analyzing]);

  function selectFile(event: ChangeEvent<HTMLInputElement>) {
    const photo = event.target.files?.[0];
    event.target.value = "";
    if (photo) void analyze(photo);
  }

  return (
    <section className="space-y-2 rounded-xl bg-zinc-50 p-3">
      <button type="button" className={buttonClass + " flex w-full items-center justify-center gap-2 bg-white py-3"}
        disabled={disabled || analyzing} onClick={() => fileInput.current?.click()}>
        <span aria-hidden="true">📷</span> 사진 선택해서 음식 채우기
      </button>
      <input ref={fileInput} type="file" accept="image/jpeg,image/png" disabled={disabled || analyzing}
        aria-label="분석할 음식 사진" hidden onChange={selectFile} />
      <p className="text-xs text-zinc-500">선택하면 OpenAI로 전송해 분석합니다. JPG/PNG · 5 MiB 이하. 앱은 원본을 보관하지 않습니다.</p>
      {analyzing && <dialog ref={analysisDialog} aria-labelledby="food-analysis-title" aria-describedby="food-analysis-description"
        onCancel={(event) => event.preventDefault()}
        className="fixed inset-0 m-auto w-[calc(100%_-_2rem)] max-w-sm rounded-2xl bg-white p-6 text-center text-zinc-950 shadow-xl backdrop:bg-black/40">
        <div role="status" aria-live="polite" className="space-y-3">
          <span aria-hidden="true" className="mx-auto block h-10 w-10 animate-spin rounded-full border-4 border-zinc-200 border-t-zinc-950 motion-reduce:animate-none" />
          <h2 id="food-analysis-title" className="text-lg font-bold">AI가 분석 중이에요</h2>
          <p id="food-analysis-description" className="text-sm text-zinc-500">음식과 1인분 기준 칼로리를 확인하고 있어요. 잠시만 기다려주세요.</p>
        </div>
      </dialog>}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    </section>
  );
}
