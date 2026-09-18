import type { Metadata, Viewport } from "next";
import ServiceWorkerRegister from "@/components/service-worker-register";
import "./globals.css";

export const metadata: Metadata = {
  title: "My Fitness",
  description: "개인 운동, 신체, 식단 기록과 로컬 AI 코치",
  applicationName: "My Fitness",
};

export const viewport: Viewport = {
  themeColor: "#111827",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko">
      <body className="min-h-screen bg-zinc-50 text-zinc-950 antialiased">
        <ServiceWorkerRegister />
        {children}
      </body>
    </html>
  );
}
