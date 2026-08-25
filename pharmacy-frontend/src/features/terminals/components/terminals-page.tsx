"use client";

import {
  Activity,
  Ban,
  Check,
  CircleDot,
  Computer,
  Cpu,
  HardDrive,
  KeyRound,
  Laptop,
  Link2,
  MonitorSmartphone,
  Pencil,
  Plus,
  RefreshCw,
  Settings2,
  ShieldBan,
  UserCheck,
  Wifi,
  WifiOff,
  X,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  PrimaryButton,
  SecondaryButton,
  SecondaryLink,
} from "@/components/ui/buttons";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  type BranchSummary,
  type Terminal,
  type TerminalInput,
  type TerminalType,
  getLocalTerminalId,
  isTerminalOnline,
  setLocalTerminalId,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";
import {
  useWorkspaceQuery,
} from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";

const terminalTypes: Array<{ label: string; value: TerminalType }> = [
  { label: "Web browser", value: "WEB" },
  { label: "Windows workstation", value: "WINDOWS" },
  { label: "Android handheld", value: "ANDROID_HANDHELD" },
  { label: "Android tablet", value: "ANDROID_TABLET" },
  { label: "iOS device", value: "IOS" },
  { label: "API integration", value: "API" },
];

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function emptyDraft(branchId: string): TerminalInput {
  return {
    branchId,
    firmwareVersion: null,
    manufacturer: null,
    model: null,
    name: "",
    osVersion: typeof navigator === "undefined" ? null : navigator.platform,
    platform: "Browser",
    serialNumber: null,
    terminalType: "WEB",
  };
}

function terminalStatusTone(terminal: Terminal) {
  if (terminal.status === "BLOCKED") return "danger" as const;
  if (terminal.status === "PENDING") return "warning" as const;
  if (terminal.status === "DEACTIVATED") return "neutral" as const;
  return isTerminalOnline(terminal) ? ("success" as const) : ("info" as const);
}

function terminalStatusLabel(terminal: Terminal) {
  if (terminal.status === "ACTIVE") {
    return isTerminalOnline(terminal) ? "Online" : "Offline";
  }
  return terminal.status.charAt(0) + terminal.status.slice(1).toLowerCase();
}

function detectDevice() {
  if (typeof navigator === "undefined") {
    return { browser: "Unknown", os: "Unknown", screen: "", kind: "Desktop" };
  }
  const ua = navigator.userAgent;
  const browser = /Edg\//.test(ua)
    ? "Edge"
    : /OPR\//.test(ua)
      ? "Opera"
      : /Chrome\//.test(ua)
        ? "Chrome"
        : /Firefox\//.test(ua)
          ? "Firefox"
          : /Safari\//.test(ua)
            ? "Safari"
            : "Browser";
  const os = /Windows NT 10/.test(ua)
    ? "Windows 10/11"
    : /Windows/.test(ua)
      ? "Windows"
      : /Android/.test(ua)
        ? "Android"
        : /iPhone|iPad|iPod/.test(ua)
          ? "iOS"
          : /Mac OS X/.test(ua)
            ? "macOS"
            : /Linux/.test(ua)
              ? "Linux"
              : "Unknown OS";
  const kind = /Mobi|Android|iPhone/.test(ua) ? "Mobile" : "Desktop";
  const screen =
    typeof window !== "undefined" && window.screen
      ? `${window.screen.width}x${window.screen.height}`
      : "";
  return { browser, os, screen, kind };
}

export function TerminalsPage() {
  const session = useAuthStore((state) => state.session);
  const canRead = usePermission(PERMISSIONS.TERMINAL_READ);
  const canManage = usePermission(PERMISSIONS.TERMINAL_MANAGE);
  const canLoadBranches = usePermission(PERMISSIONS.SETTINGS_MANAGE);
  const [branches, setBranches] = useState<BranchSummary[]>([]);
  const [terminals, setTerminals] = useState<Terminal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [branchFilter, setBranchFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [wizardOpen, setWizardOpen] = useState(false);
  const [wizardStep, setWizardStep] = useState(1);
  const [editing, setEditing] = useState<Terminal | null>(null);
  const [draft, setDraft] = useState<TerminalInput>(() => emptyDraft(""));
  const [assignLocally, setAssignLocally] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<Terminal | null>(null);
  const [blockTarget, setBlockTarget] = useState<Terminal | null>(null);
  const [regenerateTarget, setRegenerateTarget] = useState<Terminal | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [localTerminalId, setLocalTerminal] = useState<string | null>(null);
  const [registered, setRegistered] = useState<Terminal | null>(null);
  const [registeredApproved, setRegisteredApproved] = useState(false);
  const staff = useWorkspaceQuery((state) => state.staff);
  const [pairingTarget, setPairingTarget] = useState<Terminal | null>(null);
  const [pairingCode, setPairingCode] = useState<string | null>(null);
  const [pairingExpiresAt, setPairingExpiresAt] = useState<string | null>(null);
  const device = useMemo(() => detectDevice(), []);

  const fallbackBranch = useMemo<BranchSummary | null>(
    () =>
      session
        ? {
            branchCode: session.user.activeBranch.code,
            branchName: session.user.activeBranch.name,
            id: session.user.activeBranch.id,
            status: "ACTIVE",
          }
        : null,
    [session],
  );

  const loadTerminals = useCallback(async () => {
    await Promise.resolve();
    setLoading(true);
    try {
      setTerminals(await terminalGateway.listTerminals());
      setLocalTerminal(getLocalTerminalId());
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Terminals could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!canRead || !session || !fallbackBranch) return;
    let active = true;
    const branchRequest = canLoadBranches
      ? terminalGateway.listBranches(session.user.pharmacyId)
      : Promise.resolve([fallbackBranch]);
    void Promise.all([branchRequest, terminalGateway.listTerminals()])
      .then(([branchRows, terminalRows]) => {
        if (!active) return;
        setBranches(branchRows);
        setTerminals(terminalRows);
        setLocalTerminal(getLocalTerminalId());
        setError(null);
      })
      .catch((caught) => {
        if (!active) return;
        setBranches([fallbackBranch]);
        setError(errorMessage(caught, "Terminal administration could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canLoadBranches, canRead, fallbackBranch, session]);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return terminals.filter(
      (terminal) =>
        (branchFilter === "ALL" || terminal.branchId === branchFilter) &&
        (statusFilter === "ALL" || terminal.status === statusFilter) &&
        (!normalized ||
          [
            terminal.name,
            terminal.terminalId,
            terminal.manufacturer,
            terminal.model,
            terminal.serialNumber,
          ].some((value) => value?.toLowerCase().includes(normalized))),
    );
  }, [branchFilter, query, statusFilter, terminals]);
  const onlineCount = terminals.filter(isTerminalOnline).length;
  const pendingCount = terminals.filter(
    (terminal) => terminal.status === "PENDING",
  ).length;
  const outdatedCount = terminals.filter(
    (terminal) =>
      terminal.appVersion &&
      terminal.minimumBackendVersion &&
      terminal.appVersion.localeCompare(terminal.minimumBackendVersion, undefined, {
        numeric: true,
      }) < 0,
  ).length;

  function openCreate() {
    setEditing(null);
    setRegistered(null);
    setDraft(emptyDraft(session?.user.activeBranch.id ?? ""));
    setAssignLocally(true);
    setWizardStep(1);
    setError(null);
    setWizardOpen(true);
  }

  function openEdit(terminal: Terminal) {
    setEditing(terminal);
    setDraft({
      branchId: terminal.branchId,
      firmwareVersion: terminal.firmwareVersion,
      manufacturer: terminal.manufacturer,
      model: terminal.model,
      name: terminal.name,
      osVersion: terminal.osVersion,
      platform: terminal.platform,
      serialNumber: terminal.serialNumber,
      terminalType: terminal.terminalType,
    });
    setAssignLocally(localTerminalId === terminal.terminalId);
    setWizardStep(1);
    setError(null);
    setWizardOpen(true);
  }

  function updateDraft<K extends keyof TerminalInput>(
    key: K,
    value: TerminalInput[K],
  ) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function saveTerminal() {
    if (!draft.name.trim() || !draft.branchId) {
      setError("Enter a terminal name and choose its branch.");
      setWizardStep(1);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const normalized = {
        ...draft,
        firmwareVersion: draft.firmwareVersion?.trim() || null,
        manufacturer: draft.manufacturer?.trim() || null,
        model: draft.model?.trim() || null,
        name: draft.name.trim(),
        osVersion: draft.osVersion?.trim() || null,
        platform: draft.platform?.trim() || null,
        serialNumber: draft.serialNumber?.trim() || null,
      };
      const saved = editing
        ? await terminalGateway.updateTerminal(editing.terminalId, normalized)
        : await terminalGateway.registerTerminal(normalized);
      let approved = Boolean(editing) && saved.status === "ACTIVE";
      if (!editing && saved.status === "PENDING") {
        try {
          await terminalGateway.approveTerminal(saved.terminalId);
          approved = true;
        } catch {
          approved = false;
        }
      }
      if (assignLocally) {
        setLocalTerminalId(saved.terminalId);
        setLocalTerminal(saved.terminalId);
      } else if (localTerminalId === saved.terminalId) {
        setLocalTerminalId(null);
        setLocalTerminal(null);
      }
      await loadTerminals();
      if (editing) {
        setWizardOpen(false);
      } else {
        setRegistered({ ...saved, status: approved ? "ACTIVE" : saved.status });
        setRegisteredApproved(approved);
        setWizardStep(4);
      }
    } catch (caught) {
      setError(errorMessage(caught, "The terminal could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  async function approve(terminal: Terminal) {
    if (busyId) return;
    setBusyId(terminal.id);
    try {
      await terminalGateway.approveTerminal(terminal.terminalId);
      await loadTerminals();
    } catch (caught) {
      setError(errorMessage(caught, "The terminal could not be approved."));
    } finally {
      setBusyId(null);
    }
  }

  async function deactivate() {
    if (!deactivateTarget || busyId) return;
    setBusyId(deactivateTarget.id);
    try {
      await terminalGateway.deactivateTerminal(deactivateTarget.terminalId);
      if (localTerminalId === deactivateTarget.terminalId) {
        setLocalTerminalId(null);
        setLocalTerminal(null);
      }
      setDeactivateTarget(null);
      await loadTerminals();
    } catch (caught) {
      setDeactivateTarget(null);
      setError(errorMessage(caught, "The terminal could not be deactivated."));
    } finally {
      setBusyId(null);
    }
  }

  async function block() {
    if (!blockTarget || busyId) return;
    setBusyId(blockTarget.id);
    setError(null);
    setNotice(null);
    try {
      await terminalGateway.blockTerminal(blockTarget.terminalId);
      if (localTerminalId === blockTarget.terminalId) {
        setLocalTerminalId(null);
        setLocalTerminal(null);
      }
      setNotice(`${blockTarget.name} has been blocked.`);
      setBlockTarget(null);
      await loadTerminals();
    } catch (caught) {
      setBlockTarget(null);
      setError(errorMessage(caught, "The terminal could not be blocked."));
    } finally {
      setBusyId(null);
    }
  }

  async function regenerateApiKey() {
    if (!regenerateTarget || busyId) return;
    setBusyId(regenerateTarget.id);
    setError(null);
    setNotice(null);
    try {
      await terminalGateway.regenerateApiKey(regenerateTarget.terminalId);
      setNotice(
        `API credentials were regenerated for ${regenerateTarget.name}.`,
      );
      setRegenerateTarget(null);
      await loadTerminals();
    } catch (caught) {
      setRegenerateTarget(null);
      setError(
        errorMessage(caught, "The terminal API credentials could not be regenerated."),
      );
    } finally {
      setBusyId(null);
    }
  }

  function assignThisComputer(terminal: Terminal) {
    const next = localTerminalId === terminal.terminalId ? null : terminal.terminalId;
    setLocalTerminalId(next);
    setLocalTerminal(next);
    if (next && terminal.status === "ACTIVE") {
      void terminalGateway.heartbeat(next).then(loadTerminals).catch(() => undefined);
    }
  }

  async function startPairingFor(terminal: Terminal) {
    if (busyId) return;
    setBusyId(terminal.id);
    setError(null);
    try {
      const result = await terminalGateway.startPairing(terminal.terminalId);
      setPairingTarget(terminal);
      setPairingCode(result.code);
      setPairingExpiresAt(result.expiresAt);
    } catch (caught) {
      setError(errorMessage(caught, "A pairing code could not be generated."));
    } finally {
      setBusyId(null);
    }
  }

  async function assignStaffMember(terminal: Terminal, userId: string | null) {
    if (busyId) return;
    setBusyId(terminal.id);
    try {
      await terminalGateway.assignTerminalUser(terminal.terminalId, userId);
      await loadTerminals();
    } catch (caught) {
      setError(errorMessage(caught, "The terminal assignment could not be saved."));
    } finally {
      setBusyId(null);
    }
  }

  if (!canRead) return <AccessRestricted />;

  return (
    <div>
      <PageHeader
        title="Terminals"
        description="Register pharmacy workstations, assign branches, monitor connectivity, and configure attached hardware."
        actions={
          <>
            <SecondaryButton
              type="button"
              title="Refresh terminals"
              aria-label="Refresh terminals"
              className="px-3"
              disabled={loading}
              onClick={() => void loadTerminals()}
            >
              <RefreshCw aria-hidden="true" size={17} />
            </SecondaryButton>
            {canManage ? (
              <PrimaryButton type="button" onClick={openCreate}>
                <Plus aria-hidden="true" size={17} /> Add terminal
              </PrimaryButton>
            ) : null}
          </>
        }
      />

      <section className="mb-5 grid overflow-hidden rounded-md border border-[var(--border)] bg-white sm:grid-cols-3">
        {[
          { icon: Wifi, label: "Online", value: onlineCount, filter: null as string | null },
          {
            icon: CircleDot,
            label: "Awaiting approval",
            value: pendingCount,
            filter: pendingCount ? "PENDING" : null,
          },
          { icon: Activity, label: "Updates required", value: outdatedCount, filter: null as string | null },
        ].map(({ icon: Icon, label, value, filter }, index) => {
          const clickable = Boolean(filter);
          const content = (
            <>
              <span className="flex size-9 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]">
                <Icon aria-hidden="true" size={17} />
              </span>
              <div>
                <p className="text-xl font-semibold">{value}</p>
                <p className="text-xs text-[var(--text-muted)]">
                  {clickable ? `${label} - click to review` : label}
                </p>
              </div>
            </>
          );
          return clickable && canManage ? (
            <button
              key={label}
              type="button"
              onClick={() => {
                setQuery("");
                setStatusFilter(filter as string);
              }}
              className={`flex items-center gap-3 px-4 py-4 text-left hover:bg-[var(--surface-muted)] ${index ? "border-t border-[var(--border)] sm:border-l sm:border-t-0" : ""}`}
            >
              {content}
            </button>
          ) : (
            <div
              key={label}
              className={`flex items-center gap-3 px-4 py-4 ${index ? "border-t border-[var(--border)] sm:border-l sm:border-t-0" : ""}`}
            >
              {content}
            </div>
          );
        })}
      </section>

      <div className="mb-4 grid gap-3 md:grid-cols-[minmax(240px,1fr)_220px_200px]">
        <Input
          placeholder="Search terminal, ID, model, or serial"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <Select
          value={branchFilter}
          onChange={(event) => setBranchFilter(event.target.value)}
        >
          <option value="ALL">All branches</option>
          {branches.map((branch) => (
            <option key={branch.id} value={branch.id}>
              {branch.branchName} ({branch.branchCode})
            </option>
          ))}
        </Select>
        <Select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="ALL">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="PENDING">Pending</option>
          <option value="DEACTIVATED">Deactivated</option>
          <option value="BLOCKED">Blocked</option>
        </Select>
      </div>

      {error ? (
        <div className="mb-4">
          <FormError message={error} />
        </div>
      ) : null}
      {notice ? (
        <div
          role="status"
          className="mb-4 rounded-md border border-[var(--success)]/30 bg-[var(--success-soft)] px-4 py-3 text-sm text-[var(--success)]"
        >
          {notice}
        </div>
      ) : null}

      <section className="overflow-hidden rounded-md border border-[var(--border)] bg-white">
        {filtered.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1050px] text-left text-sm">
              <thead className="border-b border-[var(--border)] bg-[var(--surface-muted)] text-xs text-[var(--text-muted)]">
                <tr>
                  <th className="px-4 py-3 font-semibold">Terminal</th>
                  <th className="px-4 py-3 font-semibold">Branch</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                  <th className="px-4 py-3 font-semibold">Last seen</th>
                  <th className="px-4 py-3 font-semibold">Version</th>
                  <th className="px-4 py-3 text-right font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {filtered.map((terminal) => {
                  const branch = branches.find((item) => item.id === terminal.branchId);
                  const local = localTerminalId === terminal.terminalId;
                  return (
                    <tr key={terminal.id} className="hover:bg-[var(--surface-muted)]/60">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <span className="flex size-9 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]">
                            <Computer aria-hidden="true" size={17} />
                          </span>
                          <div className="min-w-0">
                            <div className="flex items-center gap-2">
                              <p className="font-semibold">{terminal.name}</p>
                              {local ? <StatusBadge tone="info">This PC</StatusBadge> : null}
                            </div>
                            <p className="mt-0.5 font-mono text-xs text-[var(--text-muted)]">
                              {terminal.terminalId} - {terminal.terminalType.replaceAll("_", " ")}
                            </p>
                            {canManage && staff.length ? (
                              <Select
                                aria-label={`Staff responsible for ${terminal.name}`}
                                className="mt-1 h-8 w-full max-w-56 text-xs"
                                disabled={busyId === terminal.id}
                                value={terminal.assignedUserId ?? ""}
                                onChange={(event) =>
                                  void assignStaffMember(
                                    terminal,
                                    event.target.value || null,
                                  )
                                }
                              >
                                <option value="">Unassigned staff</option>
                                {staff.map((user) => (
                                  <option key={user.id} value={user.id}>
                                    {user.displayName}
                                  </option>
                                ))}
                              </Select>
                            ) : !canManage && terminal.assignedUserName ? (
                              <p className="mt-0.5 flex items-center gap-1 text-xs text-[var(--text-muted)]">
                                <UserCheck aria-hidden="true" size={12} />
                                {terminal.assignedUserName}
                              </p>
                            ) : null}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-medium">
                          {terminal.branchName || branch?.branchName || "Unknown branch"}
                        </p>
                        <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                          {branch?.branchCode || terminal.branchId.slice(0, 8)}
                        </p>
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge tone={terminalStatusTone(terminal)}>
                          {terminal.status === "ACTIVE" && isTerminalOnline(terminal) ? (
                            <Wifi aria-hidden="true" className="mr-1" size={13} />
                          ) : terminal.status === "ACTIVE" ? (
                            <WifiOff aria-hidden="true" className="mr-1" size={13} />
                          ) : null}
                          {terminalStatusLabel(terminal)}
                        </StatusBadge>
                      </td>
                      <td className="px-4 py-3 text-xs text-[var(--text-muted)]">
                        {terminal.lastSeenAt
                          ? formatDateTime(terminal.lastSeenAt)
                          : "Never"}
                      </td>
                      <td className="px-4 py-3 text-xs">
                        <p className="font-medium">{terminal.appVersion || "Not reported"}</p>
                        <p className="mt-0.5 text-[var(--text-muted)]">
                          Minimum {terminal.minimumBackendVersion || "not set"}
                        </p>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-1">
                          <button
                            type="button"
                            title={local ? "Remove this PC assignment" : "Use on this PC"}
                            aria-label={local ? "Remove this PC assignment" : "Use on this PC"}
                            disabled={terminal.status !== "ACTIVE"}
                            onClick={() => assignThisComputer(terminal)}
                            className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)] disabled:opacity-30"
                          >
                            <Laptop aria-hidden="true" size={16} />
                          </button>
                          <SecondaryLink
                            href={`/admin/terminals/${terminal.terminalId}/hardware`}
                            title={`Configure ${terminal.name} hardware`}
                            aria-label={`Configure ${terminal.name} hardware`}
                            className="min-h-9 px-2.5"
                          >
                            <Settings2 aria-hidden="true" size={16} />
                          </SecondaryLink>
                          {canManage &&
                          (terminal.status === "PENDING" ||
                            terminal.status === "ACTIVE") ? (
                            <button
                              type="button"
                              title="Pair another device to this terminal"
                              aria-label={`Pair another device to ${terminal.name}`}
                              disabled={busyId === terminal.id}
                              onClick={() => void startPairingFor(terminal)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)] disabled:opacity-40"
                            >
                              <Link2 aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                          {canManage ? (
                            <button
                              type="button"
                              title={`Edit ${terminal.name}`}
                              aria-label={`Edit ${terminal.name}`}
                              onClick={() => openEdit(terminal)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)]"
                            >
                              <Pencil aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                          {canManage && terminal.status === "PENDING" ? (
                            <SecondaryButton
                              type="button"
                              title={`Approve ${terminal.name}`}
                              aria-label={`Approve ${terminal.name}`}
                              className="min-h-9 gap-1.5 px-3 text-[var(--success)]"
                              disabled={busyId === terminal.id}
                              onClick={() => void approve(terminal)}
                            >
                              <Check aria-hidden="true" size={15} /> Approve
                            </SecondaryButton>
                          ) : null}
                          {canManage && terminal.status === "ACTIVE" ? (
                            <button
                              type="button"
                              title={`Regenerate API credentials for ${terminal.name}`}
                              aria-label={`Regenerate API credentials for ${terminal.name}`}
                              disabled={busyId === terminal.id}
                              onClick={() => setRegenerateTarget(terminal)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-white hover:text-[var(--text)] disabled:opacity-40"
                            >
                              <KeyRound aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                          {canManage &&
                          (terminal.status === "ACTIVE" ||
                            terminal.status === "PENDING") ? (
                            <button
                              type="button"
                              title={`Block ${terminal.name}`}
                              aria-label={`Block ${terminal.name}`}
                              disabled={busyId === terminal.id}
                              onClick={() => setBlockTarget(terminal)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)] disabled:opacity-40"
                            >
                              <ShieldBan aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                          {canManage && terminal.status === "ACTIVE" ? (
                            <button
                              type="button"
                              title={`Deactivate ${terminal.name}`}
                              aria-label={`Deactivate ${terminal.name}`}
                              onClick={() => setDeactivateTarget(terminal)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)]"
                            >
                              <Ban aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : loading ? (
          <div className="p-8 text-center text-sm text-[var(--text-muted)]">
            Loading terminals...
          </div>
        ) : (
          <EmptyState
            icon={Cpu}
            title={terminals.length ? "No matching terminals" : "No terminals registered"}
            description={
              terminals.length
                ? "Adjust the branch, status, or search filters."
                : "Register the first pharmacy workstation."
            }
            action={
              !terminals.length && canManage ? (
                <PrimaryButton type="button" onClick={openCreate}>
                  <Plus aria-hidden="true" size={17} /> Register terminal
                </PrimaryButton>
              ) : undefined
            }
          />
        )}
      </section>

      {wizardOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label={editing ? "Edit terminal" : "Terminal onboarding"}
            className="w-full max-w-2xl rounded-md border border-[var(--border)] bg-white shadow-xl"
          >
            <div className="flex items-center justify-between border-b border-[var(--border)] px-5 py-4">
              <div>
                <h2 className="text-base font-semibold">
                  {editing ? "Edit terminal" : "Terminal onboarding"}
                </h2>
                <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                  {wizardStep === 4 ? "Setup complete" : `Step ${wizardStep} of 3`}
                </p>
              </div>
              <button
                type="button"
                title="Close"
                aria-label="Close terminal onboarding"
                onClick={() => setWizardOpen(false)}
                className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
              >
                <X aria-hidden="true" size={18} />
              </button>
            </div>

            <div className="p-5">
              {wizardStep === 4 && registered ? (
                <div>
                  <p className="text-sm text-[var(--text-muted)]">
                    {registered.name} is ready to use.
                  </p>
                  <ul className="mt-4 space-y-2 text-sm">
                    {[
                      { label: "Registered", done: true },
                      {
                        label: registeredApproved
                          ? "Approved"
                          : "Approval pending - a manager must approve it from the terminals list",
                        done: registeredApproved,
                      },
                      {
                        label: assignLocally
                          ? "Assigned to this computer"
                          : "Not assigned to this computer",
                        done: assignLocally,
                      },
                    ].map((item) => (
                      <li key={item.label} className="flex items-center gap-2">
                        <Check
                          aria-hidden="true"
                          size={16}
                          className={item.done ? "text-[var(--success)]" : "text-[var(--warning)]"}
                        />
                        {item.label}
                      </li>
                    ))}
                  </ul>
                  <div className="mt-4 rounded-md bg-[var(--surface-muted)] p-3">
                    <p className="text-xs text-[var(--text-muted)]">Terminal ID</p>
                    <p className="mt-1 font-mono text-base font-semibold">{registered.terminalId}</p>
                  </div>
                </div>
              ) : null}

              {wizardStep === 1 ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="rounded-md border border-[var(--border)] bg-[var(--surface-muted)] p-3 text-xs sm:col-span-2">
                    <p className="flex items-center gap-1.5 font-semibold">
                      <MonitorSmartphone aria-hidden="true" size={14} className="text-[var(--brand)]" />
                      Detected on this device
                    </p>
                    <p className="mt-1 text-[var(--text-muted)]">
                      {device.kind} - {device.os} - {device.browser}
                      {device.screen ? ` - ${device.screen}` : ""}
                    </p>
                  </div>
                  <Field label="Terminal name" required>
                    <Input
                      autoFocus
                      placeholder="e.g. Front counter"
                      value={draft.name}
                      onChange={(event) => updateDraft("name", event.target.value)}
                    />
                  </Field>
                  <Field label="Terminal type" required>
                    <Select
                      value={draft.terminalType}
                      onChange={(event) =>
                        updateDraft("terminalType", event.target.value as TerminalType)
                      }
                    >
                      {terminalTypes.map((type) => (
                        <option key={type.value} value={type.value}>
                          {type.label}
                        </option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Platform">
                    <Input
                      value={draft.platform ?? ""}
                      onChange={(event) => updateDraft("platform", event.target.value)}
                    />
                  </Field>
                  <Field label="OS version">
                    <Input
                      value={draft.osVersion ?? ""}
                      onChange={(event) => updateDraft("osVersion", event.target.value)}
                    />
                  </Field>
                </div>
              ) : null}

              {wizardStep === 2 ? (
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="Assigned branch" required>
                    <Select
                      value={draft.branchId}
                      onChange={(event) => updateDraft("branchId", event.target.value)}
                    >
                      <option value="">Choose branch</option>
                      {branches.map((branch) => (
                        <option key={branch.id} value={branch.id}>
                          {branch.branchName} ({branch.branchCode})
                        </option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Serial number">
                    <Input
                      value={draft.serialNumber ?? ""}
                      onChange={(event) =>
                        updateDraft("serialNumber", event.target.value)
                      }
                    />
                  </Field>
                  <Field label="Manufacturer">
                    <Input
                      value={draft.manufacturer ?? ""}
                      onChange={(event) =>
                        updateDraft("manufacturer", event.target.value)
                      }
                    />
                  </Field>
                  <Field label="Model">
                    <Input
                      value={draft.model ?? ""}
                      onChange={(event) => updateDraft("model", event.target.value)}
                    />
                  </Field>
                </div>
              ) : null}

              {wizardStep === 3 ? (
                <div>
                  <div className="grid gap-3 rounded-md border border-[var(--border)] p-4 text-sm sm:grid-cols-2">
                    <div>
                      <p className="text-xs text-[var(--text-muted)]">Terminal</p>
                      <p className="mt-1 font-semibold">{draft.name || "Unnamed"}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[var(--text-muted)]">Type</p>
                      <p className="mt-1 font-semibold">
                        {draft.terminalType.replaceAll("_", " ")}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-[var(--text-muted)]">Branch</p>
                      <p className="mt-1 font-semibold">
                        {branches.find((branch) => branch.id === draft.branchId)
                          ?.branchName || "Not selected"}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-[var(--text-muted)]">Hardware</p>
                      <p className="mt-1 font-semibold">
                        {[draft.manufacturer, draft.model].filter(Boolean).join(" ") ||
                          "Not recorded"}
                      </p>
                    </div>
                  </div>
                  <label className="mt-4 flex items-start gap-3 rounded-md bg-[var(--surface-muted)] p-3 text-sm">
                    <input
                      type="checkbox"
                      className="mt-0.5 size-4 accent-[var(--brand)]"
                      checked={assignLocally}
                      onChange={(event) => setAssignLocally(event.target.checked)}
                    />
                    <span>
                      <span className="block font-semibold">Use on this computer</span>
                      <span className="mt-0.5 block text-xs text-[var(--text-muted)]">
                        This browser will send node heartbeats after the terminal is approved.
                      </span>
                    </span>
                  </label>
                </div>
              ) : null}

              <div className="mt-4">
                <FormError message={error} />
              </div>
            </div>

            <div className="flex items-center justify-between border-t border-[var(--border)] px-5 py-4">
              {wizardStep === 4 ? (
                <span />
              ) : (
                <SecondaryButton
                  type="button"
                  disabled={wizardStep === 1 || saving}
                  onClick={() => setWizardStep((step) => step - 1)}
                >
                  Back
                </SecondaryButton>
              )}
              {wizardStep < 3 ? (
                <PrimaryButton
                  type="button"
                  onClick={() => {
                    if (wizardStep === 1 && !draft.name.trim()) {
                      setError("Enter a terminal name.");
                      return;
                    }
                    setError(null);
                    setWizardStep((step) => step + 1);
                  }}
                >
                  Continue
                </PrimaryButton>
              ) : wizardStep === 3 ? (
                <PrimaryButton
                  type="button"
                  disabled={saving}
                  onClick={() => void saveTerminal()}
                >
                  <HardDrive aria-hidden="true" size={17} />
                  {saving ? "Saving..." : editing ? "Save terminal" : "Register terminal"}
                </PrimaryButton>
              ) : (
                <PrimaryButton
                  type="button"
                  onClick={() => setWizardOpen(false)}
                >
                  Done
                </PrimaryButton>
              )}
            </div>
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={Boolean(deactivateTarget)}
        busy={Boolean(deactivateTarget && busyId === deactivateTarget.id)}
        busyLabel="Deactivating..."
        title="Deactivate terminal?"
        description={`${deactivateTarget?.name ?? "This terminal"} will stop reporting as an active pharmacy node. Its history and hardware records are retained.`}
        confirmLabel="Deactivate terminal"
        onCancel={() => setDeactivateTarget(null)}
        onConfirm={() => void deactivate()}
      />
      {pairingTarget && pairingCode ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4">
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Pair another device"
            className="w-full max-w-md rounded-md border border-[var(--border)] bg-white p-5 shadow-xl"
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="flex items-center gap-2 text-base font-semibold">
                  <MonitorSmartphone aria-hidden="true" size={18} className="text-[var(--brand)]" />
                  Pair another device
                </h2>
                <p className="mt-1 text-xs text-[var(--text-muted)]">
                  On the other device, sign in, open the Point of sale, and enter
                  this code when prompted.
                </p>
              </div>
              <button
                type="button"
                title="Close"
                aria-label="Close pairing dialog"
                onClick={() => {
                  setPairingTarget(null);
                  setPairingCode(null);
                }}
                className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
              >
                <X aria-hidden="true" size={18} />
              </button>
            </div>
            <p className="mt-5 text-center font-mono text-4xl font-bold tracking-[0.35em]">
              {pairingCode}
            </p>
            <p className="mt-3 text-center text-xs text-[var(--text-muted)]">
              One-time code for {pairingTarget.name} ({pairingTarget.terminalId})
              {pairingExpiresAt ? ` - expires ${formatDateTime(pairingExpiresAt)}` : ""}
            </p>
            <div className="mt-5 flex justify-end">
              <PrimaryButton
                type="button"
                onClick={() => navigator.clipboard?.writeText(pairingCode).catch(() => undefined)}
              >
                Copy code
              </PrimaryButton>
            </div>
          </div>
        </div>
      ) : null}
      <ConfirmDialog
        open={Boolean(blockTarget)}
        busy={Boolean(blockTarget && busyId === blockTarget.id)}
        busyLabel="Blocking..."
        title="Block terminal?"
        description={`${blockTarget?.name ?? "This terminal"} will immediately lose terminal access and stop reporting heartbeats. Its history and hardware records are retained.`}
        confirmLabel="Block terminal"
        onCancel={() => setBlockTarget(null)}
        onConfirm={() => void block()}
      />
      <ConfirmDialog
        open={Boolean(regenerateTarget)}
        busy={Boolean(regenerateTarget && busyId === regenerateTarget.id)}
        busyLabel="Regenerating..."
        title="Regenerate API credentials?"
        description={`Existing API credentials for ${regenerateTarget?.name ?? "this terminal"} will stop working immediately. The terminal must authenticate again before it can reconnect.`}
        confirmLabel="Regenerate credentials"
        onCancel={() => setRegenerateTarget(null)}
        onConfirm={() => void regenerateApiKey()}
      />
    </div>
  );
}
