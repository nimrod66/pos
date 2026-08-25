"use client";

import { ArrowLeft, Boxes, ClipboardCheck, History, LogOut, ShoppingCart, Users } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";

import { BrandMark } from "@/components/brand-mark";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import {
  canAccess,
  homePathForPermissions,
  PERMISSIONS,
} from "@/features/auth/access-control";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { useCartStore } from "@/features/pos/store/cart-store";
import { NotificationMenu } from "@/features/notifications/components/notification-menu";
import { PeripheralHealthBar } from "@/features/terminals/components/peripheral-health-bar";
import { useWorkspaceQuery } from "@/features/workspace/gateway/workspace-gateway";
import { cn } from "@/lib/cn";

export function PosShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const session = useAuthStore((state) => state.session);
  const status = useAuthStore((state) => state.status);
  const signOut = useAuthStore((state) => state.signOut);
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);
  const linesCount = useCartStore((state) => state.lines.length);

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
  const salesAccess = canAccess(session.user.permissions, {
    allOf: [PERMISSIONS.SALE_READ],
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

  // Quick-jump tabs keep the operator inside the selling flow while still
  // reaching shifts, receipts, and customers in one tap - sized for touch
  // on handhelds as well as desktop.
  const quickTabs: Array<{
    href: string;
    label: string;
    icon: React.ComponentType<{ size?: number | string }>;
    badge?: string | number;
    show: boolean;
  }> = [
    { href: "/pos", label: "Sell", icon: ShoppingCart, show: true },
    {
      href: "/shifts/current",
      label: "Shift",
      icon: ClipboardCheck,
      badge: currentShiftId ? "open" : undefined,
      show: shiftAccess,
    },
    { href: "/sales", label: "Receipts", icon: History, show: salesAccess },
    {
      href: "/customers",
      label: "Customers",
      icon: Users,
      show: canAccess(session.user.permissions, { allOf: [PERMISSIONS.CUSTOMER_READ] }),
    },
    {
      href: "/inventory",
      label: "Stock",
      icon: Boxes,
      show: canAccess(session.user.permissions, { allOf: [PERMISSIONS.INVENTORY_READ] }),
    },
  ].filter((tab) => tab.show);

  return (
    <div className="flex min-h-screen flex-col bg-[var(--surface-muted)]">
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
          <NotificationMenu branchId={session.user.activeBranch.id} />
          {shiftAccess ? <Link href="/shifts/current" className={`hidden min-h-9 items-center gap-2 rounded-md px-3 text-xs font-semibold sm:flex ${currentShiftId ? "bg-[var(--success-soft)] text-[var(--success)]" : "bg-[var(--warning-soft)] text-[var(--warning)]"}`}>
            <ClipboardCheck aria-hidden="true" size={15} /> {currentShiftId ? "Shift open" : "Open shift"}
          </Link> : null}
          <span className="hidden text-sm font-medium md:inline">{session.user.displayName}</span>
          <button type="button" onClick={() => void handleSignOut()} aria-label="Sign out" title="Sign out" className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--danger-soft)] hover:text-[var(--danger)]">
            <LogOut aria-hidden="true" size={18} />
          </button>
        </div>
      </header>
      <PeripheralHealthBar canConfigure={session.user.permissions.includes(PERMISSIONS.TERMINAL_READ)} />
      <main className="flex-1 pb-16">{children}</main>

      {/* Handheld-friendly quick navigation. */}
      <nav
        aria-label="Point of sale sections"
        className="fixed inset-x-0 bottom-0 z-30 flex border-t border-[var(--border)] bg-white md:hidden"
      >
        {quickTabs.map(({ href, label, icon: Icon, badge }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              aria-current={active ? "page" : undefined}
              className={cn(
                "relative flex flex-1 flex-col items-center gap-0.5 py-2.5 text-[11px] font-medium",
                active ? "text-[var(--brand-strong)]" : "text-[var(--text-muted)]",
              )}
            >
              <Icon size={19} />
              {label}
              {label === "Sell" && linesCount > 0 ? (
                <span className="absolute right-1/2 top-1 translate-x-4 rounded-full bg-[var(--brand)] px-1.5 text-[10px] font-bold leading-4 text-white">
                  {linesCount}
                </span>
              ) : null}
              {badge === "open" ? (
                <span className="absolute right-1/2 top-1 translate-x-4 size-2 rounded-full bg-[var(--success)]" />
              ) : null}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
