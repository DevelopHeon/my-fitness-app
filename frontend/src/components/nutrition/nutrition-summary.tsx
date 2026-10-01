import { DailyNutrition, NutritionTotals } from "@/lib/nutrition-api";
import { buttonClass, nutrients } from "./nutrition-fields";

type Props = {
  daily: DailyNutrition;
  busy: boolean;
  onEditGoal: () => void;
};

export default function NutritionSummary({ daily, busy, onEditGoal }: Props) {
  return (
    <section className="rounded-2xl border border-zinc-200 bg-white p-4">
      <h2 className="mb-3 font-bold">{daily.date} 섭취량</h2>
      <Totals totals={daily.consumed} />
      {daily.remaining && (
        <>
          <h3 className="mt-4 mb-2 text-sm font-semibold">목표까지 남은 양</h3>
          <Totals totals={daily.remaining} />
        </>
      )}
      <button className={buttonClass + " mt-4"} disabled={busy} onClick={onEditGoal}>
        {daily.goal ? "영양 목표 수정" : "영양 목표 설정 (선택)"}
      </button>
    </section>
  );
}

function Totals({ totals }: { totals: NutritionTotals }) {
  return (
    <dl className="grid grid-cols-2 gap-3 text-sm">
      {nutrients.map(({ key, label, unit }) => (
        <div key={key}>
          <dt className="text-zinc-500">{label}</dt>
          <dd className="mt-1 font-semibold">
            {totals[key] == null ? "미입력 항목 있음" : `${totals[key]} ${unit}`}
          </dd>
        </div>
      ))}
    </dl>
  );
}
