"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  DailyNutrition,
  Food,
  FoodInput,
  FoodSuggestions,
  MealFood,
  MealType,
  NutritionGoalInput,
  ServingUnit,
  nutritionApi,
} from "@/lib/nutrition-api";
import {
  sanitizeDecimal,
  sanitizeText,
  todayString,
} from "@/lib/input-utils";

type FoodFormState = {
  name: string;
  servingAmount: string;
  servingUnit: ServingUnit;
  calories: string;
  carbohydrateGrams: string;
  proteinGrams: string;
  fatGrams: string;
};

type GoalFormState = {
  calories: string;
  carbohydrateGrams: string;
  proteinGrams: string;
  fatGrams: string;
};

type NutritionView = "summary" | "meals" | "foods";

const mealTypes: { value: MealType; label: string }[] = [
  { value: "BREAKFAST", label: "아침" },
  { value: "LUNCH", label: "점심" },
  { value: "DINNER", label: "저녁" },
  { value: "SNACK", label: "간식" },
];

const servingUnits: { value: ServingUnit; label: string }[] = [
  { value: "G", label: "g" },
  { value: "ML", label: "ml" },
  { value: "COUNT", label: "개" },
];

function emptyFoodForm(): FoodFormState {
  return {
    name: "",
    servingAmount: "",
    servingUnit: "G",
    calories: "",
    carbohydrateGrams: "",
    proteinGrams: "",
    fatGrams: "",
  };
}

function emptyGoalForm(): GoalFormState {
  return {
    calories: "",
    carbohydrateGrams: "",
    proteinGrams: "",
    fatGrams: "",
  };
}

export default function NutritionScreen() {
  const [view, setView] = useState<NutritionView>("summary");
  const [date, setDate] = useState(todayString);
  const [daily, setDaily] = useState<DailyNutrition | null>(null);
  const [foods, setFoods] = useState<Food[]>([]);
  const [suggestions, setSuggestions] = useState<FoodSuggestions>({
    recent: [],
    frequent: [],
  });
  const [query, setQuery] = useState("");
  const [addingMealType, setAddingMealType] =
    useState<MealType | null>(null);
  const [selectedFoodId, setSelectedFoodId] =
    useState<number | null>(null);
  const [servings, setServings] = useState("1");
  const [editingItemId, setEditingItemId] =
    useState<number | null>(null);
  const [editingServings, setEditingServings] = useState("");
  const [foodForm, setFoodForm] =
    useState<FoodFormState>(emptyFoodForm);
  const [editingFoodId, setEditingFoodId] =
    useState<number | null>(null);
  const [goalOpen, setGoalOpen] = useState(false);
  const [goalForm, setGoalForm] =
    useState<GoalFormState>(emptyGoalForm);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setBusy(true);
      setError(null);
      try {
        const [nextDaily, nextFoods, nextSuggestions] =
          await Promise.all([
            nutritionApi.getDaily(date),
            nutritionApi.listFoods(),
            nutritionApi.getSuggestions(),
          ]);
        if (cancelled) return;

        setDaily(nextDaily);
        setFoods(nextFoods);
        setSuggestions(nextSuggestions);
        if (nextDaily.goal) {
          setGoalForm(goalToForm(nextDaily.goal));
        } else {
          setGoalForm(emptyGoalForm());
          setGoalOpen(true);
        }
      } catch (caught) {
        if (!cancelled) {
          setError(errorMessage(caught));
        }
      } finally {
        if (!cancelled) setBusy(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [date]);

  const filteredFoods = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase("ko-KR");
    if (!normalized) return foods;
    return foods.filter((food) =>
      food.name.toLocaleLowerCase("ko-KR").includes(normalized),
    );
  }, [foods, query]);

  const selectedFood =
    foods.find((food) => food.id === selectedFoodId) ?? null;

  async function run<T>(action: () => Promise<T>) {
    setBusy(true);
    setError(null);
    try {
      return await action();
    } catch (caught) {
      setError(errorMessage(caught));
      return null;
    } finally {
      setBusy(false);
    }
  }

  async function refreshDaily() {
    const next = await run(() => nutritionApi.getDaily(date));
    if (next) {
      setDaily(next);
      if (next.goal) setGoalForm(goalToForm(next.goal));
    }
  }

  async function refreshFoods() {
    const result = await run(async () => {
      const [nextFoods, nextSuggestions] = await Promise.all([
        nutritionApi.listFoods(),
        nutritionApi.getSuggestions(),
      ]);
      return { nextFoods, nextSuggestions };
    });
    if (!result) return;

    setFoods(result.nextFoods);
    setSuggestions(result.nextSuggestions);
  }

  function openFoodPicker(mealType: MealType) {
    setView("meals");
    setAddingMealType(mealType);
    setSelectedFoodId(null);
    setServings("1");
    setQuery("");
    requestAnimationFrame(() => {
      document
        .getElementById("nutrition-food-picker")
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
    });
  }

  async function handleAddMealFood() {
    if (!addingMealType || !selectedFood) return;

    const parsedServings = Number(servings);
    if (
      !Number.isFinite(parsedServings) ||
      parsedServings <= 0 ||
      parsedServings > 100
    ) {
      setError("섭취 회분은 0보다 크고 100 이하여야 합니다.");
      return;
    }

    const saved = await run(() =>
      nutritionApi.addMealItem(
        date,
        addingMealType,
        selectedFood.id,
        parsedServings,
      ),
    );
    if (!saved) return;

    setSelectedFoodId(null);
    setServings("1");
    setAddingMealType(null);
    await Promise.all([refreshDaily(), refreshFoods()]);
  }

  async function handleUpdateItem(item: MealFood) {
    const parsedServings = Number(editingServings);
    if (
      !Number.isFinite(parsedServings) ||
      parsedServings <= 0 ||
      parsedServings > 100
    ) {
      setError("섭취 회분은 0보다 크고 100 이하여야 합니다.");
      return;
    }

    const saved = await run(() =>
      nutritionApi.updateMealItem(item.id, parsedServings),
    );
    if (!saved) return;

    setEditingItemId(null);
    setEditingServings("");
    await refreshDaily();
  }

  async function handleDeleteItem(itemId: number) {
    const deleted = await run(async () => {
      await nutritionApi.deleteMealItem(itemId);
      return true;
    });
    if (!deleted) return;

    if (editingItemId === itemId) {
      setEditingItemId(null);
      setEditingServings("");
    }
    await Promise.all([refreshDaily(), refreshFoods()]);
  }

  function beginEditFood(food: Food) {
    setView("foods");
    setEditingFoodId(food.id);
    setFoodForm({
      name: food.name,
      servingAmount: String(food.servingAmount),
      servingUnit: food.servingUnit,
      calories: String(food.calories),
      carbohydrateGrams: String(food.carbohydrateGrams),
      proteinGrams: String(food.proteinGrams),
      fatGrams: String(food.fatGrams),
    });
    requestAnimationFrame(() => {
      document
        .getElementById("nutrition-food-manager")
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
    });
  }

  function resetFoodForm() {
    setEditingFoodId(null);
    setFoodForm(emptyFoodForm());
  }

  async function handleFoodSubmit(event: FormEvent) {
    event.preventDefault();
    const input = foodFormToInput(foodForm);
    if (!input) {
      setError("음식 제공량과 영양정보를 숫자로 입력해주세요.");
      return;
    }

    const saved = await run(() =>
      editingFoodId
        ? nutritionApi.updateFood(editingFoodId, input)
        : nutritionApi.createFood(input),
    );
    if (!saved) return;

    resetFoodForm();
    await refreshFoods();
  }

  async function handleDeleteFood(foodId: number) {
    const deleted = await run(async () => {
      await nutritionApi.deleteFood(foodId);
      return true;
    });
    if (!deleted) return;

    if (editingFoodId === foodId) resetFoodForm();
    if (selectedFoodId === foodId) setSelectedFoodId(null);
    await refreshFoods();
  }

  async function handleGoalSubmit(event: FormEvent) {
    event.preventDefault();
    const input = goalFormToInput(goalForm);
    if (!input) {
      setError("목표 칼로리와 탄단지를 숫자로 입력해주세요.");
      return;
    }

    const saved = await run(() => nutritionApi.upsertGoal(input));
    if (!saved) return;

    setGoalForm(goalToForm(saved));
    setGoalOpen(false);
    await refreshDaily();
  }

  if (!daily) {
    return (
      <main className="mx-auto min-h-screen w-full max-w-3xl px-4 py-8 sm:px-6">
        <div className="rounded-3xl border border-zinc-200 bg-white px-5 py-12 text-center text-sm text-zinc-400">
          Nutrition을 불러오고 있습니다.
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto min-h-screen w-full max-w-3xl px-4 py-6 sm:px-6">
      <header className="mb-6">
        <p className="text-xs font-semibold tracking-[0.18em] text-zinc-400">
          MY FITNESS
        </p>
        <div className="mt-2 flex items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              Nutrition
            </h1>
            <p className="mt-2 text-sm leading-6 text-zinc-500">
              자주 먹는 음식을 재사용하고 하루 칼로리와 탄단지를 기록하세요.
            </p>
          </div>
          <span className="rounded-full bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white">
            Phase 5
          </span>
        </div>
      </header>

      {error && (
        <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mb-5 grid grid-cols-3 gap-1.5 rounded-2xl bg-zinc-100 p-1.5">
        <NutritionMenuButton
          active={view === "summary"}
          label="요약 · 목표"
          onClick={() => setView("summary")}
        />
        <NutritionMenuButton
          active={view === "meals"}
          label="식단 기록"
          onClick={() => setView("meals")}
        />
        <NutritionMenuButton
          active={view === "foods"}
          label="음식 관리"
          onClick={() => setView("foods")}
        />
      </div>

      {view !== "foods" && (
        <div className="mb-4 flex items-center justify-between rounded-2xl border border-zinc-200 bg-white px-4 py-3">
          <div>
            <p className="text-xs font-medium text-zinc-400">
              기록 날짜
            </p>
            <p className="mt-1 text-sm font-semibold text-zinc-700">
              {date}
            </p>
          </div>
          <input
            type="date"
            max={todayString()}
            value={date}
            onChange={(event) => {
              setDate(event.target.value);
              setAddingMealType(null);
              setSelectedFoodId(null);
            }}
            className="rounded-xl border border-zinc-200 px-2.5 py-2 text-sm outline-none focus:border-zinc-500"
          />
        </div>
      )}

      {view === "summary" && (
      <section className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm">
        <div>
          <p className="text-xs font-medium text-zinc-400">
            일별 섭취
          </p>
          <h2 className="mt-1 text-lg font-semibold">
            오늘의 영양 요약
          </h2>
        </div>

        <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <NutritionMetric
            label="칼로리"
            consumed={daily.consumed.calories}
            goal={daily.goal?.calories ?? null}
            remaining={daily.remaining?.calories ?? null}
            unit="kcal"
          />
          <NutritionMetric
            label="탄수화물"
            consumed={daily.consumed.carbohydrateGrams}
            goal={daily.goal?.carbohydrateGrams ?? null}
            remaining={
              daily.remaining?.carbohydrateGrams ?? null
            }
            unit="g"
          />
          <NutritionMetric
            label="단백질"
            consumed={daily.consumed.proteinGrams}
            goal={daily.goal?.proteinGrams ?? null}
            remaining={daily.remaining?.proteinGrams ?? null}
            unit="g"
          />
          <NutritionMetric
            label="지방"
            consumed={daily.consumed.fatGrams}
            goal={daily.goal?.fatGrams ?? null}
            remaining={daily.remaining?.fatGrams ?? null}
            unit="g"
          />
        </div>

        <button
          type="button"
          onClick={() => setGoalOpen((current) => !current)}
          className="mt-4 flex w-full items-center justify-between rounded-xl bg-zinc-50 px-3 py-3 text-left text-xs font-semibold text-zinc-600"
        >
          <span>
            {daily.goal ? "영양 목표 수정" : "영양 목표 설정"}
          </span>
          <span className="text-zinc-400">
            {goalOpen ? "접기 ↑" : "열기 ↓"}
          </span>
        </button>

        {goalOpen && (
          <form
            onSubmit={handleGoalSubmit}
            className="mt-3 rounded-2xl border border-zinc-200 p-4"
          >
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <DecimalField
                label="칼로리"
                unit="kcal"
                value={goalForm.calories}
                onChange={(value) =>
                  setGoalForm((current) => ({
                    ...current,
                    calories: value,
                  }))
                }
              />
              <DecimalField
                label="탄수화물"
                unit="g"
                value={goalForm.carbohydrateGrams}
                onChange={(value) =>
                  setGoalForm((current) => ({
                    ...current,
                    carbohydrateGrams: value,
                  }))
                }
              />
              <DecimalField
                label="단백질"
                unit="g"
                value={goalForm.proteinGrams}
                onChange={(value) =>
                  setGoalForm((current) => ({
                    ...current,
                    proteinGrams: value,
                  }))
                }
              />
              <DecimalField
                label="지방"
                unit="g"
                value={goalForm.fatGrams}
                onChange={(value) =>
                  setGoalForm((current) => ({
                    ...current,
                    fatGrams: value,
                  }))
                }
              />
            </div>
            <button
              type="submit"
              disabled={busy}
              className="mt-3 w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
            >
              목표 저장
            </button>
          </form>
        )}
      </section>
      )}

      {view === "meals" && (
      <>
      <section className="space-y-3">
        {daily.meals.map((meal) => (
          <article
            key={meal.mealType}
            className="rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-medium text-zinc-400">
                  {mealTypeLabel(meal.mealType)}
                </p>
                <h2 className="mt-1 text-lg font-semibold">
                  {formatNumber(meal.total.calories)} kcal
                </h2>
                <p className="mt-1 text-xs text-zinc-400">
                  탄 {formatNumber(meal.total.carbohydrateGrams)} ·
                  단 {formatNumber(meal.total.proteinGrams)} ·
                  지 {formatNumber(meal.total.fatGrams)}g
                </p>
              </div>
              <button
                type="button"
                disabled={busy}
                onClick={() => openFoodPicker(meal.mealType)}
                className="rounded-xl border border-zinc-200 px-3 py-2 text-xs font-semibold text-zinc-600 hover:bg-zinc-50 disabled:opacity-40"
              >
                + 음식 추가
              </button>
            </div>

            <div className="mt-4 space-y-2">
              {meal.items.length === 0 ? (
                <p className="rounded-xl border border-dashed border-zinc-200 px-3 py-5 text-center text-xs text-zinc-400">
                  아직 기록된 음식이 없습니다.
                </p>
              ) : (
                meal.items.map((item) => (
                  <MealItemRow
                    key={item.id}
                    item={item}
                    editing={editingItemId === item.id}
                    editingServings={editingServings}
                    busy={busy}
                    onBeginEdit={() => {
                      setEditingItemId(item.id);
                      setEditingServings(String(item.servings));
                    }}
                    onEditingServingsChange={setEditingServings}
                    onSave={() => {
                      void handleUpdateItem(item);
                    }}
                    onCancel={() => {
                      setEditingItemId(null);
                      setEditingServings("");
                    }}
                    onDelete={() => {
                      void handleDeleteItem(item.id);
                    }}
                  />
                ))
              )}
            </div>
          </article>
        ))}
      </section>

      {addingMealType && (
        <section
          id="nutrition-food-picker"
          className="mt-6 scroll-mt-20 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-medium text-zinc-400">
                {mealTypeLabel(addingMealType)}
              </p>
              <h2 className="mt-1 text-lg font-semibold">
                음식 선택
              </h2>
            </div>
            <button
              type="button"
              onClick={() => {
                setAddingMealType(null);
                setSelectedFoodId(null);
              }}
              className="text-xs font-medium text-zinc-400"
            >
              닫기
            </button>
          </div>

          <input
            type="text"
            inputMode="text"
            value={query}
            onChange={(event) =>
              setQuery(sanitizeText(event.target.value))
            }
            placeholder="음식 이름 검색"
            className="mt-4 w-full rounded-xl border border-zinc-200 px-3 py-3 text-sm outline-none focus:border-zinc-500"
          />

          <SuggestionGroup
            title="최근 먹은 음식"
            foods={suggestions.recent}
            selectedFoodId={selectedFoodId}
            onSelect={setSelectedFoodId}
          />
          <SuggestionGroup
            title="자주 먹는 음식"
            foods={suggestions.frequent}
            selectedFoodId={selectedFoodId}
            onSelect={setSelectedFoodId}
          />

          <div className="mt-4">
            <div className="mb-2 flex items-center justify-between">
              <p className="text-xs font-semibold text-zinc-500">
                내 음식
              </p>
              <span className="text-[11px] text-zinc-400">
                {filteredFoods.length}개
              </span>
            </div>
            <div className="max-h-64 space-y-2 overflow-y-auto">
              {filteredFoods.length === 0 ? (
                <p className="rounded-xl border border-dashed border-zinc-200 px-3 py-5 text-center text-xs text-zinc-400">
                  검색 결과가 없습니다. 아래에서 새 음식을 등록해주세요.
                </p>
              ) : (
                filteredFoods.map((food) => (
                  <FoodSelectButton
                    key={food.id}
                    food={food}
                    active={selectedFoodId === food.id}
                    onClick={() => setSelectedFoodId(food.id)}
                  />
                ))
              )}
            </div>
          </div>

          {selectedFood && (
            <div className="mt-4 rounded-2xl bg-zinc-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold">
                    {selectedFood.name}
                  </p>
                  <p className="mt-1 text-xs text-zinc-400">
                    1회 {formatServing(selectedFood)} ·{" "}
                    {formatNumber(selectedFood.calories)} kcal
                  </p>
                </div>
                <label className="w-24 text-xs font-medium text-zinc-500">
                  회분
                  <input
                    type="text"
                    inputMode="decimal"
                    pattern="[0-9]*[.]?[0-9]*"
                    value={servings}
                    onChange={(event) =>
                      setServings(
                        sanitizeDecimal(event.target.value, 2),
                      )
                    }
                    className="mt-1 w-full rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-zinc-500"
                  />
                </label>
              </div>
              <button
                type="button"
                disabled={busy}
                onClick={() => {
                  void handleAddMealFood();
                }}
                className="mt-3 w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
              >
                {mealTypeLabel(addingMealType)}에 추가
              </button>
            </div>
          )}
        </section>
      )}
      </>
      )}

      {view === "foods" && (
      <section
        id="nutrition-food-manager"
        className="mt-6 scroll-mt-20 rounded-3xl border border-zinc-200 bg-white p-5 shadow-sm"
      >
        <div>
          <p className="text-xs font-medium text-zinc-400">
            음식 카탈로그
          </p>
          <h2 className="mt-1 text-lg font-semibold">
            음식 등록 · 수정 · 삭제
          </h2>
          <p className="mt-1 text-xs leading-5 text-zinc-400">
            자주 먹는 음식의 1회 제공량과 영양정보를 관리합니다.
          </p>
        </div>
            <form
              onSubmit={handleFoodSubmit}
              className="mt-4 rounded-2xl bg-zinc-50 p-4"
            >
              <div className="flex items-center justify-between">
                <p className="text-xs font-semibold text-zinc-600">
                  {editingFoodId ? "음식 수정" : "새 음식 등록"}
                </p>
                {editingFoodId && (
                  <button
                    type="button"
                    onClick={resetFoodForm}
                    className="text-xs text-zinc-400"
                  >
                    수정 취소
                  </button>
                )}
              </div>

              <label className="mt-3 block text-xs font-medium text-zinc-500">
                음식 이름
                <input
                  type="text"
                  value={foodForm.name}
                  required
                  maxLength={100}
                  onChange={(event) =>
                    setFoodForm((current) => ({
                      ...current,
                      name: sanitizeText(event.target.value),
                    }))
                  }
                  className="mt-1.5 w-full rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm outline-none focus:border-zinc-500"
                />
              </label>

              <div className="mt-3 grid grid-cols-[1fr_100px] gap-2">
                <DecimalField
                  label="1회 제공량"
                  unit=""
                  value={foodForm.servingAmount}
                  onChange={(value) =>
                    setFoodForm((current) => ({
                      ...current,
                      servingAmount: value,
                    }))
                  }
                />
                <label className="text-xs font-medium text-zinc-500">
                  단위
                  <select
                    value={foodForm.servingUnit}
                    onChange={(event) =>
                      setFoodForm((current) => ({
                        ...current,
                        servingUnit:
                          event.target.value as ServingUnit,
                      }))
                    }
                    className="mt-1.5 w-full rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm outline-none"
                  >
                    {servingUnits.map((unit) => (
                      <option
                        key={unit.value}
                        value={unit.value}
                      >
                        {unit.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
                <DecimalField
                  label="칼로리"
                  unit="kcal"
                  value={foodForm.calories}
                  onChange={(value) =>
                    setFoodForm((current) => ({
                      ...current,
                      calories: value,
                    }))
                  }
                />
                <DecimalField
                  label="탄수화물"
                  unit="g"
                  value={foodForm.carbohydrateGrams}
                  onChange={(value) =>
                    setFoodForm((current) => ({
                      ...current,
                      carbohydrateGrams: value,
                    }))
                  }
                />
                <DecimalField
                  label="단백질"
                  unit="g"
                  value={foodForm.proteinGrams}
                  onChange={(value) =>
                    setFoodForm((current) => ({
                      ...current,
                      proteinGrams: value,
                    }))
                  }
                />
                <DecimalField
                  label="지방"
                  unit="g"
                  value={foodForm.fatGrams}
                  onChange={(value) =>
                    setFoodForm((current) => ({
                      ...current,
                      fatGrams: value,
                    }))
                  }
                />
              </div>

              <button
                type="submit"
                disabled={busy}
                className="mt-3 w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white disabled:opacity-40"
              >
                {editingFoodId ? "음식 수정" : "음식 등록"}
              </button>
            </form>

            <div className="mt-4 space-y-2">
              {foods.length === 0 ? (
                <p className="rounded-xl border border-dashed border-zinc-200 px-3 py-5 text-center text-xs text-zinc-400">
                  등록한 음식이 없습니다.
                </p>
              ) : (
                foods.map((food) => (
                  <div
                    key={food.id}
                    className="flex items-center justify-between gap-3 rounded-xl border border-zinc-100 px-3 py-3"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">
                        {food.name}
                      </p>
                      <p className="mt-1 text-xs text-zinc-400">
                        {formatServing(food)} ·{" "}
                        {formatNumber(food.calories)} kcal ·
                        탄 {formatNumber(food.carbohydrateGrams)} ·
                        단 {formatNumber(food.proteinGrams)} ·
                        지 {formatNumber(food.fatGrams)}g
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-3 text-xs font-medium">
                      <button
                        type="button"
                        onClick={() => beginEditFood(food)}
                        className="text-zinc-500"
                      >
                        수정
                      </button>
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => {
                          void handleDeleteFood(food.id);
                        }}
                        className="text-zinc-400 disabled:opacity-40"
                      >
                        삭제
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
      </section>
      )}
    </main>
  );
}

function NutritionMenuButton({
  active,
  label,
  onClick,
}: {
  active: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "rounded-xl px-2 py-2.5 text-xs font-semibold transition " +
        (active
          ? "bg-white text-zinc-950 shadow-sm"
          : "text-zinc-500 hover:text-zinc-900")
      }
    >
      {label}
    </button>
  );
}

function MealItemRow({
  item,
  editing,
  editingServings,
  busy,
  onBeginEdit,
  onEditingServingsChange,
  onSave,
  onCancel,
  onDelete,
}: {
  item: MealFood;
  editing: boolean;
  editingServings: string;
  busy: boolean;
  onBeginEdit: () => void;
  onEditingServingsChange: (value: string) => void;
  onSave: () => void;
  onCancel: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="rounded-xl bg-zinc-50 px-3 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold">
            {item.foodName}
          </p>
          <p className="mt-1 text-xs text-zinc-400">
            {formatNumber(item.servingAmount)}
            {servingUnitLabel(item.servingUnit)} ×{" "}
            {formatNumber(item.servings)}회 ·{" "}
            {formatNumber(item.total.calories)} kcal
          </p>
          <p className="mt-1 text-[11px] text-zinc-400">
            탄 {formatNumber(item.total.carbohydrateGrams)} ·
            단 {formatNumber(item.total.proteinGrams)} ·
            지 {formatNumber(item.total.fatGrams)}g
          </p>
        </div>
        <div className="flex shrink-0 gap-3 text-xs font-medium">
          <button
            type="button"
            onClick={onBeginEdit}
            className="text-zinc-500"
          >
            회분 수정
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onDelete}
            className="text-zinc-400 disabled:opacity-40"
          >
            삭제
          </button>
        </div>
      </div>

      {editing && (
        <div className="mt-3 flex items-end gap-2">
          <label className="flex-1 text-xs font-medium text-zinc-500">
            섭취 회분
            <input
              type="text"
              inputMode="decimal"
              pattern="[0-9]*[.]?[0-9]*"
              value={editingServings}
              onChange={(event) =>
                onEditingServingsChange(
                  sanitizeDecimal(event.target.value, 2),
                )
              }
              className="mt-1 w-full rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-sm outline-none"
            />
          </label>
          <button
            type="button"
            disabled={busy}
            onClick={onSave}
            className="rounded-xl bg-zinc-900 px-3 py-2.5 text-xs font-semibold text-white disabled:opacity-40"
          >
            저장
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="rounded-xl border border-zinc-200 bg-white px-3 py-2.5 text-xs font-semibold text-zinc-500"
          >
            취소
          </button>
        </div>
      )}
    </div>
  );
}

function SuggestionGroup({
  title,
  foods,
  selectedFoodId,
  onSelect,
}: {
  title: string;
  foods: Food[];
  selectedFoodId: number | null;
  onSelect: (foodId: number) => void;
}) {
  if (foods.length === 0) return null;

  return (
    <div className="mt-4">
      <p className="mb-2 text-xs font-semibold text-zinc-500">
        {title}
      </p>
      <div className="flex gap-2 overflow-x-auto pb-1">
        {foods.map((food) => (
          <button
            key={food.id}
            type="button"
            onClick={() => onSelect(food.id)}
            className={
              "shrink-0 rounded-full px-3 py-2 text-xs font-semibold " +
              (selectedFoodId === food.id
                ? "bg-zinc-950 text-white"
                : "bg-zinc-100 text-zinc-600")
            }
          >
            {food.name}
          </button>
        ))}
      </div>
    </div>
  );
}

function FoodSelectButton({
  food,
  active,
  onClick,
}: {
  food: Food;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "flex w-full items-center justify-between gap-3 rounded-xl border px-3 py-3 text-left " +
        (active
          ? "border-zinc-900 bg-zinc-50"
          : "border-zinc-200")
      }
    >
      <div className="min-w-0">
        <p className="truncate text-sm font-semibold">
          {food.name}
        </p>
        <p className="mt-1 text-xs text-zinc-400">
          {formatServing(food)}
        </p>
      </div>
      <div className="shrink-0 text-right">
        <p className="text-sm font-semibold">
          {formatNumber(food.calories)} kcal
        </p>
        <p className="text-[11px] text-zinc-400">
          단 {formatNumber(food.proteinGrams)}g
        </p>
      </div>
    </button>
  );
}

function NutritionMetric({
  label,
  consumed,
  goal,
  remaining,
  unit,
}: {
  label: string;
  consumed: number;
  goal: number | null;
  remaining: number | null;
  unit: string;
}) {
  const progress =
    goal && goal > 0
      ? Math.min(100, Math.max(0, (consumed / goal) * 100))
      : 0;

  return (
    <article className="rounded-2xl bg-zinc-50 p-4">
      <p className="text-xs font-medium text-zinc-400">
        {label}
      </p>
      <p className="mt-2 text-xl font-bold tracking-tight">
        {formatNumber(consumed)}
        <span className="ml-1 text-[11px] font-medium text-zinc-400">
          {unit}
        </span>
      </p>
      <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-zinc-200">
        <div
          className="h-full rounded-full bg-zinc-900"
          style={{ width: progress + "%" }}
        />
      </div>
      <p className="mt-2 text-[11px] leading-4 text-zinc-400">
        {goal === null
          ? "목표 미설정"
          : "목표 " +
            formatNumber(goal) +
            unit +
            " · " +
            remainingText(remaining, unit)}
      </p>
    </article>
  );
}

function DecimalField({
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
          className="w-full rounded-xl border border-zinc-200 bg-white px-3 py-3 pr-12 text-sm outline-none focus:border-zinc-500"
        />
        {unit && (
          <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[11px] text-zinc-400">
            {unit}
          </span>
        )}
      </div>
    </label>
  );
}

function foodFormToInput(
  form: FoodFormState,
): FoodInput | null {
  const servingAmount = Number(form.servingAmount);
  const calories = Number(form.calories);
  const carbohydrateGrams = Number(form.carbohydrateGrams);
  const proteinGrams = Number(form.proteinGrams);
  const fatGrams = Number(form.fatGrams);

  if (
    !form.name.trim() ||
    !Number.isFinite(servingAmount) ||
    servingAmount <= 0 ||
    !Number.isFinite(calories) ||
    calories < 0 ||
    !Number.isFinite(carbohydrateGrams) ||
    carbohydrateGrams < 0 ||
    !Number.isFinite(proteinGrams) ||
    proteinGrams < 0 ||
    !Number.isFinite(fatGrams) ||
    fatGrams < 0
  ) {
    return null;
  }

  return {
    name: form.name.trim(),
    servingAmount,
    servingUnit: form.servingUnit,
    calories,
    carbohydrateGrams,
    proteinGrams,
    fatGrams,
  };
}

function goalFormToInput(
  form: GoalFormState,
): NutritionGoalInput | null {
  const calories = Number(form.calories);
  const carbohydrateGrams = Number(form.carbohydrateGrams);
  const proteinGrams = Number(form.proteinGrams);
  const fatGrams = Number(form.fatGrams);

  if (
    !Number.isFinite(calories) ||
    calories <= 0 ||
    !Number.isFinite(carbohydrateGrams) ||
    carbohydrateGrams < 0 ||
    !Number.isFinite(proteinGrams) ||
    proteinGrams < 0 ||
    !Number.isFinite(fatGrams) ||
    fatGrams < 0
  ) {
    return null;
  }

  return {
    calories,
    carbohydrateGrams,
    proteinGrams,
    fatGrams,
  };
}

function goalToForm(goal: {
  calories: number;
  carbohydrateGrams: number;
  proteinGrams: number;
  fatGrams: number;
}): GoalFormState {
  return {
    calories: String(goal.calories),
    carbohydrateGrams: String(goal.carbohydrateGrams),
    proteinGrams: String(goal.proteinGrams),
    fatGrams: String(goal.fatGrams),
  };
}

function mealTypeLabel(mealType: MealType) {
  return (
    mealTypes.find((item) => item.value === mealType)?.label ??
    mealType
  );
}

function servingUnitLabel(unit: ServingUnit) {
  return (
    servingUnits.find((item) => item.value === unit)?.label ??
    unit
  );
}

function formatServing(food: Food) {
  return (
    formatNumber(food.servingAmount) +
    servingUnitLabel(food.servingUnit)
  );
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("ko-KR", {
    maximumFractionDigits: 2,
  }).format(value);
}

function remainingText(
  remaining: number | null,
  unit: string,
) {
  if (remaining === null) return "목표 미설정";
  if (remaining >= 0) {
    return "남음 " + formatNumber(remaining) + unit;
  }
  return "초과 " + formatNumber(Math.abs(remaining)) + unit;
}

function errorMessage(caught: unknown) {
  return caught instanceof Error
    ? caught.message
    : "요청 처리 중 오류가 발생했습니다.";
}
