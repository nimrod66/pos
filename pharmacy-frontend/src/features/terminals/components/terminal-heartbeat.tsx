"use client";

import { useEffect } from "react";

import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  getLocalTerminalId,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";

export function TerminalHeartbeat() {
  const authenticated = useAuthStore((state) => state.status === "authenticated");

  useEffect(() => {
    if (!authenticated) return;

    function sendHeartbeat() {
      const terminalId = getLocalTerminalId();
      if (terminalId) void terminalGateway.heartbeat(terminalId).catch(() => undefined);
    }

    sendHeartbeat();
    const timer = window.setInterval(sendHeartbeat, 120_000);
    window.addEventListener("online", sendHeartbeat);
    window.addEventListener("pharmacy-pos:terminal-assignment", sendHeartbeat);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener("online", sendHeartbeat);
      window.removeEventListener(
        "pharmacy-pos:terminal-assignment",
        sendHeartbeat,
      );
    };
  }, [authenticated]);

  return null;
}
