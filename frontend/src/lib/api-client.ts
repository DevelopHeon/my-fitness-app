const USER_ID = "1";

function apiBase() {
  if (process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }
  if (typeof window !== "undefined" && window.location.port === "3000") {
    return "http://localhost:8080";
  }
  return "";
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(apiBase() + path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": USER_ID,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.message ?? "요청 처리 중 오류가 발생했습니다.");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}
