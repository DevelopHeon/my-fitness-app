let csrfToken: {
  headerName: string;
  token: string;
} | null = null;

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function apiUrl(path: string) {
  return apiBase() + path;
}

export function resetCsrfToken() {
  csrfToken = null;
}

export async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);

  if (init.body != null && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (requiresCsrf(method)) {
    const token = await getCsrfToken();
    headers.set(token.headerName, token.token);
  }

  const response = await fetch(apiUrl(path), {
    ...init,
    method,
    credentials: "include",
    headers,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(
      error?.message ?? "요청 처리 중 오류가 발생했습니다.",
      response.status,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

function apiBase() {
  if (process.env.NEXT_PUBLIC_API_BASE_URL) {
    return process.env.NEXT_PUBLIC_API_BASE_URL;
  }
  if (typeof window !== "undefined" && window.location.port === "3000") {
    return "http://localhost:8080";
  }
  return "";
}

function requiresCsrf(method: string) {
  return !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
}

async function getCsrfToken() {
  if (csrfToken) {
    return csrfToken;
  }

  const response = await fetch(apiUrl("/api/auth/csrf"), {
    credentials: "include",
  });

  if (!response.ok) {
    throw new ApiError(
      "보안 토큰을 발급받지 못했습니다.",
      response.status,
    );
  }

  const token = await response.json() as {
    headerName: string;
    token: string;
  };
  csrfToken = token;
  return token;
}
