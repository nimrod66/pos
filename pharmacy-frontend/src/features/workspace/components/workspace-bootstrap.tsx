"use client";

import { useEffect } from "react";

import { useAuthStore } from "@/features/auth/store/auth-store";
import { useCartStore } from "@/features/pos/store/cart-store";
import { workspaceGateway } from "@/features/workspace/gateway/workspace-gateway";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

export function WorkspaceBootstrap() {
  const status = useAuthStore((state) => state.status);
  const userId = useAuthStore((state) => state.session?.user.id);

  useEffect(() => {
    void useCartStore.persist.rehydrate();
  }, []);

  useEffect(() => {
    if (!DEMO_AUTH_ENABLED && status === "checking") return;
    void workspaceGateway.hydrate().catch(() => undefined);
  }, [status, userId]);

  return null;
}
