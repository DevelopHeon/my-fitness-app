import { AiMessage, FoodPhotoItem } from "@/lib/ai-api";

export function MessageBubble({ message, onRecordFood, disabled }: {
  message: AiMessage;
  onRecordFood: (item: FoodPhotoItem) => void;
  disabled?: boolean;
}) {
  const user = message.role === "USER";
  return (
    <div
      className={
        "max-w-[86%] rounded-2xl px-4 py-3 text-sm leading-6 " +
        (user
          ? "ml-auto rounded-br-md bg-zinc-950 text-white"
          : "mr-auto rounded-bl-md bg-zinc-100 text-zinc-800")
      }
    >
      <p className="whitespace-pre-wrap break-words">{message.content}</p>
      {!user && message.foodPhotoResult?.status === "FOOD" && (
        message.foodPhotoResult.items.map((item, index) => (
          <div key={index} className="mt-3 rounded-xl border border-zinc-200 bg-white p-3">
            <p className="font-semibold">{item.foodName}</p>
            <p className="text-xs text-zinc-500">
              {item.servingDescription} · 약 {item.caloriesPerServing} kcal
            </p>
            <button
              type="button"
              disabled={disabled}
              onClick={() => onRecordFood(item)}
              className="mt-2 rounded-lg bg-zinc-950 px-3 py-2 text-xs text-white disabled:opacity-50"
            >
              식단에 기록하기
            </button>
          </div>
        ))
      )}
    </div>
  );
}
