"use client";

import {
  canAccess,
  type AccessRule,
  type Permission,
} from "@/features/auth/access-control";
import { useAuthStore } from "@/features/auth/store/auth-store";

export function usePermission(permission: Permission) {
  return useAuthStore(
    (state) => state.session?.user.permissions.includes(permission) ?? false,
  );
}

export function useAccess(rule: AccessRule) {
  return useAuthStore((state) =>
    canAccess(state.session?.user.permissions ?? [], rule),
  );
}
