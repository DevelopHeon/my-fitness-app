"use client";

import { useEffect } from "react";

export default function ServiceWorkerRegister() {
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {
        // PWA 등록 실패는 핵심 기록 기능을 막지 않는다.
      });
    }
  }, []);

  return null;
}
