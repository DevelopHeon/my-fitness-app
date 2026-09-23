"use client";

import { useEffect, useState } from "react";
import AppShell from "@/components/app-shell";
import { ApiError } from "@/lib/api-client";
import { authApi, CurrentUser } from "@/lib/auth-api";

type AuthState =
  | { status: "loading" }
  | { status: "unauthenticated" }
  | { status: "authenticated"; user: CurrentUser }
  | { status: "error"; message: string };

export default function AuthGate() {
  const [state, setState] = useState<AuthState>({
    status: "loading",
  });
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    let active = true;

    authApi.currentUser()
      .then((user) => {
        if (active) {
          setState({ status: "authenticated", user });
        }
      })
      .catch((error: unknown) => {
        if (!active) {
          return;
        }
        if (error instanceof ApiError && error.status === 401) {
          setState({ status: "unauthenticated" });
          return;
        }
        setState({
          status: "error",
          message: error instanceof Error
            ? error.message
            : "로그인 상태를 확인하지 못했습니다.",
        });
      });

    return () => {
      active = false;
    };
  }, []);

  async function logout() {
    if (loggingOut) {
      return;
    }
    setLoggingOut(true);
    try {
      await authApi.logout();
      setState({ status: "unauthenticated" });
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error
          ? error.message
          : "로그아웃하지 못했습니다.",
      });
    } finally {
      setLoggingOut(false);
    }
  }

  if (state.status === "loading") {
    return <AuthLoading />;
  }

  if (state.status === "unauthenticated") {
    return <LoginScreen />;
  }

  if (state.status === "error") {
    return (
      <LoginFrame>
        <p className="text-sm text-red-600">{state.message}</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="mt-5 w-full rounded-xl bg-zinc-950 px-4 py-3 text-sm font-semibold text-white"
        >
          다시 시도
        </button>
      </LoginFrame>
    );
  }

  return (
    <AppShell
      user={state.user}
      loggingOut={loggingOut}
      onLogout={logout}
    />
  );
}

function LoginScreen() {
  return (
    <LoginFrame>
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-zinc-400">
          My Fitness
        </p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-zinc-950">
          내 기록으로 운동을 이어가세요
        </h1>
        <p className="mt-3 text-sm leading-6 text-zinc-500">
          Google 계정으로 로그인하면 운동, 신체, 식단과 AI Coach 기록을
          사용자별로 안전하게 관리합니다.
        </p>
      </div>
      <button
        type="button"
        onClick={authApi.loginWithGoogle}
        className="w-full rounded-xl border border-zinc-300 bg-white px-4 py-3 text-sm font-semibold text-zinc-900 shadow-sm transition hover:bg-zinc-50"
      >
        Google로 로그인
      </button>
    </LoginFrame>
  );
}

function AuthLoading() {
  return (
    <LoginFrame>
      <div className="flex items-center gap-3 text-sm text-zinc-500">
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-zinc-300 border-t-zinc-900" />
        로그인 상태를 확인하는 중...
      </div>
    </LoginFrame>
  );
}

function LoginFrame({ children }: { children: React.ReactNode }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-50 px-5">
      <section className="w-full max-w-sm rounded-3xl border border-zinc-200 bg-white p-7 shadow-sm">
        {children}
      </section>
    </main>
  );
}
