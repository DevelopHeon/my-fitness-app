import type { MetadataRoute } from "next";

export const dynamic = "force-static";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "My Fitness",
    short_name: "My Fitness",
    description: "개인 운동, 신체, 식단 기록과 로컬 AI 코치",
    start_url: "/",
    display: "standalone",
    background_color: "#fafafa",
    theme_color: "#111827",
    icons: [
      {
        src: "/icon.svg",
        sizes: "any",
        type: "image/svg+xml",
        purpose: "any",
      },
    ],
  };
}
