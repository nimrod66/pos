import { create } from "zustand";

import { authGateway } from "@/features/auth/lib/auth-gateway";
import type {
  AuthSession,
  LoginCredentials,
} from "@/features/auth/types";

type AuthStatus = "checking" | "anonymous" | "authenticated";

interface AuthStore {
  error: string | null;
  session: AuthSession | null;
  status: AuthStatus;
  clearError(): void;
  expireSession(): void;
  restoreSession(): Promise<void>;
  signIn(credentials: LoginCredentials): Promise<AuthSession>;
  signOut(): Promise<void>;
  switchBranch(branchId: string): Promise<AuthSession>;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  error: null,
  session: null,
  status: "checking",
  clearError() {
    set({ error: null });
  },
  expireSession() {
    const wasAuthenticated = get().status === "authenticated";
    set({ error: null, session: null, status: "anonymous" });
    if (wasAuthenticated) {
      void authGateway.logout().catch(() => undefined);
    }
  },
  async restoreSession() {
    if (get().status !== "checking") {
      return;
    }

    try {
      const session = await authGateway.restore();
      set({
        error: null,
        session,
        status: session ? "authenticated" : "anonymous",
      });
    } catch {
      set({ error: null, session: null, status: "anonymous" });
    }
  },
  async signIn(credentials: LoginCredentials) {
    set({ error: null });

    try {
      const session = await authGateway.login(credentials);
      set({ error: null, session, status: "authenticated" });
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
      set({ error: null, session: null, status: "anonymous" });
    }
  },
  async switchBranch(branchId: string) {
    set({ error: null });
    try {
      const session = await authGateway.switchBranch(branchId);
      set({ error: null, session, status: "authenticated" });
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
