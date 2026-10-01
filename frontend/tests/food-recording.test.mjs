import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import ts from "typescript";

const require = createRequire(import.meta.url);
function load(file, globals = {}, imports = {}) {
  const filename = path.resolve(fs.existsSync(file) ? file : file.replace(/\.ts$/, ".tsx"));
  const output = ts.transpileModule(fs.readFileSync(filename, "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX },
  }).outputText;
  const loadedModule = { exports: {} };
  // Share Error across test modules as in the browser's single JavaScript realm.
  vm.runInNewContext(output, { module: loadedModule, exports: loadedModule.exports, process, console, Headers, FormData, Error,
    require: (name) => {
      if (Object.hasOwn(imports, name)) return imports[name];
      if (name.startsWith("@/")) return load("src/" + name.slice(2) + ".ts", globals, imports);
      if (name.startsWith(".")) return load(path.resolve(path.dirname(filename), name) + ".ts", globals, imports);
      return require(name);
    },
    ...globals }, { filename });
  return loadedModule.exports;
}

test("FormData keeps browser boundary, auth cookie and CSRF header", async () => {
  const requests = [];
  const api = load("src/lib/api-client.ts", {
    document: { cookie: "XSRF-TOKEN=test-token" },
    fetch: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => url.endsWith("/csrf")
        ? { headerName: "X-XSRF-TOKEN", cookieName: "XSRF-TOKEN" } : {} };
    },
  });
  await api.request("/photo", { method: "POST", body: new FormData() });
  const sent = requests.at(-1).init;
  assert.equal(sent.headers.has("Content-Type"), false);
  assert.equal(sent.headers.get("X-XSRF-TOKEN"), "test-token");
  assert.equal(sent.credentials, "include");
});


test("meal defaults use Korea time at all boundaries and preserve unknown vs zero", () => {
  const { mealDefaults, optionalNutrient } = load("src/lib/meal-input.ts");
  for (const [time, expected] of [["04:59", "SNACK"], ["05:00", "BREAKFAST"], ["09:59", "BREAKFAST"],
    ["10:00", "LUNCH"], ["14:59", "LUNCH"], ["15:00", "DINNER"], ["20:59", "DINNER"], ["21:00", "SNACK"]]) {
    const result = mealDefaults(new Date(`2026-10-01T${time}:00+09:00`));
    assert.equal(result.mealType, expected);
    assert.equal(result.mealDate, "2026-10-01");
  }
  assert.equal(mealDefaults(new Date("2026-09-30T15:00:00Z")).mealDate, "2026-10-01");
  assert.equal(optionalNutrient(""), null);
  assert.equal(optionalNutrient("0"), 0);
});

test("only assistant food results offer record actions without calling save on render", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const { MessageBubble } = load("src/components/ai/message-bubble.tsx");
  let calls = 0;
  for (const status of ["FOOD", "NOT_FOOD", "UNCERTAIN"]) {
    const html = renderToStaticMarkup(React.createElement(MessageBubble, {
      message: { role: "ASSISTANT", content: "안내", foodPhotoResult: { status,
        items: status === "FOOD" ? [{ foodName: "밥", servingDescription: "1그릇", caloriesPerServing: 300 }] : [] } },
      onRecordFoods: () => calls++,
    }));
    assert.equal(html.includes("입력하기"), status === "FOOD");
  }
  assert.equal(calls, 0);
});

test("photo draft opens an editable form with optional macros initially closed", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Screen = load("src/components/nutrition/nutrition-screen.tsx").default;
  const html = renderToStaticMarkup(React.createElement(Screen, { initialDraft: {
    items: [{ foodName: "밥", caloriesPerServing: 300, servingDescription: "1그릇" }], mealDate: "2026-10-01", mealType: "LUNCH",
  } }));
  assert.match(html, /value="밥"/);
  assert.match(html, /value="300"/);
  assert.match(html, /영양정보 추가 입력/);
  assert.doesNotMatch(html, /<details[^>]*\bopen/);
  assert.doesNotMatch(html, /음식 관리|섭취 회분/);
});

test("photo normalization rejects unsupported/large inputs before decoding and closes decoded bitmap", async () => {
  let decodes = 0;
  let closed = 0;
  const { prepareFoodPhoto } = load("src/lib/food-photo.ts", {
    createImageBitmap: async () => { decodes++; return { width: 5000, height: 5000, close: () => closed++ }; },
  });
  await assert.rejects(prepareFoodPhoto({ type: "image/webp", size: 100 }));
  await assert.rejects(prepareFoodPhoto({ type: "image/jpeg", size: 5 * 1024 * 1024 + 1 }));
  assert.equal(decodes, 0);
  await assert.rejects(prepareFoodPhoto({ type: "image/jpeg", size: 100 }));
  assert.equal(closed, 1);
});

test("text coach stays available without a second photo upload flow", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Coach = load("src/components/ai/ai-coach.tsx").default;
  for (const currentView of ["dashboard", "workout", "routine", "body", "nutrition"]) {
    const html = renderToStaticMarkup(React.createElement(Coach, {
      currentView,
      open: true,
      onOpenChange: () => {},
      onRecordFoods: () => {},
    }));
    assert.equal(html.includes('type="file"'), false, currentView);
    assert.match(html, /<textarea/);
  }
});

test("nutrition screen offers a photo recording entry without saving a meal on render", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Screen = load("src/components/nutrition/nutrition-screen.tsx").default;
  let calls = 0;
  const html = renderToStaticMarkup(React.createElement(Screen, {
    onAnalyzePhoto: () => calls++,
  }));
  assert.match(html, /사진으로 식단 입력/);
  assert.match(html, /식단 추가/);
  assert.equal(calls, 0);
});


test("reopening the coach during its first lookup does not create duplicate conversations", async () => {
  let resolveList;
  const pendingList = new Promise((resolve) => { resolveList = resolve; });
  let created = 0;
  const refs = [];
  let refIndex = 0;
  let effects = [];
  // Drive only the pre-response renders: state has not changed yet, and refs survive reopening.
  const hooks = {
    ...require("react"),
    useState: (initial) => [typeof initial === "function" ? initial() : initial, () => {}],
    useRef: (initial) => {
      const index = refIndex++;
      refs[index] ??= { current: initial };
      return refs[index];
    },
    useEffect: (effect) => { effects.push(effect); },
    useCallback: (callback) => callback,
  };
  const Coach = load("src/components/ai/ai-coach.tsx", {}, {
    react: hooks,
    "@/lib/ai-api": { aiApi: {
      listConversations: () => pendingList,
      createConversation: async () => ({ id: ++created, title: "새 대화" }),
      listMessages: async () => [],
    } },
  }).default;

  function render(open) {
    refIndex = 0;
    effects = [];
    Coach({ currentView: "nutrition", open, onOpenChange: () => {}, onRecordFoods: () => {} });
    for (const effect of effects) effect();
  }

  render(true);
  render(false);
  render(true);
  resolveList([]);
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(created, 1);
});

test("a photo with multiple foods produces one editable batch and one save action", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Form = load("src/components/nutrition/meal-record-form.tsx").default;
  const html = renderToStaticMarkup(React.createElement(Form, {
    source: { draft: { mealDate: "2026-10-01", mealType: "LUNCH", items: [
      { foodName: "밥", caloriesPerServing: 300, servingDescription: "1그릇" },
      { foodName: "닭", caloriesPerServing: 200, servingDescription: "1인분" },
    ] } }, busy: false, onSave: () => {}, onCancel: () => {},
  }));
  assert.match(html, /value="밥"/);
  assert.match(html, /value="닭"/);
  assert.match(html, /500/);
  assert.equal((html.match(/type="submit"/g) ?? []).length, 1);
  assert.equal((html.match(/type="date"/g) ?? []).length, 1);
  assert.doesNotMatch(html, /<details[^>]*\bopen/);
});

test("nutrition browsing offers daily and calendar views without embedding the editor", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Screen = load("src/components/nutrition/nutrition-screen.tsx").default;
  const html = renderToStaticMarkup(React.createElement(Screen, {}));
  assert.match(html, /일별 조회/);
  assert.match(html, /캘린더 조회/);
  assert.doesNotMatch(html, /<form/);
});

// Exercise handlers on the real form with persistent state, without mocking its food rows.
function componentDriver(file, props, imports = {}) {
  const states = [];
  const refs = [];
  let stateIndex = 0;
  let refIndex = 0;
  let effects = [];
  let effectIndex = 0;
  let callbackIndex = 0;
  const callbacks = [];
  const effectStates = [];
  const hooks = {
    ...require("react"),
    useState(initial) {
      const index = stateIndex++;
      if (!(index in states)) states[index] = typeof initial === "function" ? initial() : initial;
      return [states[index], (value) => { states[index] = typeof value === "function" ? value(states[index]) : value; }];
    },
    useRef(initial) {
      const index = refIndex++;
      refs[index] ??= { current: initial };
      return refs[index];
    },
  };
  hooks.useCallback = (callback, deps) => {
    const index = callbackIndex++;
    const previous = callbacks[index];
    if (!previous || deps.some((value, position) => !Object.is(value, previous.deps[position]))) {
      callbacks[index] = { callback, deps };
    }
    return callbacks[index].callback;
  };
  hooks.useEffect = (effect, deps) => {
    const index = effectIndex++;
    const previous = effectStates[index];
    if (!previous || deps.some((value, position) => !Object.is(value, previous.deps[position]))) {
      effects.push(() => {
        previous?.cleanup?.();
        effectStates[index] = { deps, cleanup: effect() };
      });
    }
  };
  const Component = load(file, {}, { ...imports, react: hooks }).default;
  function render() {
    stateIndex = 0;
    refIndex = 0;
    effects = [];
    effectIndex = 0;
    callbackIndex = 0;
    return Component(props);
  }
  return { render, flushEffects() { for (const effect of effects) effect(); } };
}

function formDriver(source, onSave) {
  return componentDriver("src/components/nutrition/meal-record-form.tsx",
    { source, busy: false, onSave, onCancel: () => {} });
}

function elements(node, predicate) {
  if (!node || typeof node !== "object") return [];
  if (Array.isArray(node)) return node.flatMap((child) => elements(child, predicate));
  return [...(predicate(node) ? [node] : []), ...elements(node.props?.children, predicate)];
}

test("batch editing excludes a food, preserves unknown macros, and prevents duplicate pending submits", async () => {
  const saved = [];
  let finish;
  const waiting = new Promise((resolve) => { finish = resolve; });
  const driver = formDriver({ draft: { mealDate: "2026-10-01", mealType: "LUNCH", items: [
    { foodName: "밥", caloriesPerServing: 300 },
    { foodName: "닭", caloriesPerServing: 200 },
  ] } }, async (input) => { saved.push(input); await waiting; });
  let tree = driver.render();
  const calories = elements(tree, (element) => element.type === "input" && element.props.value === "300")[0];
  calories.props.onChange({ target: { value: "150" } });
  tree = driver.render();
  elements(tree, (element) => element.props?.["aria-label"] === "음식 2 제외")[0].props.onClick();
  tree = driver.render();
  tree.props.onSubmit({ preventDefault() {} });
  tree.props.onSubmit({ preventDefault() {} });
  assert.equal(saved.length, 1);
  assert.equal(saved[0].mealDate, "2026-10-01");
  assert.equal(saved[0].mealType, "LUNCH");
  assert.equal(saved[0].items.length, 1);
  assert.equal(saved[0].items[0].calories, 150);
  assert.equal(saved[0].items[0].proteinGrams, null);
  finish();
  await new Promise((resolve) => setImmediate(resolve));
});

test("editing an existing food keeps its date, meal type, and record id", async () => {
  let saved;
  const driver = formDriver({ item: { id: 42, mealDate: "2026-09-29", mealType: "BREAKFAST",
    foodName: "밥", calories: 100, proteinGrams: 0, carbohydrateGrams: null, fatGrams: null } },
  async (input, id) => { saved = { input, id }; });
  const tree = driver.render();
  tree.props.onSubmit({ preventDefault() {} });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(saved.id, 42);
  assert.equal(saved.input.mealDate, "2026-09-29");
  assert.equal(saved.input.mealType, "BREAKFAST");
  assert.equal(saved.input.items.length, 1);
  assert.equal(saved.input.items[0].proteinGrams, 0);
});

test("initial photo fills all foods once; nonfood and failures keep the existing draft", async () => {
  for (const status of ["FOOD", "NOT_FOOD", "UNCERTAIN", "ERROR"]) {
    const results = [];
    let calls = 0;
    const items = [{ foodName: "밥", caloriesPerServing: 300 }, { foodName: "닭", caloriesPerServing: 200 }];
    const driver = componentDriver("src/components/nutrition/meal-photo-input.tsx", {
      initialPhoto: { name: "meal.jpg" }, disabled: false,
      onAnalyzed: (result) => results.push(result), onBusyChange: () => {},
    }, {
      "@/lib/food-photo": { prepareFoodPhoto: async (photo) => photo },
      "@/lib/ai-api": { aiApi: {
        listConversations: async () => [{ id: 1 }],
        sendFoodPhoto: async () => {
          calls++;
          if (status === "ERROR") throw new Error("분석 실패");
          return { assistantMessage: { foodPhotoResult: { status, items: status === "FOOD" ? items : [] } } };
        },
      } },
    });
    driver.render();
    driver.flushEffects();
    await new Promise((resolve) => setImmediate(resolve));
    const view = driver.render();
    driver.flushEffects();
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(calls, 1);
    assert.equal(results.length, status === "FOOD" ? 1 : 0);
    if (status === "FOOD") {
      assert.equal(results[0].length, 2);
      assert.equal(results[0][0].foodName, "밥");
    } else {
      assert.equal(elements(view, (element) => element.props?.role === "alert").length, 1);
    }
  }
});

test("calendar distinguishes recorded zero and clears previous month before fetching", async () => {
  let selected;
  const props = { selectedDate: "2026-09-15", month: "2026-09", revision: 0,
    onMonthChange: (month) => { props.month = month; }, onSelectDate: (date) => { selected = date; } };
  const driver = componentDriver("src/components/nutrition/nutrition-calendar-view.tsx", props, {
    "@/lib/nutrition-api": { nutritionApi: { getCalendar: async () => [
      { date: "2026-09-01", calories: 0, mealTypes: ["LUNCH"] },
    ] } },
  });
  driver.render();
  driver.flushEffects();
  await new Promise((resolve) => setImmediate(resolve));
  let view = driver.render();
  const recorded = elements(view, (element) => element.props?.["aria-label"] === "2026-09-01 0 kcal");
  assert.equal(recorded.length, 1);
  assert.equal(elements(view, (element) => element.props?.["aria-label"] === "2026-09-02 기록 없음").length, 1);
  recorded[0].props.onClick();
  assert.equal(selected, "2026-09-01");
  elements(view, (element) => element.props?.["aria-label"] === "이전 달")[0].props.onClick();
  view = driver.render();
  assert.equal(elements(view, (element) => element.props?.["aria-label"] === "2026-09-01 0 kcal").length, 0);
  assert.equal(elements(view, (element) => element.props?.role === "status").length, 1);
});

test("canceling registration preserves the browsed calendar month", () => {
  const driver = componentDriver("src/components/nutrition/nutrition-screen.tsx", {});
  let view = driver.render();
  elements(view, (element) => element.type === "button" && element.props.children === "캘린더 조회")[0].props.onClick();
  view = driver.render();
  let calendar = elements(view, (element) => element.type?.name === "NutritionCalendarView")[0];
  assert.equal(typeof calendar.props.onMonthChange, "function");
  calendar.props.onMonthChange("2026-08");
  view = driver.render();
  elements(view, (element) => element.type === "button" && element.props.children === "식단 추가")[0].props.onClick();
  view = driver.render();
  elements(view, (element) => element.type?.name === "MealRecordForm")[0].props.onCancel();
  view = driver.render();
  calendar = elements(view, (element) => element.type?.name === "NutritionCalendarView")[0];
  assert.equal(calendar.props.month, "2026-08");
});

test("reopening the coach refreshes photo messages saved by the nutrition editor", async () => {
  let messages = [{ id: 1, role: "ASSISTANT", content: "이전 대화", foodPhotoResult: null }];
  const props = { currentView: "nutrition", open: true, onOpenChange: () => {}, onRecordFoods: () => {} };
  const driver = componentDriver("src/components/ai/ai-coach.tsx", props, {
    "@/lib/ai-api": { aiApi: {
      listConversations: async () => [{ id: 10, title: "대화" }],
      listMessages: async () => messages,
    } },
  });
  driver.render();
  driver.flushEffects();
  await new Promise((resolve) => setImmediate(resolve));
  props.open = false;
  driver.render();
  driver.flushEffects();
  messages = [...messages, { id: 2, role: "ASSISTANT", content: "새 사진 분석", foodPhotoResult: null }];
  props.open = true;
  driver.render();
  driver.flushEffects();
  await new Promise((resolve) => setImmediate(resolve));
  const view = driver.render();
  assert.equal(elements(view, (element) => element.props?.message?.id === 2).length, 1);
});

test("reopening during a text request cannot replace its new response with an older refresh", async () => {
  let serverMessages = [{ id: 1, role: "ASSISTANT", content: "이전", foodPhotoResult: null }];
  let completeSend;
  let releaseRefresh;
  let delayed = false;
  const conversation = { id: 10, title: "대화" };
  const props = { currentView: "nutrition", open: true, onOpenChange: () => {}, onRecordFoods: () => {} };
  const driver = componentDriver("src/components/ai/ai-coach.tsx", props, {
    "@/lib/ai-api": { aiApi: {
      listConversations: async () => [conversation],
      listMessages: async () => {
        const snapshot = serverMessages.slice();
        if (!delayed) return snapshot;
        return new Promise((resolve) => { releaseRefresh = () => resolve(snapshot); });
      },
      sendMessage: async () => new Promise((resolve) => { completeSend = resolve; }),
    } },
  });
  async function effects() {
    driver.render();
    driver.flushEffects();
    await new Promise((resolve) => setImmediate(resolve));
  }
  await effects();
  await effects();
  let view = driver.render();
  elements(view, (element) => element.type === "textarea")[0].props.onChange({ target: { value: "오늘 식단 평가" } });
  view = driver.render();
  elements(view, (element) => element.type === "form")[0].props.onSubmit({ preventDefault() {} });
  delayed = true;
  props.open = false;
  await effects();
  props.open = true;
  await effects();
  const userMessage = { id: 2, role: "USER", content: "질문", foodPhotoResult: null };
  const assistantMessage = { id: 3, role: "ASSISTANT", content: "새 답변", foodPhotoResult: null };
  serverMessages = [...serverMessages, userMessage, assistantMessage];
  completeSend({ conversation, userMessage, assistantMessage });
  await new Promise((resolve) => setImmediate(resolve));
  await effects();
  releaseRefresh();
  await new Promise((resolve) => setImmediate(resolve));
  view = driver.render();
  assert.equal(elements(view, (element) => element.props?.message?.id === 3).length, 1);
});

test("a failed text request keeps its error notice after the post-send refresh", async () => {
  let rejectSend;
  const driver = componentDriver("src/components/ai/ai-coach.tsx", {
    currentView: "nutrition", open: true, onOpenChange: () => {}, onRecordFoods: () => {},
  }, { "@/lib/ai-api": { aiApi: {
    listConversations: async () => [{ id: 10, title: "대화" }],
    listMessages: async () => [{ id: 1, role: "ASSISTANT", content: "이전", foodPhotoResult: null }],
    sendMessage: async () => new Promise((_, reject) => { rejectSend = reject; }),
  } } });
  async function effects() {
    driver.render();
    driver.flushEffects();
    await new Promise((resolve) => setImmediate(resolve));
  }
  await effects();
  await effects();
  let view = driver.render();
  elements(view, (element) => element.type === "textarea")[0].props.onChange({ target: { value: "식단 평가" } });
  view = driver.render();
  elements(view, (element) => element.type === "form")[0].props.onSubmit({ preventDefault() {} });
  await effects();
  rejectSend(new Error("연결 실패"));
  await new Promise((resolve) => setImmediate(resolve));
  await effects();
  view = driver.render();
  assert.equal(elements(view, (element) => element.type === "p" && element.props.children === "연결 실패").length, 1);
});
