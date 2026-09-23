import type { Metadata, Viewport } from "next";
import ServiceWorkerRegister from "@/components/service-worker-register";
import "./globals.css";

export const metadata: Metadata = {
  title: "My Fitness",
  description: "개인 운동, 신체, 식단 기록과 AI 코치",
  applicationName: "My Fitness",
  appleWebApp: {
    capable: true,
    title: "My Fitness",
    statusBarStyle: "default",
  },
  other: {
    "apple-mobile-web-app-capable": "yes",
    "mobile-web-app-capable": "yes",
  },
  icons: {
    icon: [
      {
        url: "/icon-192.png",
        sizes: "192x192",
        type: "image/png",
      },
      {
        url: "/icon-512.png",
        sizes: "512x512",
        type: "image/png",
      },
    ],
    apple: [
      {
        url: "/apple-touch-icon.png",
        sizes: "180x180",
        type: "image/png",
      },
    ],
  },
};

export const viewport: Viewport = {
  themeColor: "#18181b",
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
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
