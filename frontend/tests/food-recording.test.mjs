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
  vm.runInNewContext(output, { module: loadedModule, exports: loadedModule.exports, process, console, Headers, FormData,
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
      onRecordFood: () => calls++,
    }));
    assert.equal(html.includes("식단에 기록하기"), status === "FOOD");
  }
  assert.equal(calls, 0);
});

test("photo draft opens an editable form with optional macros initially closed", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Screen = load("src/components/nutrition/nutrition-screen.tsx").default;
  const html = renderToStaticMarkup(React.createElement(Screen, { initialDraft: {
    foodName: "밥", caloriesPerServing: 300, servingDescription: "1그릇", mealDate: "2026-10-01", mealType: "LUNCH",
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

test("food photo upload is offered only in the nutrition coach while text chat stays available", () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const Coach = load("src/components/ai/ai-coach.tsx").default;
  for (const currentView of ["dashboard", "workout", "routine", "body", "nutrition"]) {
    const html = renderToStaticMarkup(React.createElement(Coach, {
      currentView,
      open: true,
      onOpenChange: () => {},
      onRecordFood: () => {},
    }));
    assert.equal(html.includes('type="file"'), currentView === "nutrition", currentView);
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
    Coach({ currentView: "nutrition", open, onOpenChange: () => {}, onRecordFood: () => {} });
    for (const effect of effects) effect();
  }

  render(true);
  render(false);
  render(true);
  resolveList([]);
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(created, 1);
});
