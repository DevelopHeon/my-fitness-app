import { DailyNutrition, MealFood } from "@/lib/nutrition-api";
import NutritionSummary from "./nutrition-summary";
import { buttonClass, mealTypes } from "./nutrition-fields";

export default function NutritionDailyView({ daily, busy, onEditGoal, onEdit, onRemove }: {
  daily: DailyNutrition;
  busy: boolean;
  onEditGoal: () => void;
  onEdit: (item: MealFood) => void;
  onRemove: (item: MealFood) => void;
}) {
  return (
    <>
      <NutritionSummary daily={daily} busy={busy} onEditGoal={onEditGoal} />
      {daily.meals.map((meal) => (
        <section key={meal.mealType} className="rounded-2xl border border-zinc-200 bg-white p-4">
          <h2 className="font-bold">{mealTypes.find((type) => type.value === meal.mealType)?.label}</h2>
          <p className="mt-1 text-sm text-zinc-500">{meal.total.calories} kcal</p>
          {meal.items.length === 0 ? <p className="mt-3 text-sm text-zinc-400">등록한 식단이 없어요.</p> : meal.items.map((item) => (
            <div key={item.id} className="mt-3 flex items-center gap-2 border-t border-zinc-100 pt-3">
              <div className="min-w-0 flex-1">
                <p className="break-words font-medium">{item.foodName}</p>
                <p className="text-sm text-zinc-500">{item.calories} kcal</p>
              </div>
              <button className={buttonClass} disabled={busy} onClick={() => onEdit(item)}>수정</button>
              <button className={buttonClass + " text-red-600"} disabled={busy} onClick={() => onRemove(item)}>삭제</button>
            </div>
          ))}
        </section>
      ))}
    </>
  );
}
