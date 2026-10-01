"use client";

import { ChangeEvent, useCallback, useEffect, useRef, useState } from "react";
import { FoodPhotoItem, aiApi } from "@/lib/ai-api";
import { prepareFoodPhoto } from "@/lib/food-photo";

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

  function selectFile(event: ChangeEvent<HTMLInputElement>) {
    const photo = event.target.files?.[0];
    event.target.value = "";
    if (photo) void analyze(photo);
  }

  return (
    <section className="space-y-2 rounded-xl bg-zinc-50 p-3">
      <label className="block text-sm font-semibold">
        사진으로 음식 채우기
        <input type="file" accept="image/jpeg,image/png" disabled={disabled || analyzing}
          className="mt-2 block w-full text-xs" onChange={selectFile} />
      </label>
      <p className="text-xs text-zinc-500">선택하면 OpenAI로 전송해 분석합니다. JPG/PNG · 5 MiB 이하. 앱은 원본을 보관하지 않습니다.</p>
      {analyzing && <p role="status" className="text-sm">음식을 분석하고 있어요…</p>}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    </section>
  );
}
