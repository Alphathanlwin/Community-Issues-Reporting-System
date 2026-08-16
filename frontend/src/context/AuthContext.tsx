import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from "@/lib/constants";
import { authService } from "@/services/authService";
import type { AuthUser, LoginRequest, Role } from "@/types/auth";

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<AuthUser>;
  logout: () => void;
  isRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

/** Holds the current session and exposes login/logout. Wrap the app once, above the router. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());
  const [isLoading, setIsLoading] = useState(Boolean(token));

  useEffect(() => {
    if (!token) {
      setIsLoading(false);
      return;
    }
    authService
      .me()
      .then((freshUser) => {
        setUser(freshUser);
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(freshUser));
      })
      .catch(() => {
        setToken(null);
        setUser(null);
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(USER_STORAGE_KEY);
      })
      .finally(() => setIsLoading(false));
    // Only re-hydrate when the token itself changes (login/logout).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      isLoading,
      login: async (data) => {
        const response = await authService.login(data);
        const authUser: AuthUser = {
          userId: response.userId,
          fullName: response.fullName,
          email: response.email,
          role: response.role,
          departmentId: response.departmentId,
          accountStatus: response.accountStatus,
        };
        localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(authUser));
        setToken(response.token);
        setUser(authUser);
        return authUser;
      },
      logout: () => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(USER_STORAGE_KEY);
        setToken(null);
        setUser(null);
      },
      isRole: (...roles) => Boolean(user && roles.includes(user.role)),
    }),
    [user, token, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
