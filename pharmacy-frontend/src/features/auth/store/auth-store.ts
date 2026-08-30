import { create } from "zustand";

import { authGateway } from "@/features/auth/lib/auth-gateway";
import { ApiClientError } from "@/lib/api-client";
import type {
  AuthSession,
  LoginCredentials,
} from "@/features/auth/types";

type AuthStatus = "checking" | "anonymous" | "authenticated";

interface AuthStore {
  error: string | null;
  expired: boolean;
  offline: boolean;
  session: AuthSession | null;
  status: AuthStatus;
  clearError(): void;
  expireSession(): void;
  restoreSession(): Promise<void>;
  signIn(credentials: LoginCredentials): Promise<AuthSession>;
  signOut(): Promise<void>;
  switchBranch(branchId: string): Promise<AuthSession>;
}

const CACHE_KEY = "pharmacy-pos:session-cache";

function readCachedSession(): AuthSession | null {
  try {
    const raw = window.localStorage.getItem(CACHE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}

function writeCachedSession(session: AuthSession | null) {
  try {
    if (session) {
      window.localStorage.setItem(CACHE_KEY, JSON.stringify(session));
    } else {
      window.localStorage.removeItem(CACHE_KEY);
    }
  } catch {
    // Storage may be unavailable; offline grace degrades gracefully.
  }
}

function isNetworkFailure(error: unknown) {
  // apiRequest maps fetch failures to status 0 / NETWORK_ERROR.
  return (
    (error instanceof ApiClientError && error.status === 0) ||
    (error instanceof TypeError)
  );
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  error: null,
  expired: false,
  offline: false,
  session: null,
  status: "checking",
  clearError() {
    set({ error: null });
  },
  expireSession() {
    const wasAuthenticated = get().status === "authenticated";
    // The server session is already gone; calling logout() on a dead
    // session only produces noise.
    set({ error: null, expired: wasAuthenticated, offline: false, session: null, status: "anonymous" });
    writeCachedSession(null);
  },
  async restoreSession() {
    if (get().status !== "checking") {
      return;
    }

    try {
      const session = await authGateway.restore();
      writeCachedSession(session);
      set({
        error: null,
        expired: false,
        offline: false,
        session,
        status: session ? "authenticated" : "anonymous",
      });
    } catch (error) {
      // Network failure with a cached session: keep working in offline
      // mode instead of locking the operator out of the till.
      if (isNetworkFailure(error)) {
        const cached = readCachedSession();
        if (cached) {
          set({
            error: null,
            expired: false,
            offline: true,
            session: cached,
            status: "authenticated",
          });
          return;
        }
      }
      set({ error: null, expired: false, offline: false, session: null, status: "anonymous" });
      writeCachedSession(null);
    }
  },
  async signIn(credentials: LoginCredentials) {
    set({ error: null, expired: false });

    try {
      const session = await authGateway.login(credentials);
      writeCachedSession(session);
      set({ error: null, expired: false, offline: false, session, status: "authenticated" });
      return session;
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Sign in could not be completed.";
      set({ error: message, session: null, status: "anonymous" });
      throw error;
    }
  },
  async signOut() {
    try {
      await authGateway.logout();
    } finally {
      set({ error: null, expired: false, offline: false, session: null, status: "anonymous" });
      writeCachedSession(null);
    }
  },
  async switchBranch(branchId: string) {
    set({ error: null });
    try {
      const session = await authGateway.switchBranch(branchId);
      writeCachedSession(session);
      set({ error: null, expired: false, offline: false, session, status: "authenticated" });
      return session;
    } catch (error) {
      set({
        error:
          error instanceof Error
            ? error.message
            : "The active branch could not be changed.",
      });
      throw error;
    }
  },
}));
