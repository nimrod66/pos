"use client";

import type { LucideIcon } from "lucide-react";
import {
  Activity,
  AlertTriangle,
  ArrowRightLeft,
  BarChart3,
  Building2,
  ChevronDown,
  ClipboardCheck,
  ClipboardList,
  Boxes,
  ContactRound,
  FileText,
  FileClock,
  History,
  LayoutDashboard,
  LogOut,
  Menu,
  Monitor,
  Pill,
  ReceiptText,
  RefreshCw,
  Settings2,
  ShoppingCart,
  ShieldCheck,
  Truck,
  Users,
  Wallet,
  X,
  ListChecks,
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { BrandMark } from "@/components/brand-mark";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import {
  canAccess,
  canAccessPath,
  homePathForPermissions,
  PERMISSIONS,
  roleLabel,
  type AccessRule,
  type Permission,
} from "@/features/auth/access-control";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { useCartStore } from "@/features/pos/store/cart-store";
import { NotificationMenu } from "@/features/notifications/components/notification-menu";
import { PeripheralHealthBar } from "@/features/terminals/components/peripheral-health-bar";
import {
  type BranchSummary,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";
import {
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";
import { cn } from "@/lib/cn";

interface NavigationItem {
  href: string;
  icon: LucideIcon;
  label: string;
  access?: AccessRule;
  section: "Workspace" | "Operations" | "Management";
}

export const appNavigation: NavigationItem[] = [
  {
    href: "/dashboard",
    icon: LayoutDashboard,
    label: "Dashboard",
    access: { allOf: [PERMISSIONS.DASHBOARD_READ] },
    section: "Workspace",
  },
  {
    href: "/pos",
    icon: ShoppingCart,
    label: "Point of sale",
    access: { allOf: [PERMISSIONS.POS_SELL] },
    section: "Workspace",
  },
  {
    href: "/shifts/current",
    icon: ClipboardCheck,
    label: "Current shift",
    access: { anyOf: [PERMISSIONS.SHIFT_OPEN, PERMISSIONS.SHIFT_CLOSE] },
    section: "Workspace",
  },
  {
    href: "/medicines",
    icon: Pill,
    label: "Medicines",
    access: { allOf: [PERMISSIONS.MEDICINE_READ] },
    section: "Operations",
  },
  {
    href: "/prescriptions",
    icon: FileText,
    label: "Prescriptions",
    access: { allOf: [PERMISSIONS.PRESCRIPTION_READ] },
    section: "Operations",
  },
  {
    href: "/inventory",
    icon: Boxes,
    label: "Inventory",
    access: { allOf: [PERMISSIONS.INVENTORY_READ] },
    section: "Operations",
  },
  {
    href: "/inventory/stock-counts",
    icon: ListChecks,
    label: "Stock counts",
    access: { allOf: [PERMISSIONS.STOCK_COUNT_READ] },
    section: "Operations",
  },
  {
    href: "/inventory/transfers",
    icon: ArrowRightLeft,
    label: "Stock transfers",
    access: { allOf: [PERMISSIONS.STOCK_TRANSFER_READ] },
    section: "Operations",
  },
  {
    href: "/suppliers",
    icon: Truck,
    label: "Suppliers & GRN",
    access: { allOf: [PERMISSIONS.SUPPLIER_READ] },
    section: "Operations",
  },
  {
    href: "/procurement/purchase-orders",
    icon: ClipboardList,
    label: "Purchase orders",
    access: { allOf: [PERMISSIONS.PURCHASE_ORDER_READ] },
    section: "Operations",
  },
  {
    href: "/procurement/grn/new",
    icon: RefreshCw,
    label: "Receive stock (GRN)",
    access: { allOf: [PERMISSIONS.INVENTORY_RECEIVE] },
    section: "Operations",
  },
  {
    href: "/customers",
    icon: ContactRound,
    label: "Customers",
    access: { allOf: [PERMISSIONS.CUSTOMER_READ] },
    section: "Operations",
  },
  {
    href: "/sales",
    icon: ReceiptText,
    label: "Sales & receipts",
    access: { allOf: [PERMISSIONS.SALE_READ] },
    section: "Operations",
  },
  {
    href: "/expenses",
    icon: Wallet,
    label: "Expenses",
    access: { allOf: [PERMISSIONS.EXPENSE_READ] },
    section: "Operations",
  },
  {
    href: "/insurance",
    icon: ClipboardCheck,
    label: "Insurance",
    access: { allOf: [PERMISSIONS.INSURANCE_READ] },
    section: "Operations",
  },
  {
    href: "/insurance/claims",
    icon: ClipboardList,
    label: "Insurance Claims",
    access: { allOf: [PERMISSIONS.INSURANCE_READ] },
    section: "Operations",
  },
  {
    href: "/admin/controlled-drugs",
    icon: ShieldCheck,
    label: "Controlled drugs",
    access: { allOf: [PERMISSIONS.CONTROLLED_DRUGS_READ] },
    section: "Operations",
  },
  {
    href: "/reports",
    icon: BarChart3,
    label: "Reports",
    access: {
      anyOf: [PERMISSIONS.REPORT_SALES_READ, PERMISSIONS.REPORT_INVENTORY_READ],
    },
    section: "Management",
  },
  {
    href: "/admin/users",
    icon: Users,
    label: "Staff",
    access: { allOf: [PERMISSIONS.USER_MANAGE] },
    section: "Management",
  },
  {
    href: "/admin/branches",
    icon: Building2,
    label: "Branches",
    access: { allOf: [PERMISSIONS.SETTINGS_MANAGE] },
    section: "Management",
  },
  {
    href: "/admin/shifts",
    icon: History,
    label: "Shift history",
    access: { allOf: [PERMISSIONS.SHIFT_VARIANCE_APPROVE] },
    section: "Management",
  },
  {
    href: "/admin/settings",
    icon: Settings2,
    label: "Settings",
    access: { allOf: [PERMISSIONS.SETTINGS_MANAGE] },
    section: "Management",
  },
  {
    href: "/admin/terminals",
    icon: Monitor,
    label: "Terminals",
    access: { allOf: [PERMISSIONS.TERMINAL_READ] },
    section: "Management",
  },
  {
    href: "/admin/audit",
    icon: FileClock,
    label: "Audit log",
    access: { allOf: [PERMISSIONS.AUDIT_READ] },
    section: "Management",
  },
  {
    href: "/system",
    icon: Activity,
    label: "System health",
    access: { anyOf: [PERMISSIONS.TERMINAL_MANAGE, PERMISSIONS.SETTINGS_MANAGE] },
    section: "Management",
  },
];

export function visibleNavigation(
  items: NavigationItem[],
  permissions: Permission[],
) {
  return items.filter((item) => canAccess(permissions, item.access));
}

function LoadingWorkspace() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--surface-muted)]">
      <div className="flex items-center gap-3 text-sm text-[var(--text-muted)]">
        <span
          className="size-4 animate-spin rounded-full border-2 border-[var(--border-strong)] border-t-[var(--brand)]"
          aria-hidden="true"
        />
        Restoring workspace
      </div>
    </div>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [navigationOpen, setNavigationOpen] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [branches, setBranches] = useState<BranchSummary[]>([]);
  const [switchingBranch, setSwitchingBranch] = useState(false);
  const [branchError, setBranchError] = useState<string | null>(null);
  const session = useAuthStore((state) => state.session);
  const offline = useAuthStore((state) => state.offline);
  const signOut = useAuthStore((state) => state.signOut);
  const switchBranch = useAuthStore((state) => state.switchBranch);
  const status = useAuthStore((state) => state.status);
  const currentShiftId = useWorkspaceQuery((state) => state.currentShiftId);
  const shifts = useWorkspaceQuery((state) => state.shifts);
  const loadError = useWorkspaceQuery((state) => state.loadError);
  const cartLines = useCartStore((state) => state.lines);
  const clearCart = useCartStore((state) => state.clear);

  useEffect(() => {
    if (status === "anonymous") {
      router.replace("/login");
    }
  }, [router, status]);

  useEffect(() => {
    if (!session?.user.roles.includes("OWNER")) return;
    let active = true;
    void terminalGateway
      .listBranches(session.user.pharmacyId)
      .then((rows) => {
        if (active) setBranches(rows.filter((branch) => branch.status === "ACTIVE"));
      })
      .catch(() => {
        if (active) setBranches([]);
      });
    return () => {
      active = false;
    };
  }, [session]);

  if (status !== "authenticated" || !session) {
    return <LoadingWorkspace />;
  }

  const navigation = visibleNavigation(
    appNavigation,
    session.user.permissions,
  );
  const routeAllowed = canAccessPath(pathname, session.user.permissions);
  const shiftAccess = canAccess(session.user.permissions, {
    anyOf: [PERMISSIONS.SHIFT_OPEN, PERMISSIONS.SHIFT_CLOSE],
  });
  const peripheralHealthAccess = canAccess(session.user.permissions, {
    anyOf: [
      PERMISSIONS.POS_SELL,
      PERMISSIONS.INVENTORY_READ,
      PERMISSIONS.TERMINAL_READ,
      PERMISSIONS.TERMINAL_MANAGE,
    ],
  });
  const initials = session.user.displayName
    .split(" ")
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  const currentShift = shifts.find((shift) => shift.id === currentShiftId);

  async function handleSignOut() {
    await signOut();
    router.replace("/login");
  }

  async function handleBranchChange(branchId: string) {
    if (!session || branchId === session.user.activeBranch.id) return;
    setSwitchingBranch(true);
    setBranchError(null);
    try {
      await switchBranch(branchId);
      clearCart();
      await workspaceGateway.hydrate();
      router.refresh();
    } catch (error) {
      setBranchError(
        error instanceof Error
          ? error.message
          : "The active branch could not be changed.",
      );
    } finally {
      setSwitchingBranch(false);
    }
  }

  return (
    <div className="min-h-screen bg-[var(--surface-muted)] text-[var(--text)]">
      {navigationOpen ? (
        <button
          type="button"
          aria-label="Close navigation"
          className="fixed inset-0 z-30 bg-black/30 lg:hidden"
          onClick={() => setNavigationOpen(false)}
        />
      ) : null}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-[var(--border)] bg-white transition-transform print:hidden lg:translate-x-0",
          navigationOpen
            ? "visible translate-x-0"
            : "invisible -translate-x-full lg:visible",
        )}
      >
        <div className="flex h-16 items-center gap-3 border-b border-[var(--border)] px-4">
          <BrandMark className="size-9" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold">Pharmacy POS</p>
            <p className="truncate text-xs text-[var(--text-muted)]">
              {session.user.activeBranch.name}
            </p>
          </div>
          <button
            type="button"
            aria-label="Close navigation"
            className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] lg:hidden"
            onClick={() => setNavigationOpen(false)}
          >
            <X aria-hidden="true" size={18} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label="Primary navigation">
          {(["Workspace", "Operations", "Management"] as const).map((section) => {
            const sectionItems = navigation.filter((item) => item.section === section);
            if (sectionItems.length === 0) {
              return null;
            }
            return (
              <div className="mb-5" key={section}>
                <p className="mb-1.5 px-3 text-xs font-semibold uppercase text-[var(--text-subtle)]">
                  {section}
                </p>
                <ul className="space-y-1">
                  {sectionItems.map(({ href, icon: Icon, label }) => {
                    const active =
                      pathname === href || pathname.startsWith(`${href}/`);
                    return (
                      <li key={href}>
                        <Link
                          href={href}
                          aria-current={active ? "page" : undefined}
                          className={cn(
                            "flex h-10 items-center gap-3 rounded-md px-3 text-sm font-medium transition-colors",
                            active
                              ? "bg-[var(--brand-soft)] text-[var(--brand-strong)]"
                              : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]",
                          )}
                          onClick={() => setNavigationOpen(false)}
                        >
                          <Icon aria-hidden="true" size={18} />
                          {label}
                        </Link>
                      </li>
                    );
                  })}
                </ul>
              </div>
            );
          })}
        </nav>

        <div className="border-t border-[var(--border)] p-4">
          <div className="flex items-center gap-2 text-xs text-[var(--text-muted)]">
            <span
              className="size-2 rounded-full bg-[var(--success)]"
              aria-hidden="true"
            />
            {DEMO_AUTH_ENABLED ? "Local preview" : "API connected"}
          </div>
        </div>
      </aside>

      <div className="min-h-screen print:min-h-0 print:pl-0 lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center border-b border-[var(--border)] bg-white px-4 print:hidden sm:px-6">
          <button
            type="button"
            aria-label="Open navigation"
            className="mr-3 flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] lg:hidden"
            onClick={() => setNavigationOpen(true)}
          >
            <Menu aria-hidden="true" size={20} />
          </button>

          <div className="hidden min-w-0 items-center gap-2 text-sm sm:flex">
            <Building2
              aria-hidden="true"
              className="text-[var(--text-muted)]"
              size={17}
            />
            {session.user.roles.includes("OWNER") && branches.length > 1 ? (
              <select
                aria-label="Active branch"
                title={
                  currentShift
                    ? "Close the current shift before switching branches"
                    : cartLines.length
                      ? "Clear the current cart before switching branches"
                      : "Active branch"
                }
                className="max-w-64 rounded-md border-0 bg-transparent py-1 pr-2 font-medium outline-none focus:ring-2 focus:ring-[var(--brand-ring)] disabled:opacity-60"
                value={session.user.activeBranch.id}
                disabled={
                  switchingBranch || Boolean(currentShift) || cartLines.length > 0
                }
                onChange={(event) => void handleBranchChange(event.target.value)}
              >
                {branches.map((branch) => (
                  <option key={branch.id} value={branch.id}>
                    {branch.branchName} ({branch.branchCode})
                  </option>
                ))}
              </select>
            ) : (
              <>
                <span className="truncate font-medium">
                  {session.user.activeBranch.name}
                </span>
                <span className="text-[var(--text-subtle)]">
                  {session.user.activeBranch.code}
                </span>
              </>
            )}
          </div>

          <div className="ml-auto flex items-center gap-3">
            <NotificationMenu branchId={session.user.activeBranch.id} />
            {shiftAccess ? (
              <Link
                href="/shifts/current"
                className={cn(
                  "hidden min-h-8 items-center gap-2 rounded-md px-2.5 text-xs font-medium sm:flex",
                  currentShift
                    ? "bg-[var(--success-soft)] text-[var(--success)]"
                    : "bg-[var(--warning-soft)] text-[var(--warning)]",
                )}
              >
                <span className="size-2 rounded-full bg-current" aria-hidden="true" />
                {currentShift ? "Shift open" : "Shift closed"}
              </Link>
            ) : null}
            {DEMO_AUTH_ENABLED ? (
              <span className="hidden rounded-full bg-[var(--surface-muted)] px-2.5 py-1 text-xs font-medium text-[var(--text-muted)] sm:inline-flex">
                Preview
              </span>
            ) : null}
            <details className="group relative">
              <summary className="flex h-10 cursor-pointer list-none items-center gap-2 rounded-md px-1.5 hover:bg-[var(--surface-muted)]">
                <span className="flex size-8 items-center justify-center rounded-md bg-[var(--brand-soft)] text-xs font-semibold text-[var(--brand-strong)]">
                  {initials}
                </span>
                <span className="hidden max-w-36 truncate text-sm font-medium md:inline">
                  {session.user.displayName}
                </span>
                <ChevronDown
                  aria-hidden="true"
                  className="hidden text-[var(--text-muted)] transition-transform group-open:rotate-180 md:block"
                  size={15}
                />
              </summary>
              <div className="absolute right-0 mt-2 w-60 rounded-md border border-[var(--border)] bg-white p-2 shadow-lg">
                <div className="border-b border-[var(--border)] px-2 py-2.5">
                  <p className="truncate text-sm font-semibold">
                    {session.user.displayName}
                  </p>
                  <p className="mt-0.5 truncate text-xs text-[var(--text-muted)]">
                    {session.user.roles.map(roleLabel).join(", ")}
                  </p>
                </div>
                <button
                  type="button"
                  className="mt-1 flex h-9 w-full items-center gap-2 rounded-md px-2 text-sm text-[var(--danger)] hover:bg-[var(--danger-soft)]"
                  onClick={() => void handleSignOut()}
                >
                  <LogOut aria-hidden="true" size={16} />
                  Sign out
                </button>
              </div>
            </details>
          </div>
        </header>

        {peripheralHealthAccess ? (
          <PeripheralHealthBar
            canConfigure={session.user.permissions.includes(PERMISSIONS.TERMINAL_READ)}
          />
        ) : null}

        {offline ? (
          <div
            role="status"
            className="mt-3 flex flex-wrap items-center gap-2 rounded-md border border-[var(--warning)]/40 bg-[var(--warning-soft)] px-3 py-2 text-sm text-[var(--warning)] print:hidden"
          >
            <AlertTriangle aria-hidden="true" size={16} />
            Working offline - the server is unreachable. Sales will fail until
            the connection returns.
          </div>
        ) : null}

        <main className="mx-auto w-full max-w-[1500px] px-4 py-6 print:max-w-none print:p-0 sm:px-6 sm:py-8">
          {branchError ? (
            <div
              role="alert"
              className="mb-5 flex items-center gap-3 rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)] print:hidden"
            >
              <AlertTriangle aria-hidden="true" size={17} />
              <span>{branchError}</span>
            </div>
          ) : null}
          {loadError ? (
            <div
              role="alert"
              className="mb-5 flex flex-wrap items-center gap-3 rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)] print:hidden"
            >
              <AlertTriangle aria-hidden="true" size={17} />
              <span className="min-w-0 flex-1">{loadError}</span>
              <button
                type="button"
                className="inline-flex h-8 items-center gap-2 rounded-md px-2 font-medium hover:bg-white/60 disabled:opacity-60"
                disabled={retrying}
                onClick={() => {
                  setRetrying(true);
                  void workspaceGateway
                    .hydrate()
                    .catch(() => undefined)
                    .finally(() => setRetrying(false));
                }}
              >
                <RefreshCw aria-hidden="true" className={retrying ? "animate-spin" : ""} size={15} />
                Retry
              </button>
            </div>
          ) : null}
          {routeAllowed ? (
            children
          ) : (
            <AccessRestricted
              homePath={(() => {
                const home = homePathForPermissions(session.user.permissions);
                return canAccessPath(home, session.user.permissions) ? home : undefined;
              })()}
            />
          )}
        </main>
      </div>
    </div>
  );
}
