"use client";

import { Cable, Save } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { SecondaryButton } from "@/components/ui/buttons";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { StatusBadge } from "@/components/ui/status-badge";
import {
  announceHardwareStatusChanged,
  getLocalConnectorConfig,
  getLocalConnectorHealth,
  type LocalConnectorConfig,
  type LocalConnectorHealth,
  updateLocalConnectorConfig,
} from "@/features/terminals/local-hardware-connector";

interface Props {
  canManage: boolean;
  connectorUrl: string;
}

function numberValue(value: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function ConnectorConfigurationPanel({ canManage, connectorUrl }: Props) {
  const [config, setConfig] = useState<LocalConnectorConfig | null>(null);
  const [health, setHealth] = useState<LocalConnectorHealth | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      const [nextConfig, nextHealth] = await Promise.all([
        getLocalConnectorConfig(connectorUrl),
        getLocalConnectorHealth(connectorUrl),
      ]);
      setConfig(nextConfig);
      setHealth(nextHealth);
      setError(null);
    } catch (caught) {
      setHealth(null);
      setError(caught instanceof Error ? caught.message : "Hardware connector is unavailable.");
    }
  }, [connectorUrl]);

  useEffect(() => {
    const initial = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(initial);
  }, [load]);

  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!config || !canManage) return;
    setSaving(true);
    setError(null);
    try {
      setConfig(await updateLocalConnectorConfig(connectorUrl, config));
      setHealth(await getLocalConnectorHealth(connectorUrl));
      announceHardwareStatusChanged();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Device profile could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  function updateSection<Section extends keyof LocalConnectorConfig>(
    section: Section,
    value: Partial<LocalConnectorConfig[Section]>,
  ) {
    setConfig((current) => current ? {
      ...current,
      [section]: { ...current[section], ...value },
    } : current);
  }

  return (
    <section className="rounded-md border border-[var(--border)] bg-white">
      <div className="flex items-center justify-between gap-3 border-b border-[var(--border)] px-4 py-3.5">
        <div className="flex items-center gap-2">
          <Cable aria-hidden="true" className="text-[var(--brand)]" size={18} />
          <h2 className="text-sm font-semibold">Local device profile</h2>
          <StatusBadge tone={health ? "success" : "danger"}>
            {health ? "Connected" : "Offline"}
          </StatusBadge>
        </div>
      </div>
      {error ? <div className="p-4 pb-0"><FormError message={error} /></div> : null}
      {config ? (
        <form onSubmit={save}>
          <div className="grid gap-x-6 gap-y-4 p-4 md:grid-cols-2">
            <div className="space-y-4">
              <h3 className="text-xs font-semibold uppercase text-[var(--text-muted)]">Receipt printer</h3>
              <Field label="Connection">
                <Select disabled={!canManage} value={config.printer.type} onChange={(event) => updateSection("printer", { type: event.target.value as LocalConnectorConfig["printer"]["type"] })}>
                  <option value="network">Network</option>
                  <option value="serial">Serial / COM</option>
                  <option value="usb">USB</option>
                </Select>
              </Field>
              {config.printer.type === "network" ? (
                <div className="grid grid-cols-[minmax(0,1fr)_110px] gap-3">
                  <Field label="Printer address"><Input disabled={!canManage} value={config.printer.host} onChange={(event) => updateSection("printer", { host: event.target.value })} /></Field>
                  <Field label="Port"><Input disabled={!canManage} min={1} max={65535} type="number" value={config.printer.port} onChange={(event) => updateSection("printer", { port: numberValue(event.target.value) })} /></Field>
                </div>
              ) : null}
              {config.printer.type === "serial" ? (
                <div className="grid grid-cols-2 gap-3">
                  <Field label="COM port"><Input disabled={!canManage} value={config.printer.com_port} onChange={(event) => updateSection("printer", { com_port: event.target.value })} /></Field>
                  <Field label="Baud rate"><Input disabled={!canManage} min={300} type="number" value={config.printer.baud_rate} onChange={(event) => updateSection("printer", { baud_rate: numberValue(event.target.value) })} /></Field>
                </div>
              ) : null}
              {config.printer.type === "usb" ? (
                <div className="grid grid-cols-2 gap-3">
                  <Field label="USB vendor ID"><Input disabled={!canManage} value={`0x${config.printer.vendor_id.toString(16).padStart(4, "0")}`} onChange={(event) => updateSection("printer", { vendor_id: Number.parseInt(event.target.value.replace(/^0x/i, ""), 16) || 0 })} /></Field>
                  <Field label="USB product ID"><Input disabled={!canManage} value={`0x${config.printer.product_id.toString(16).padStart(4, "0")}`} onChange={(event) => updateSection("printer", { product_id: Number.parseInt(event.target.value.replace(/^0x/i, ""), 16) || 0 })} /></Field>
                </div>
              ) : null}
              <Field label="Receipt characters"><Select disabled={!canManage} value={config.printer.width} onChange={(event) => updateSection("printer", { width: numberValue(event.target.value) })}><option value={32}>32 (58 mm)</option><option value={42}>42 (80 mm)</option><option value={48}>48 (wide)</option></Select></Field>
              <p className="text-xs text-[var(--text-muted)]">Printer: {health?.printer.connected ? "online" : "not detected"}</p>
            </div>

            <div className="space-y-4">
              <h3 className="text-xs font-semibold uppercase text-[var(--text-muted)]">Barcode scanner</h3>
              <Field label="Scanner connection"><Select disabled={!canManage} value={config.scanner.mode} onChange={(event) => updateSection("scanner", { mode: event.target.value as LocalConnectorConfig["scanner"]["mode"] })}><option value="keyboard_wedge">USB keyboard wedge</option><option value="serial">Serial / COM</option></Select></Field>
              {config.scanner.mode === "serial" ? (
                <div className="grid grid-cols-2 gap-3">
                  <Field label="COM port"><Input disabled={!canManage} value={config.scanner.com_port} onChange={(event) => updateSection("scanner", { com_port: event.target.value })} /></Field>
                  <Field label="Baud rate"><Input disabled={!canManage} min={300} type="number" value={config.scanner.baud_rate} onChange={(event) => updateSection("scanner", { baud_rate: numberValue(event.target.value) })} /></Field>
                </div>
              ) : null}
              <p className="text-xs text-[var(--text-muted)]">Scanner: {health?.scanner.ready ? "ready" : "not detected"}</p>

              <h3 className="pt-2 text-xs font-semibold uppercase text-[var(--text-muted)]">Cash drawer</h3>
              <label className="flex min-h-10 items-center gap-3 rounded-md border border-[var(--border)] px-3 text-sm"><input className="size-4 accent-[var(--brand)]" type="checkbox" disabled={!canManage} checked={config.cash_drawer.enabled} onChange={(event) => updateSection("cash_drawer", { enabled: event.target.checked })} />Enable cash drawer</label>
              {config.cash_drawer.enabled ? <div className="grid grid-cols-2 gap-3"><Field label="Connection"><Select disabled={!canManage} value={config.cash_drawer.mode} onChange={(event) => updateSection("cash_drawer", { mode: event.target.value as LocalConnectorConfig["cash_drawer"]["mode"] })}><option value="printer">Receipt printer port</option><option value="serial">Serial / COM</option></Select></Field>{config.cash_drawer.mode === "serial" ? <Field label="COM port"><Input disabled={!canManage} value={config.cash_drawer.com_port} onChange={(event) => updateSection("cash_drawer", { com_port: event.target.value })} /></Field> : <Field label="Drawer pin"><Select disabled={!canManage} value={config.cash_drawer.pin} onChange={(event) => updateSection("cash_drawer", { pin: numberValue(event.target.value) })}><option value={0}>Pin 2</option><option value={1}>Pin 5</option><option value={2}>Auto / profile 2</option></Select></Field>}</div> : null}

              <h3 className="pt-2 text-xs font-semibold uppercase text-[var(--text-muted)]">Customer display</h3>
              <label className="flex min-h-10 items-center gap-3 rounded-md border border-[var(--border)] px-3 text-sm"><input className="size-4 accent-[var(--brand)]" type="checkbox" disabled={!canManage} checked={config.display.enabled} onChange={(event) => updateSection("display", { enabled: event.target.checked })} />Enable serial display</label>
              {config.display.enabled ? <div className="grid grid-cols-2 gap-3"><Field label="COM port"><Input disabled={!canManage} value={config.display.com_port} onChange={(event) => updateSection("display", { com_port: event.target.value })} /></Field><Field label="Baud rate"><Input disabled={!canManage} min={300} type="number" value={config.display.baud_rate} onChange={(event) => updateSection("display", { baud_rate: numberValue(event.target.value) })} /></Field></div> : null}
            </div>
          </div>
          {canManage ? <div className="flex justify-end border-t border-[var(--border)] px-4 py-3"><SecondaryButton type="submit" disabled={saving}><Save aria-hidden="true" size={15} />{saving ? "Saving..." : "Save device profile"}</SecondaryButton></div> : null}
        </form>
      ) : (
        <p className="p-4 text-sm text-[var(--text-muted)]">Start the local connector to configure printer ports and serial devices.</p>
      )}
    </section>
  );
}
