"use client";

import {
  ArrowLeft,
  Cable,
  CheckCircle2,
  Keyboard,
  PlugZap,
  Printer,
  RefreshCw,
  ScanBarcode,
  Server,
  Trash2,
  Usb,
} from "lucide-react";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

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
import {
  type ConnectionType,
  type HardwareBridgeConfig,
  type HardwarePeripheral,
  type PeripheralInput,
  type PeripheralType,
  type Terminal,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";
import { ApiClientError } from "@/lib/api-client";

const peripheralTypes: Array<{ label: string; value: PeripheralType }> = [
  { label: "Receipt printer", value: "PRINTER" },
  { label: "Barcode scanner", value: "SCANNER" },
  { label: "Cash drawer", value: "CASH_DRAWER" },
  { label: "Barcode printer", value: "BARCODE_PRINTER" },
  { label: "Customer display", value: "DISPLAY" },
  { label: "Scale", value: "SCALE" },
  { label: "Camera", value: "CAMERA" },
];

const connectionTypes: Array<{ label: string; value: ConnectionType }> = [
  { label: "USB", value: "USB" },
  { label: "Keyboard wedge", value: "WEDGE" },
  { label: "Network", value: "NETWORK" },
  { label: "Bluetooth", value: "BLUETOOTH" },
  { label: "Serial", value: "SERIAL" },
  { label: "Printer port", value: "PRINTER_PORT" },
];

const emptyPeripheral: PeripheralInput = {
  configuration: null,
  connectionType: "USB",
  manufacturer: null,
  model: null,
  type: "PRINTER",
};

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiClientError || error instanceof Error
    ? error.message
    : fallback;
}

function peripheralIcon(type: PeripheralType) {
  if (type === "PRINTER" || type === "BARCODE_PRINTER") return Printer;
  if (type === "SCANNER" || type === "CAMERA") return ScanBarcode;
  if (type === "CASH_DRAWER") return PlugZap;
  return Usb;
}

export function HardwareSetupPage() {
  const params = useParams<{ id: string }>();
  const canRead = usePermission(PERMISSIONS.TERMINAL_READ);
  const canManage = usePermission(PERMISSIONS.TERMINAL_MANAGE);
  const [terminal, setTerminal] = useState<Terminal | null>(null);
  const [bridgeConfig, setBridgeConfig] =
    useState<HardwareBridgeConfig | null>(null);
  const [bridgeState, setBridgeState] = useState<
    "checking" | "online" | "offline"
  >("checking");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [draft, setDraft] = useState<PeripheralInput>(emptyPeripheral);
  const [saving, setSaving] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<HardwarePeripheral | null>(null);
  const [scannerValue, setScannerValue] = useState("");
  const [lastScan, setLastScan] = useState<string | null>(null);
  const [printing, setPrinting] = useState(false);
  const [printTimestamp, setPrintTimestamp] = useState("");

  const loadTerminal = useCallback(async () => {
    await Promise.resolve();
    setLoading(true);
    try {
      setTerminal(await terminalGateway.getTerminal(params.id));
      setError(null);
    } catch (caught) {
      setError(errorMessage(caught, "Terminal hardware could not be loaded."));
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  const checkBridge = useCallback(async (config: HardwareBridgeConfig) => {
    setBridgeState("checking");
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 2500);
    try {
      const response = await fetch(`${config.connectorUrl}/health`, {
        cache: "no-store",
        signal: controller.signal,
      });
      setBridgeState(response.ok ? "online" : "offline");
    } catch {
      setBridgeState("offline");
    } finally {
      window.clearTimeout(timeout);
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    void Promise.all([
      terminalGateway.getTerminal(params.id),
      terminalGateway.getHardwareConfig(),
    ])
      .then(([terminalResult, config]) => {
        if (!active) return;
        setTerminal(terminalResult);
        setBridgeConfig(config);
        setError(null);
        void checkBridge(config);
      })
      .catch((caught) => {
        if (!active) return;
        setError(errorMessage(caught, "Terminal hardware could not be loaded."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canRead, checkBridge, params.id]);

  function updateDraft<K extends keyof PeripheralInput>(
    key: K,
    value: PeripheralInput[K],
  ) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function addPeripheral(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!terminal || !canManage) return;
    setSaving(true);
    setError(null);
    try {
      await terminalGateway.addPeripheral(terminal.terminalId, {
        ...draft,
        configuration: draft.configuration?.trim() || null,
        manufacturer: draft.manufacturer?.trim() || null,
        model: draft.model?.trim() || null,
      });
      setDraft(emptyPeripheral);
      setFormOpen(false);
      await loadTerminal();
    } catch (caught) {
      setError(errorMessage(caught, "The peripheral could not be added."));
    } finally {
      setSaving(false);
    }
  }

  async function removePeripheral() {
    if (!removeTarget || !canManage) return;
    setSaving(true);
    try {
      await terminalGateway.removePeripheral(removeTarget.id);
      setRemoveTarget(null);
      await loadTerminal();
    } catch (caught) {
      setRemoveTarget(null);
      setError(errorMessage(caught, "The peripheral could not be removed."));
    } finally {
      setSaving(false);
    }
  }

  function printTestReceipt() {
    setPrintTimestamp(new Intl.DateTimeFormat("en-KE", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Africa/Nairobi",
    }).format(new Date()));
    setPrinting(true);
    window.setTimeout(() => {
      window.print();
      setPrinting(false);
    }, 100);
  }

  if (!canRead) return <AccessRestricted />;

  if (loading) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6 text-sm text-[var(--text-muted)]">
        Loading hardware setup...
      </div>
    );
  }

  if (!terminal) {
    return (
      <div className="max-w-3xl">
        <PageHeader title="Hardware setup" />
        <FormError message={error ?? "Terminal not found."} />
        <SecondaryLink className="mt-4" href="/admin/terminals">
          <ArrowLeft aria-hidden="true" size={16} /> Back to terminals
        </SecondaryLink>
      </div>
    );
  }

  return (
    <div className="max-w-6xl">
      <PageHeader
        eyebrow={`${terminal.name} - ${terminal.terminalId}`}
        title="Hardware setup"
        description="Configure recorded peripherals and verify the scanner, receipt layout, and optional local connector."
        actions={
          <SecondaryLink href="/admin/terminals">
            <ArrowLeft aria-hidden="true" size={16} /> Terminals
          </SecondaryLink>
        }
      />

      {error ? (
        <div className="mb-5">
          <FormError message={error} />
        </div>
      ) : null}

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          <section className="rounded-md border border-[var(--border)] bg-white">
            <div className="flex items-center justify-between gap-3 border-b border-[var(--border)] px-4 py-3.5">
              <div>
                <h2 className="text-sm font-semibold">Attached peripherals</h2>
                <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                  {terminal.peripherals.length} configured
                </p>
              </div>
              {canManage ? (
                <SecondaryButton
                  type="button"
                  onClick={() => setFormOpen((open) => !open)}
                >
                  {formOpen ? (
                    <Cable aria-hidden="true" size={16} />
                  ) : (
                    <PlugZap aria-hidden="true" size={16} />
                  )}
                  {formOpen ? "Close" : "Add device"}
                </SecondaryButton>
              ) : null}
            </div>

            {formOpen ? (
              <form
                onSubmit={addPeripheral}
                className="border-b border-[var(--border)] bg-[var(--surface-muted)]/60 p-4"
              >
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="Device type" required>
                    <Select
                      value={draft.type}
                      onChange={(event) =>
                        updateDraft("type", event.target.value as PeripheralType)
                      }
                    >
                      {peripheralTypes.map((type) => (
                        <option key={type.value} value={type.value}>
                          {type.label}
                        </option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Connection" required>
                    <Select
                      value={draft.connectionType}
                      onChange={(event) =>
                        updateDraft(
                          "connectionType",
                          event.target.value as ConnectionType,
                        )
                      }
                    >
                      {connectionTypes.map((type) => (
                        <option key={type.value} value={type.value}>
                          {type.label}
                        </option>
                      ))}
                    </Select>
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
                  <Field label="Configuration">
                    <Input
                      placeholder="Port, address, or device profile"
                      value={draft.configuration ?? ""}
                      onChange={(event) =>
                        updateDraft("configuration", event.target.value)
                      }
                    />
                  </Field>
                </div>
                <div className="mt-4 flex justify-end">
                  <PrimaryButton type="submit" disabled={saving}>
                    <PlugZap aria-hidden="true" size={16} />
                    {saving ? "Adding..." : "Add device"}
                  </PrimaryButton>
                </div>
              </form>
            ) : null}

            {terminal.peripherals.length ? (
              <div className="divide-y divide-[var(--border)]">
                {terminal.peripherals.map((peripheral) => {
                  const Icon = peripheralIcon(peripheral.type);
                  return (
                    <div
                      key={peripheral.id}
                      className="grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 px-4 py-3"
                    >
                      <span className="flex size-9 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]">
                        <Icon aria-hidden="true" size={17} />
                      </span>
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="font-semibold">
                            {peripheral.type.replaceAll("_", " ")}
                          </p>
                          <StatusBadge
                            tone={
                              peripheral.status === "ONLINE"
                                ? "success"
                                : peripheral.status === "ERROR"
                                  ? "danger"
                                  : "neutral"
                            }
                          >
                            {peripheral.status.toLowerCase()}
                          </StatusBadge>
                        </div>
                        <p className="mt-1 truncate text-xs text-[var(--text-muted)]">
                          {[peripheral.manufacturer, peripheral.model]
                            .filter(Boolean)
                            .join(" ") || "Unspecified device"} - {peripheral.connectionType.replaceAll("_", " ")}
                        </p>
                      </div>
                      {canManage ? (
                        <button
                          type="button"
                          title="Remove peripheral"
                          aria-label={`Remove ${peripheral.type}`}
                          onClick={() => setRemoveTarget(peripheral)}
                          className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)]"
                        >
                          <Trash2 aria-hidden="true" size={16} />
                        </button>
                      ) : null}
                    </div>
                  );
                })}
              </div>
            ) : (
              <EmptyState
                icon={PlugZap}
                title="No peripherals configured"
                description="Add a printer, scanner, cash drawer, or other terminal device."
              />
            )}
          </section>

          <section className="grid gap-5 md:grid-cols-2">
            <div className="rounded-md border border-[var(--border)] bg-white p-4">
              <div className="flex items-center gap-2">
                <Keyboard aria-hidden="true" className="text-[var(--brand)]" size={18} />
                <h2 className="text-sm font-semibold">Scanner test</h2>
              </div>
              <label className="mt-4 block">
                <span className="sr-only">Scan barcode</span>
                <Input
                  placeholder="Focus here and scan"
                  value={scannerValue}
                  onChange={(event) => setScannerValue(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key !== "Enter" || !scannerValue.trim()) return;
                    event.preventDefault();
                    setLastScan(scannerValue.trim());
                    setScannerValue("");
                  }}
                />
              </label>
              <div className="mt-3 min-h-10 rounded-md bg-[var(--surface-muted)] px-3 py-2 text-sm">
                {lastScan ? (
                  <span className="flex items-center gap-2 text-[var(--success)]">
                    <CheckCircle2 aria-hidden="true" size={16} />
                    <span className="font-mono">{lastScan}</span>
                  </span>
                ) : (
                  <span className="text-[var(--text-muted)]">Waiting for scan</span>
                )}
              </div>
            </div>

            <div className="rounded-md border border-[var(--border)] bg-white p-4">
              <div className="flex items-center gap-2">
                <Printer aria-hidden="true" className="text-[var(--brand)]" size={18} />
                <h2 className="text-sm font-semibold">Receipt printer test</h2>
              </div>
              <div className="mt-4 border-y border-dashed border-[var(--border-strong)] py-4 text-center text-xs">
                <p className="font-bold">PHARMACY POS</p>
                <p className="mt-1 text-[var(--text-muted)]">{terminal.name}</p>
                <p className="mt-3 font-mono">PRINT TEST</p>
              </div>
              <SecondaryButton
                type="button"
                className="mt-4 w-full"
                onClick={printTestReceipt}
              >
                <Printer aria-hidden="true" size={16} /> Print test
              </SecondaryButton>
            </div>
          </section>
        </div>

        <aside className="space-y-5">
          <section className="rounded-md border border-[var(--border)] bg-white p-4">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Server aria-hidden="true" className="text-[var(--brand)]" size={18} />
                <h2 className="text-sm font-semibold">Hardware connector</h2>
              </div>
              <button
                type="button"
                title="Check connector"
                aria-label="Check hardware connector"
                disabled={!bridgeConfig || bridgeState === "checking"}
                onClick={() => bridgeConfig && void checkBridge(bridgeConfig)}
                className="flex size-8 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] disabled:opacity-40"
              >
                <RefreshCw aria-hidden="true" size={15} />
              </button>
            </div>
            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-[var(--text-muted)]">Connector service</span>
              <StatusBadge
                tone={
                  bridgeState === "online"
                    ? "success"
                    : bridgeState === "offline"
                      ? "danger"
                      : "neutral"
                }
              >
                {bridgeState === "checking"
                  ? "Checking"
                  : bridgeState === "online"
                    ? "Online"
                    : "Offline"}
              </StatusBadge>
            </div>
            <dl className="mt-4 space-y-3 border-t border-[var(--border)] pt-4 text-xs">
              <div>
                <dt className="text-[var(--text-muted)]">Address</dt>
                <dd className="mt-1 font-mono">
                  {bridgeConfig?.connectorUrl ?? "Not available"}
                </dd>
              </div>
              <div>
                <dt className="text-[var(--text-muted)]">Printer profile</dt>
                <dd className="mt-1 font-medium">
                  {bridgeConfig?.printerType ?? "Browser print"}
                </dd>
              </div>
              <div>
                <dt className="text-[var(--text-muted)]">Scanner mode</dt>
                <dd className="mt-1 font-medium">
                  {bridgeConfig?.scannerMode?.replaceAll("_", " ") ??
                    "Keyboard wedge"}
                </dd>
              </div>
            </dl>
          </section>

          <section className="rounded-md border border-[var(--border)] bg-white p-4">
            <h2 className="text-sm font-semibold">Terminal identity</h2>
            <dl className="mt-4 space-y-3 text-xs">
              <div className="flex justify-between gap-4">
                <dt className="text-[var(--text-muted)]">Type</dt>
                <dd className="font-medium">{terminal.terminalType.replaceAll("_", " ")}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-[var(--text-muted)]">Platform</dt>
                <dd className="text-right font-medium">
                  {terminal.platform || "Not recorded"}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-[var(--text-muted)]">Model</dt>
                <dd className="text-right font-medium">
                  {[terminal.manufacturer, terminal.model].filter(Boolean).join(" ") ||
                    "Not recorded"}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-[var(--text-muted)]">Serial</dt>
                <dd className="max-w-48 truncate text-right font-mono">
                  {terminal.serialNumber || "Not recorded"}
                </dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>

      {printing ? (
        <div className="device-test-print">
          <p className="text-center text-base font-bold">PHARMACY POS</p>
          <p className="mt-1 text-center">Printer test</p>
          <div className="my-4 border-t border-dashed border-black" />
          <p>Terminal: {terminal.name}</p>
          <p>ID: {terminal.terminalId}</p>
          <p>Time: {printTimestamp}</p>
          <div className="my-4 border-t border-dashed border-black" />
          <p className="text-center font-bold">PRINT OK</p>
        </div>
      ) : null}

      <ConfirmDialog
        open={Boolean(removeTarget)}
        busy={saving}
        busyLabel="Removing..."
        title="Remove peripheral?"
        description={`Remove ${removeTarget?.type.replaceAll("_", " ").toLowerCase() ?? "this device"} from ${terminal.name}.`}
        confirmLabel="Remove device"
        onCancel={() => setRemoveTarget(null)}
        onConfirm={() => void removePeripheral()}
      />
    </div>
  );
}
