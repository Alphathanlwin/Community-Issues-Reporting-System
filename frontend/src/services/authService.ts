import { api } from "@/lib/api";
import type { AuthResponse, AuthUser, LoginRequest } from "@/types/auth";

export const authService = {
  login: (data: LoginRequest) => api.post<AuthResponse>("/auth/login", data).then((r) => r.data),

  me: () => api.get<AuthUser>("/auth/me").then((r) => r.data),
};
