export default function LoadingSpinner({ className }: { className: string }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"
      className={"ai-loading-spinner shrink-0 " + className} fill="none" stroke="currentColor" strokeWidth="3">
      <circle cx="12" cy="12" r="9" opacity="0.15" />
      <circle cx="12" cy="12" r="9" pathLength="100" strokeDasharray="25 75" strokeLinecap="round" />
    </svg>
  );
}
