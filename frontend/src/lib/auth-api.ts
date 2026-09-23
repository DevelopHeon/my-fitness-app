import {
  apiUrl,
  request,
  resetCsrfToken,
} from "@/lib/api-client";

export type CurrentUser = {
  id: number;
  email: string | null;
  displayName: string | null;
  profileImageUrl: string | null;
};

export const authApi = {
  currentUser: () => request<CurrentUser>("/api/users/me"),

  loginWithGoogle: () => {
    window.location.assign(apiUrl("/oauth2/authorization/google"));
  },

  logout: async () => {
    try {
      await request<void>("/logout", { method: "POST" });
    } finally {
      resetCsrfToken();
    }
  },
};
