const features = [
  ["Workout", "운동과 세트 기록"],
  ["Routine", "반복 운동 루틴"],
  ["Body", "체중·체지방·골격근"],
  ["Dashboard", "운동량과 변화 추이"],
  ["Nutrition", "식단과 탄단지 기록"],
  ["AI Coach", "Ollama 기반 기록 분석"],
];

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col gap-8 px-5 py-10">
      <header className="space-y-2">
        <p className="text-sm font-medium text-zinc-500">MY FITNESS</p>
        <h1 className="text-3xl font-bold tracking-tight">운동 기록부터 AI 코치까지</h1>
        <p className="text-sm leading-6 text-zinc-600">
          개인 운동, 신체 변화와 식단을 기록하고 로컬 AI가 데이터를 해석하는 개인용 피트니스 앱입니다.
        </p>
      </header>

      <section className="grid grid-cols-2 gap-3" aria-label="개발 기능">
        {features.map(([title, description], index) => (
          <article key={title} className="rounded-2xl border border-zinc-200 bg-white p-4 shadow-sm">
            <span className="text-xs font-semibold text-zinc-400">PHASE {index + 1}</span>
            <h2 className="mt-2 font-semibold">{title}</h2>
            <p className="mt-1 text-sm leading-5 text-zinc-500">{description}</p>
          </article>
        ))}
      </section>

      <footer className="mt-auto rounded-2xl bg-zinc-900 p-4 text-sm leading-6 text-zinc-200">
        현재는 프로젝트 기반 구성을 위한 PWA 셸입니다. 첫 구현 기능은 Workout 기록입니다.
      </footer>
    </main>
  );
}
