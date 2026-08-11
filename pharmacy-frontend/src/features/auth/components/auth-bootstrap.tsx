"use client";

import { useEffect } from "react";

import { useAuthStore } from "@/features/auth/store/auth-store";
import { SESSION_EXPIRED_EVENT } from "@/lib/api-client";

export function AuthBootstrap() {
  const restoreSession = useAuthStore((state) => state.restoreSession);
  const expireSession = useAuthStore((state) => state.expireSession);
  const session = useAuthStore((state) => state.session);
  const status = useAuthStore((state) => state.status);

  useEffect(() => {
    void restoreSession();
  }, [restoreSession]);

  useEffect(() => {
    window.addEventListener(SESSION_EXPIRED_EVENT, expireSession);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expireSession);
  }, [expireSession]);

  useEffect(() => {
    if (status !== "authenticated" || !session) return;

    const remainingMs = Date.parse(session.expiresAt) - Date.now();
    if (!Number.isFinite(remainingMs) || remainingMs <= 0) {
      expireSession();
      return;
    }

    const timeout = window.setTimeout(expireSession, remainingMs);
    return () => window.clearTimeout(timeout);
  }, [expireSession, session, status]);

  return null;
}
