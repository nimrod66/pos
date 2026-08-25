"use client";

import {
  ArrowLeft,
  Banknote,
  Cable,
  CheckCircle2,
  Keyboard,
  PlugZap,
  Printer,
  RefreshCw,
  Save,
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
import { ConnectorConfigurationPanel } from "@/features/terminals/components/connector-configuration-panel";
import { announceHardwareStatusChanged } from "@/features/terminals/local-hardware-connector";
import {
  type ConnectionType,
  type CashRegisterConfig,
  type HardwareBridgeConfig,
  type HardwarePeripheral,
  type PeripheralInput,
  type PeripheralStatus,
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
  const [registerConfig, setRegisterConfig] =
    useState<CashRegisterConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [draft, setDraft] = useState<PeripheralInput>(emptyPeripheral);
  const [saving, setSaving] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<HardwarePeripheral | null>(null);
  const [scannerValue, setScannerValue] = useState("");
  const [lastScan, setLastScan] = useState<string | null>(null);
  const [scannerTestPeripheralId, setScannerTestPeripheralId] =
    useState<string | null>(null);
  const [printing, setPrinting] = useState(false);
  const [printTimestamp, setPrintTimestamp] = useState("");

  const loadTerminal = useCallback(async () => {
    await Promise.resolve();
    setLoading(true);
    try {
      setTerminal(await terminalGateway.getTerminal(params.id));
      announceHardwareStatusChanged();
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
      announceHardwareStatusChanged();
    }
  }, []);

  useEffect(() => {
    if (!canRead) return;
    let active = true;
    void Promise.all([
      terminalGateway.getTerminal(params.id),
      terminalGateway.getHardwareConfig(),
      terminalGateway.getCashRegisterConfig(params.id),
    ])
      .then(([terminalResult, config, cashRegisterConfig]) => {
        if (!active) return;
        setTerminal(terminalResult);
        setBridgeConfig(config);
        setRegisterConfig(cashRegisterConfig);
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

  async function updatePeripheralStatus(
    peripheralId: string,
    status: PeripheralStatus,
  ) {
    if (!canManage) return;
    setSaving(true);
    setError(null);
    try {
      await terminalGateway.updatePeripheralStatus(peripheralId, status);
      await loadTerminal();
    } catch (caught) {
      setError(errorMessage(caught, "The device status could not be updated."));
    } finally {
      setSaving(false);
    }
  }

  async function saveRegisterConfig() {
    if (!terminal || !registerConfig || !canManage) return;
    setSaving(true);
    setError(null);
    try {
      setRegisterConfig(await terminalGateway.updateCashRegisterConfig(
        terminal.terminalId,
        registerConfig,
      ));
    } catch (caught) {
      setError(errorMessage(caught, "The cash register configuration could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  function updateRegister<K extends keyof CashRegisterConfig>(
    key: K,
    value: CashRegisterConfig[K],
  ) {
    setRegisterConfig((current) => current ? { ...current, [key]: value } : current);
  }

  async function testConnectorPeripheral(peripheral: HardwarePeripheral) {
    if (!bridgeConfig) return;
    const endpoint = peripheral.type === "CASH_DRAWER"
      ? "/cash-drawer/open"
      : "/display/show";
    setSaving(true);
    setError(null);
    try {
      const response = await fetch(`${bridgeConfig.connectorUrl}${endpoint}`, {
        body: peripheral.type === "DISPLAY"
          ? JSON.stringify({ line1: "PHARMACY POS", line2: "DISPLAY TEST" })
          : undefined,
        headers: peripheral.type === "DISPLAY"
          ? { "Content-Type": "application/json" }
          : undefined,
        method: "POST",
      });
      await terminalGateway.updatePeripheralStatus(
        peripheral.id,
        response.ok ? "ONLINE" : "ERROR",
      );
      if (!response.ok) throw new Error("The connector rejected the device test.");
      await loadTerminal();
    } catch (caught) {
      await terminalGateway.updatePeripheralStatus(peripheral.id, "ERROR").catch(() => undefined);
      setError(errorMessage(caught, "The device test failed."));
      await loadTerminal();
    } finally {
      setSaving(false);
    }
  }

  async function printTestReceipt() {
    setPrintTimestamp(new Intl.DateTimeFormat("en-KE", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "Africa/Nairobi",
    }).format(new Date()));
    const printer = terminal?.peripherals.find((item) => item.type === "PRINTER");
    if (terminal && bridgeConfig && bridgeState === "online" && printer) {
      setSaving(true);
      try {
        const response = await fetch(`${bridgeConfig.connectorUrl}/print`, {
          body: JSON.stringify({
            receipt: `PHARMACY POS\n${terminal.name}\nPRINT TEST\n${new Date().toISOString()}\n`,
          }),
          headers: { "Content-Type": "application/json" },
          method: "POST",
        });
        await terminalGateway.updatePeripheralStatus(
          printer.id,
          response.ok ? "ONLINE" : "ERROR",
        );
        if (!response.ok) throw new Error("The connector rejected the print test.");
        await loadTerminal();
        return;
      } catch (caught) {
        setError(errorMessage(caught, "The receipt printer test failed."));
        await loadTerminal();
      } finally {
        setSaving(false);
      }
    }
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
                        <div className="flex items-center gap-1">
                          <Select
                            aria-label={`${peripheral.type} status`}
                            className="h-9 w-28 text-xs"
                            disabled={saving}
                            value={peripheral.status}
                            onChange={(event) => void updatePeripheralStatus(
                              peripheral.id,
                              event.target.value as PeripheralStatus,
                            )}
                          >
                            <option value="UNKNOWN">Unknown</option>
                            <option value="ONLINE">Online</option>
                            <option value="OFFLINE">Offline</option>
                            <option value="ERROR">Error</option>
                          </Select>
                          {peripheral.type === "SCANNER" ? (
                            <button
                              type="button"
                              title="Test scanner"
                              aria-label="Test scanner"
                              onClick={() => {
                                setScannerTestPeripheralId(peripheral.id);
                                document.getElementById("scanner-test-input")?.focus();
                              }}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--brand-strong)] hover:bg-[var(--brand-soft)]"
                            >
                              <ScanBarcode aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                          {peripheral.type === "CASH_DRAWER" || peripheral.type === "DISPLAY" ? (
                            <button
                              type="button"
                              title={`Test ${peripheral.type.replaceAll("_", " ").toLowerCase()}`}
                              aria-label={`Test ${peripheral.type}`}
                              disabled={saving || bridgeState !== "online"}
                              onClick={() => void testConnectorPeripheral(peripheral)}
                              className="flex size-9 items-center justify-center rounded-md text-[var(--brand-strong)] hover:bg-[var(--brand-soft)] disabled:opacity-35"
                            >
                              <PlugZap aria-hidden="true" size={16} />
                            </button>
                          ) : null}
                          <button
                            type="button"
                            title="Remove peripheral"
                            aria-label={`Remove ${peripheral.type}`}
                            onClick={() => setRemoveTarget(peripheral)}
                            className="flex size-9 items-center justify-center rounded-md text-[var(--danger)] hover:bg-[var(--danger-soft)]"
                          >
                            <Trash2 aria-hidden="true" size={16} />
                          </button>
                        </div>
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

          {registerConfig ? (
            <section className="rounded-md border border-[var(--border)] bg-white">
              <div className="flex items-center justify-between gap-3 border-b border-[var(--border)] px-4 py-3.5">
                <div className="flex items-center gap-2">
                  <Banknote aria-hidden="true" className="text-[var(--brand)]" size={18} />
                  <h2 className="text-sm font-semibold">Cash register</h2>
                </div>
                {canManage ? (
                  <SecondaryButton type="button" disabled={saving} onClick={() => void saveRegisterConfig()}>
                    <Save aria-hidden="true" size={15} /> {saving ? "Saving..." : "Save"}
                  </SecondaryButton>
                ) : null}
              </div>
              <div className="grid gap-x-6 gap-y-4 p-4 md:grid-cols-2">
                <Field label="Default opening float (KES)">
                  <Input type="number" min={0} step="0.01" readOnly={!canManage} value={registerConfig.defaultOpeningFloat} onChange={(event) => updateRegister("defaultOpeningFloat", Number(event.target.value))} />
                </Field>
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Paper width">
                    <Select disabled={!canManage} value={registerConfig.receiptPaperWidth} onChange={(event) => updateRegister("receiptPaperWidth", Number(event.target.value))}>
                      <option value={58}>58 mm</option>
                      <option value={80}>80 mm</option>
                    </Select>
                  </Field>
                  <Field label="Receipt copies">
                    <Select disabled={!canManage} value={registerConfig.receiptCopies} onChange={(event) => updateRegister("receiptCopies", Number(event.target.value))}>
                      <option value={1}>1</option><option value={2}>2</option><option value={3}>3</option>
                    </Select>
                  </Field>
                </div>
                <Field label="Scanner mode">
                  <Select disabled={!canManage} value={registerConfig.scannerMode} onChange={(event) => updateRegister("scannerMode", event.target.value as CashRegisterConfig["scannerMode"])}>
                    <option value="KEYBOARD_WEDGE">Keyboard wedge</option>
                    <option value="CAMERA">Camera</option>
                    <option value="LOCAL_CONNECTOR">Local connector</option>
                  </Select>
                </Field>
                <Field label="Scanner suffix">
                  <Select disabled={!canManage} value={registerConfig.barcodeSubmitKey} onChange={(event) => updateRegister("barcodeSubmitKey", event.target.value as CashRegisterConfig["barcodeSubmitKey"])}>
                    <option value="ENTER">Enter</option><option value="TAB">Tab</option>
                  </Select>
                </Field>
                {registerConfig.scannerMode === "LOCAL_CONNECTOR" ? (
                  <p className={bridgeState === "online" ? "text-xs text-[var(--success)] md:col-span-2" : "text-xs text-[var(--danger)] md:col-span-2"}>
                    {bridgeState === "online"
                      ? `Barcode scans are read from the local connector at ${bridgeConfig?.connectorUrl ?? "the connector URL"}.`
                      : `Scanner mode is set to the local connector, but no connector responded at ${bridgeConfig?.connectorUrl ?? "the configured URL"}. Start the connector or switch to keyboard wedge.`}
                  </p>
                ) : null}
                <div className="grid gap-2 sm:grid-cols-2 md:col-span-2">
                  {([
                    ["cashEnabled", "Cash payments"],
                    ["mpesaEnabled", "M-Pesa payments"],
                    ["requireOpenShift", "Require open shift for checkout"],
                    ["autoPrintReceipt", "Print receipt automatically"],
                    ["openDrawerOnCashSale", "Open drawer after cash sale"],
                  ] as const).map(([key, label]) => (
                    <label key={key} className="flex min-h-10 items-center gap-3 rounded-md border border-[var(--border)] px-3 text-sm">
                      <input type="checkbox" disabled={!canManage} checked={registerConfig[key]} onChange={(event) => updateRegister(key, event.target.checked)} className="size-4 accent-[var(--brand)]" />
                      {label}
                    </label>
                  ))}
                </div>
              </div>
            </section>
          ) : null}

          {bridgeConfig ? (
            <ConnectorConfigurationPanel
              canManage={canManage}
              connectorUrl={bridgeConfig.connectorUrl}
            />
          ) : null}

          <section className="grid gap-5 md:grid-cols-2">
            <div className="rounded-md border border-[var(--border)] bg-white p-4">
              <div className="flex items-center gap-2">
                <Keyboard aria-hidden="true" className="text-[var(--brand)]" size={18} />
                <h2 className="text-sm font-semibold">Scanner test</h2>
              </div>
              <label className="mt-4 block">
                <span className="sr-only">Scan barcode</span>
                <Input
                  id="scanner-test-input"
                  placeholder="Focus here and scan"
                  value={scannerValue}
                  onChange={(event) => setScannerValue(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key !== "Enter" || !scannerValue.trim()) return;
                    event.preventDefault();
                    setLastScan(scannerValue.trim());
                    setScannerValue("");
                    const scanner = scannerTestPeripheralId
                      ? terminal.peripherals.find((item) => item.id === scannerTestPeripheralId)
                      : terminal.peripherals.find((item) => item.type === "SCANNER");
                    if (scanner && canManage) {
                      void updatePeripheralStatus(scanner.id, "ONLINE");
                    }
                    setScannerTestPeripheralId(null);
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
                onClick={() => void printTestReceipt()}
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
