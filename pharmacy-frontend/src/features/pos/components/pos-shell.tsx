"use client";

import { ArrowLeft, ClipboardCheck, LogOut } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { BrandMark } from "@/components/brand-mark";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import {
  canAccess,
  homePathForPermissions,
  PERMISSIONS,
} from "@/features/auth/access-control";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";

export function PosShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const session = useAuthStore((state) => state.session);
  const status = useAuthStore((state) => state.status);
  const signOut = useAuthStore((state) => state.signOut);
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);

  useEffect(() => {
    if (status === "anonymous") router.replace("/login");
  }, [router, status]);

  if (status !== "authenticated" || !session) {
    return <div className="flex min-h-screen items-center justify-center text-sm text-[var(--text-muted)]">Opening point of sale...</div>;
  }

  const homePath = homePathForPermissions(session.user.permissions);
  const canSell = session.user.permissions.includes(PERMISSIONS.POS_SELL);
  const shiftAccess = canAccess(session.user.permissions, {
    anyOf: [PERMISSIONS.SHIFT_OPEN, PERMISSIONS.SHIFT_CLOSE],
  });

  if (!canSell) {
    return (
      <main className="min-h-screen bg-[var(--surface-muted)] p-4 sm:p-8">
        <div className="mx-auto max-w-xl">
          <AccessRestricted homePath={homePath} />
        </div>
      </main>
    );
  }

  async function handleSignOut() {
    await signOut();
    router.replace("/login");
  }

  return (
    <div className="min-h-screen bg-[var(--surface-muted)]">
      <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-[var(--border)] bg-white px-3 sm:px-5">
        <Link href={homePath} aria-label="Back to workspace" title="Back to workspace" className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]">
          <ArrowLeft aria-hidden="true" size={19} />
        </Link>
        <BrandMark className="size-8" />
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold">Point of sale</p>
          <p className="truncate text-xs text-[var(--text-muted)]">{session.user.activeBranch.name}</p>
        </div>
        <div className="ml-auto flex items-center gap-2">
          {shiftAccess ? <Link href="/shifts/current" className={`hidden min-h-9 items-center gap-2 rounded-md px-3 text-xs font-semibold sm:flex ${currentShiftId ? "bg-[var(--success-soft)] text-[var(--success)]" : "bg-[var(--warning-soft)] text-[var(--warning)]"}`}>
            <ClipboardCheck aria-hidden="true" size={15} /> {currentShiftId ? "Shift open" : "Open shift"}
          </Link> : null}
          <span className="hidden text-sm font-medium md:inline">{session.user.displayName}</span>
          <button type="button" onClick={() => void handleSignOut()} aria-label="Sign out" title="Sign out" className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]">
            <LogOut aria-hidden="true" size={18} />
          </button>
        </div>
      </header>
      <main>{children}</main>
    </div>
  );
}
