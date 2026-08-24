"use client";

import { Cable, CircleAlert, MonitorCheck, PlugZap, Printer, ScanBarcode } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";

import {
  HARDWARE_STATUS_CHANGED_EVENT,
  getLocalConnectorHealth,
  type LocalConnectorHealth,
} from "@/features/terminals/local-hardware-connector";
import {
  getLocalTerminalId,
  type HardwarePeripheral,
  type TerminalHealth,
  terminalGateway,
} from "@/features/terminals/terminal-gateway";
import { cn } from "@/lib/cn";

function deviceIcon(peripheral: HardwarePeripheral) {
  if (peripheral.type === "PRINTER" || peripheral.type === "BARCODE_PRINTER") return Printer;
  if (peripheral.type === "SCANNER" || peripheral.type === "CAMERA") return ScanBarcode;
  return PlugZap;
}

function statusClass(status: "ONLINE" | "OFFLINE" | "UNKNOWN" | "ERROR") {
  if (status === "ONLINE") return "text-[var(--success)]";
  if (status === "ERROR" || status === "OFFLINE") return "text-[var(--danger)]";
  return "text-[var(--warning)]";
}

export function PeripheralHealthBar({ canConfigure }: { canConfigure: boolean }) {
  const [terminalId, setTerminalId] = useState<string | null>(null);
  const [health, setHealth] = useState<TerminalHealth | null>(null);
  const [connectorHealth, setConnectorHealth] = useState<LocalConnectorHealth | null>(null);

  const load = useCallback(async () => {
    const assigned = getLocalTerminalId();
    setTerminalId(assigned);
    if (!assigned) {
      setHealth(null);
      setConnectorHealth(null);
      return;
    }
    try {
      const [terminalHealth, bridge] = await Promise.all([
        terminalGateway.getTerminalHealth(assigned),
        terminalGateway.getHardwareConfig(),
      ]);
      setHealth(terminalHealth);
      const controller = new AbortController();
      const timeout = window.setTimeout(() => controller.abort(), 2_000);
      try {
        setConnectorHealth(await getLocalConnectorHealth(
          bridge.connectorUrl,
          controller.signal,
        ));
      } catch {
        setConnectorHealth(null);
      } finally {
        window.clearTimeout(timeout);
      }
    } catch {
      setHealth(null);
      setConnectorHealth(null);
    }
  }, []);

  useEffect(() => {
    const initial = window.setTimeout(() => void load(), 0);
    const interval = window.setInterval(() => void load(), 30_000);
    window.addEventListener("pharmacy-pos:terminal-assignment", load);
    window.addEventListener(HARDWARE_STATUS_CHANGED_EVENT, load);
    window.addEventListener("online", load);
    window.addEventListener("offline", load);
    return () => {
      window.clearTimeout(initial);
      window.clearInterval(interval);
      window.removeEventListener("pharmacy-pos:terminal-assignment", load);
      window.removeEventListener(HARDWARE_STATUS_CHANGED_EVENT, load);
      window.removeEventListener("online", load);
      window.removeEventListener("offline", load);
    };
  }, [load]);

  const visibleDevices = useMemo(
    () => health?.peripherals.filter((item) =>
      ["PRINTER", "SCANNER", "CASH_DRAWER", "BARCODE_PRINTER", "DISPLAY"].includes(item.type),
    ).slice(0, 5) ?? [],
    [health],
  );

  const content = (
    <div className="flex min-w-max items-center gap-4 px-4 text-xs sm:px-6">
      <span className={cn("flex items-center gap-1.5 font-semibold", terminalId && health?.online ? "text-[var(--success)]" : "text-[var(--warning)]")}>
        {terminalId ? <MonitorCheck aria-hidden="true" size={14} /> : <CircleAlert aria-hidden="true" size={14} />}
        {terminalId ? terminalId : "Register not assigned"}
      </span>
      {terminalId ? <span className={cn("flex items-center gap-1.5", connectorHealth ? "text-[var(--success)]" : "text-[var(--danger)]")}><Cable aria-hidden="true" size={14} />Connector {connectorHealth ? "online" : "offline"}</span> : null}
      {visibleDevices.map((peripheral) => {
        const Icon = deviceIcon(peripheral);
        const liveStatus = connectorHealth
          ? connectorStatus(peripheral, connectorHealth)
          : "OFFLINE";
        return <span key={peripheral.id} className={cn("flex items-center gap-1.5", statusClass(liveStatus))}><Icon aria-hidden="true" size={14} />{peripheral.type.replaceAll("_", " ").toLowerCase()} {liveStatus.toLowerCase()}</span>;
      })}
      {terminalId && health && visibleDevices.length === 0 ? <span className="text-[var(--text-muted)]">No peripherals recorded</span> : null}
    </div>
  );

  return (
    <div className="sticky top-16 z-10 h-9 overflow-x-auto border-b border-[var(--border)] bg-white print:hidden">
      {terminalId && canConfigure ? <Link href={`/admin/terminals/${terminalId}/hardware`} className="flex h-full items-center hover:bg-[var(--surface-muted)]" title="Open hardware setup">{content}</Link> : <div className="flex h-full items-center">{content}</div>}
    </div>
  );
}

function connectorStatus(
  peripheral: HardwarePeripheral,
  health: LocalConnectorHealth,
): "ONLINE" | "OFFLINE" | "UNKNOWN" | "ERROR" {
  if (peripheral.type === "PRINTER" || peripheral.type === "BARCODE_PRINTER") {
    return health.printer.connected ? "ONLINE" : "OFFLINE";
  }
  if (peripheral.type === "SCANNER") {
    return health.scanner.ready ? "ONLINE" : "OFFLINE";
  }
  if (peripheral.type === "CASH_DRAWER") {
    return health.cash_drawer.ready ? "ONLINE" : "OFFLINE";
  }
  if (peripheral.type === "DISPLAY") {
    return health.display.connected ? "ONLINE" : "OFFLINE";
  }
  return peripheral.status;
}
