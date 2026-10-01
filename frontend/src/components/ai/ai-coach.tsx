"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { AiConversation, AiMessage, FoodPhotoItem, aiApi } from "@/lib/ai-api";
import LoadingSpinner from "@/components/loading-spinner";

import { MessageBubble } from "./message-bubble";

type AppView = "dashboard" | "workout" | "routine" | "body" | "nutrition";

type Props = {
  currentView: AppView;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedDate?: string;
  onRecordFoods: (items: FoodPhotoItem[]) => void;
};

const prompts: Record<AppView, string[]> = {
  dashboard: [
    "최근 체중과 운동 퍼포먼스 변화를 같이 봐줘",
    "최근 운동량 분석해줘",
    "내 기록에서 개선할 점을 알려줘",
  ],
  workout: [
    "최근 운동량 분석해줘",
    "다음 중량 어떻게 잡을까?",
    "오늘 운동 강도는 어떻게 가져갈까?",
  ],
  routine: [
    "최근 운동 기록을 바탕으로 루틴을 평가해줘",
    "운동 부위 균형을 봐줘",
    "최근 운동 빈도가 적절한지 알려줘",
  ],
  body: [
    "최근 체중 변화를 알려줘",
    "체지방과 골격근 변화를 같이 봐줘",
    "신체 변화와 운동 기록을 같이 분석해줘",
  ],
  nutrition: [
    "오늘 식단 평가해줘",
    "남은 영양 목표에 맞는 식단 추천해줘",
    "단백질을 어떻게 채우면 좋을까?",
  ],
};

export default function AiCoach({ currentView, open, onOpenChange, selectedDate, onRecordFoods }: Props) {
  const [showList, setShowList] = useState(false);
  const [conversations, setConversations] = useState<AiConversation[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const endRef = useRef<HTMLDivElement | null>(null);
  const composingRef = useRef(false);
  const initializationPendingRef = useRef(false);

  const busy = loading || sending || initializing;

  const active = conversations.find((item) => item.id === activeId) ?? null;

  const loadMessages = useCallback(async (id: number) => {
    setMessages(await aiApi.listMessages(id));
  }, []);

  function recordFoods(items: FoodPhotoItem[]) {
    closeCoach();
    onRecordFoods(items);
  }

  function openCoach() {
    setError(null);
    setShowList(false);
    onOpenChange(true);
  }

  function closeCoach() {
    setShowList(false);
    onOpenChange(false);
  }

  useEffect(() => {
    if (!open || sending || initializationPendingRef.current) return;
    initializationPendingRef.current = true;

    async function initialize() {
      setInitializing(true);
      try {
        const list = await aiApi.listConversations();
        if (list.length > 0) {
          const selected = list.find((item) => item.id === activeId) ?? list[0];
          setConversations(list);
          setActiveId(selected.id);
          await loadMessages(selected.id);
        } else {
          const created = await aiApi.createConversation();
          setConversations([created]);
          setActiveId(created.id);
          setMessages([]);
        }
      } catch (nextError) {
        setError(errorMessage(nextError));
      } finally {
        initializationPendingRef.current = false;
        setInitializing(false);
      }
    }

    void initialize();
  }, [open, activeId, sending, loadMessages]);

  useEffect(() => {
    if (open) {
      endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
    }
  }, [messages, pendingMessage, sending, open]);

  async function createConversation() {
    setLoading(true);
    setError(null);
    try {
      const created = await aiApi.createConversation();
      setConversations((current) => [created, ...current]);
      setActiveId(created.id);
      setMessages([]);
      setInput("");
      setShowList(false);
    } catch (nextError) {
      setError(errorMessage(nextError));
    } finally {
      setLoading(false);
    }
  }

  async function selectConversation(id: number) {
    if (id === activeId) {
      setShowList(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await loadMessages(id);
      setActiveId(id);
      setInput("");
      setShowList(false);
    } catch (nextError) {
      setError(errorMessage(nextError));
    } finally {
      setLoading(false);
    }
  }

  async function renameConversation(conversation: AiConversation) {
    const title = window.prompt("대화 제목", conversation.title);
    if (!title?.trim() || title.trim() === conversation.title) return;

    setLoading(true);
    setError(null);
    try {
      const renamed = await aiApi.renameConversation(conversation.id, title.trim());
      setConversations((current) =>
        current.map((item) => (item.id === renamed.id ? renamed : item)),
      );
    } catch (nextError) {
      setError(errorMessage(nextError));
    } finally {
      setLoading(false);
    }
  }

  async function deleteConversation(id: number) {
    if (!window.confirm("이 AI 대화를 삭제할까요?")) return;

    setLoading(true);
    setError(null);
    try {
      await aiApi.deleteConversation(id);
      const remaining = conversations.filter((item) => item.id !== id);
      setConversations(remaining);

      if (activeId === id) {
        if (remaining.length > 0) {
          setActiveId(remaining[0].id);
          await loadMessages(remaining[0].id);
        } else {
          const created = await aiApi.createConversation();
          setConversations([created]);
          setActiveId(created.id);
          setMessages([]);
        }
      }
    } catch (nextError) {
      setError(errorMessage(nextError));
    } finally {
      setLoading(false);
    }
  }

  async function send(event?: FormEvent, preset?: string) {
    event?.preventDefault();
    if (preset === undefined && composingRef.current) return;

    const message = (preset ?? input).trim();
    if (!message || busy || activeId === null) return;

    setInput("");
    setPendingMessage(message);
    setSending(true);
    setError(null);

    try {
      const result = await aiApi.sendMessage(activeId, message, {
        screen: currentView.toUpperCase(),
        selectedDate,
      });
      setMessages((current) => [
        ...current,
        result.userMessage,
        result.assistantMessage,
      ]);
      setPendingMessage(null);
      setConversations((current) => [
        result.conversation,
        ...current.filter((item) => item.id !== result.conversation.id),
      ]);
    } catch (nextError) {
      setError(errorMessage(nextError));
      try {
        await loadMessages(activeId);
      } catch {
        // Provider 오류를 우선 표시한다.
      }
      setPendingMessage(null);
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      {!open ? (
        <button
          type="button"
          aria-label="AI Coach 열기"
          onClick={openCoach}
          className="fixed bottom-[calc(env(safe-area-inset-bottom)+1rem)] right-4 z-40 flex h-14 min-w-14 items-center justify-center rounded-full bg-zinc-950 px-4 text-sm font-bold text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-zinc-800 sm:right-6"
        >
          AI
        </button>
      ) : null}

      {open ? (
        <div className="fixed inset-0 z-50">
          <button
            type="button"
            aria-label="AI Coach 닫기"
            className="absolute inset-0 bg-black/25"
            onClick={closeCoach}
          />
          <section className="absolute inset-x-0 bottom-0 flex h-[88dvh] max-h-dvh min-h-0 flex-col overflow-hidden rounded-t-3xl border border-zinc-200 bg-white shadow-2xl sm:inset-y-0 sm:left-auto sm:right-0 sm:h-dvh sm:w-[460px] sm:rounded-none sm:rounded-l-3xl">
            <header className="flex items-center gap-2 border-b border-zinc-200 px-4 py-3">
              <button
                type="button"
                className="min-w-0 flex-1 text-left"
                onClick={() => setShowList((current) => !current)}
              >
                <p className="text-xs font-semibold text-zinc-400">AI Coach · 대화 목록</p>
                <p className="truncate text-sm font-bold text-zinc-950">
                  {active?.title ?? "AI Coach"}
                </p>
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={() => void createConversation()}
                className="rounded-xl border border-zinc-200 px-3 py-2 text-xs font-semibold text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
              >
                새 대화
              </button>
              <button
                type="button"
                aria-label="닫기"
                onClick={closeCoach}
                className="flex h-9 w-9 items-center justify-center rounded-full text-xl text-zinc-500 hover:bg-zinc-100"
              >
                ×
              </button>
            </header>

            {showList ? (
              <ConversationList
                conversations={conversations}
                activeId={activeId}
                loading={busy}
                onSelect={selectConversation}
                onRename={renameConversation}
                onDelete={deleteConversation}
              />
            ) : (
              <>
                <div className="min-h-0 flex-1 overflow-y-auto px-4 py-4">
                  {initializing ? (
                    <Centered text="AI 대화를 불러오는 중이에요." />
                  ) : messages.length === 0 && pendingMessage === null && !sending ? (
                    <div className="flex min-h-full flex-col justify-center gap-5 py-6">
                      <div>
                        <p className="text-lg font-bold text-zinc-950">
                          기록을 바탕으로 무엇이든 물어보세요
                        </p>
                        <p className="mt-1 text-sm leading-6 text-zinc-500">
                          운동, 신체 변화, 식단과 영양 관련 질문에 저장된 기록을
                          근거로 답합니다.
                        </p>
                      </div>
                      <QuickPrompts
                        prompts={prompts[currentView]}
                        disabled={busy || activeId === null}
                        onSelect={(prompt) => void send(undefined, prompt)}
                      />
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {messages.map((message) => (
                        <MessageBubble
                          key={message.id}
                          message={message}
                          disabled={busy}
                          onRecordFoods={recordFoods}
                        />
                      ))}
                      {pendingMessage ? (
                        <div className="ml-auto max-w-[86%] rounded-2xl rounded-br-md bg-zinc-950 px-4 py-3 text-sm leading-6 text-white">
                          <p className="whitespace-pre-wrap break-words">
                            {pendingMessage}
                          </p>
                        </div>
                      ) : null}
                      {sending ? (
                        <div
                          role="status"
                          aria-live="polite"
                          className="mr-auto flex max-w-[86%] items-center gap-2 rounded-2xl rounded-bl-md bg-zinc-100 px-4 py-3 text-sm text-zinc-600"
                        >
                          <LoadingSpinner className="h-5 w-5 text-zinc-600" />
                          AI 답변 생성 중...
                        </div>
                      ) : null}
                      <div ref={endRef} />
                    </div>
                  )}
                </div>

                <div className="border-t border-zinc-200 bg-white px-4 pb-[calc(env(safe-area-inset-bottom)+1rem)] pt-3">
                  {messages.length > 0 ? (
                    <div className="mb-3 flex gap-2 overflow-x-auto pb-1">
                      {prompts[currentView].slice(0, 2).map((prompt) => (
                        <button
                          key={prompt}
                          type="button"
                          disabled={busy}
                          onClick={() => void send(undefined, prompt)}
                          className="shrink-0 rounded-full border border-zinc-200 px-3 py-1.5 text-xs font-medium text-zinc-600 disabled:opacity-50"
                        >
                          {prompt}
                        </button>
                      ))}
                    </div>
                  ) : null}

                  {error ? (
                    <p className="mb-2 rounded-xl bg-red-50 px-3 py-2 text-xs leading-5 text-red-700">
                      {error}
                    </p>
                  ) : null}

                  <form
                    onSubmit={(event) => void send(event)}
                    className="flex items-end gap-2"
                  >
                    <textarea
                      value={input}
                      disabled={busy}
                      onChange={(event) => setInput(event.target.value.slice(0, 1000))}
                      onCompositionStart={() => {
                        composingRef.current = true;
                      }}
                      onCompositionEnd={() => {
                        composingRef.current = false;
                      }}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" && !event.shiftKey) {
                          if (
                            event.nativeEvent.isComposing ||
                            event.nativeEvent.keyCode === 229 ||
                            composingRef.current
                          ) {
                            return;
                          }
                          event.preventDefault();
                          void send();
                        }
                      }}
                      rows={1}
                      maxLength={1000}
                      placeholder="운동이나 식단에 대해 질문해보세요"
                      className="max-h-28 min-h-11 flex-1 resize-none rounded-2xl border border-zinc-300 bg-white px-4 py-3 text-sm outline-none focus:border-zinc-950"
                    />
                    <button
                      type="submit"
                      disabled={busy || !input.trim() || activeId === null}
                      className="h-11 rounded-2xl bg-zinc-950 px-4 text-sm font-bold text-white disabled:cursor-not-allowed disabled:bg-zinc-300"
                    >
                      전송
                    </button>
                  </form>
                  <p className="mt-2 text-center text-[11px] text-zinc-400">
                    AI 답변은 기록 기반 참고 정보이며 의료 진단을 제공하지 않습니다.
                  </p>
                </div>
              </>
            )}
          </section>
        </div>
      ) : null}
    </>
  );
}

function ConversationList({
  conversations,
  activeId,
  loading,
  onSelect,
  onRename,
  onDelete,
}: {
  conversations: AiConversation[];
  activeId: number | null;
  loading: boolean;
  onSelect: (id: number) => Promise<void>;
  onRename: (conversation: AiConversation) => Promise<void>;
  onDelete: (id: number) => Promise<void>;
}) {
  return (
    <div className="min-h-0 flex-1 overflow-y-auto px-4 py-4">
      <p className="mb-3 text-sm font-bold text-zinc-950">대화 목록</p>
      <div className="space-y-2">
        {conversations.map((conversation) => (
          <div
            key={conversation.id}
            className={
              "flex items-center gap-2 rounded-2xl border p-3 " +
              (conversation.id === activeId
                ? "border-zinc-950 bg-zinc-50"
                : "border-zinc-200")
            }
          >
            <button
              type="button"
              disabled={loading}
              onClick={() => void onSelect(conversation.id)}
              className="min-w-0 flex-1 text-left"
            >
              <p className="truncate text-sm font-semibold text-zinc-900">
                {conversation.title}
              </p>
              <p className="mt-1 text-[11px] text-zinc-400">
                {formatDate(conversation.updatedAt)}
              </p>
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={() => void onRename(conversation)}
              className="rounded-lg px-2 py-1 text-xs font-medium text-zinc-500 hover:bg-zinc-100"
            >
              이름
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={() => void onDelete(conversation.id)}
              className="rounded-lg px-2 py-1 text-xs font-medium text-red-500 hover:bg-red-50"
            >
              삭제
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

function QuickPrompts({
  prompts,
  disabled,
  onSelect,
}: {
  prompts: string[];
  disabled: boolean;
  onSelect: (prompt: string) => void;
}) {
  return (
    <div className="grid gap-2">
      {prompts.map((prompt) => (
        <button
          key={prompt}
          type="button"
          disabled={disabled}
          onClick={() => onSelect(prompt)}
          className="rounded-2xl border border-zinc-200 bg-white px-4 py-3 text-left text-sm font-medium text-zinc-700 shadow-sm transition hover:border-zinc-400 hover:bg-zinc-50 disabled:opacity-50"
        >
          {prompt}
        </button>
      ))}
    </div>
  );
}

function Centered({ text }: { text: string }) {
  return (
    <div className="flex min-h-full items-center justify-center text-sm text-zinc-500">
      {text}
    </div>
  );
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "AI 요청 처리 중 오류가 발생했습니다.";
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
