export interface LocalConnectorConfig {
  printer: {
    type: "network" | "serial" | "usb";
    host: string;
    port: number;
    com_port: string;
    baud_rate: number;
    vendor_id: number;
    product_id: number;
    width: number;
  };
  scanner: {
    mode: "keyboard_wedge" | "serial";
    com_port: string;
    baud_rate: number;
  };
  cash_drawer: {
    enabled: boolean;
    mode: "printer" | "serial";
    com_port: string;
    pin: number;
  };
  display: {
    enabled: boolean;
    com_port: string;
    baud_rate: number;
    lines: number;
    columns: number;
  };
}

export interface LocalConnectorHealth {
  status: "ok";
  printer: { connected: boolean; type: string; width: number };
  scanner: {
    ready: boolean;
    type: string;
    com_port: string | null;
    last_barcode: string | null;
  };
  cash_drawer: { enabled: boolean; mode: string; ready: boolean };
  display: {
    enabled: boolean;
    connected: boolean;
    com_port: string;
    lines: number;
    columns: number;
  };
}

export const HARDWARE_STATUS_CHANGED_EVENT =
  "pharmacy-pos:hardware-status-changed";

export function announceHardwareStatusChanged() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(HARDWARE_STATUS_CHANGED_EVENT));
  }
}

function endpoint(connectorUrl: string, path: string) {
  return `${connectorUrl.replace(/\/$/, "")}${path}`;
}

async function connectorRequest<T>(
  connectorUrl: string,
  path: string,
  init?: RequestInit,
) {
  const response = await fetch(endpoint(connectorUrl, path), {
    cache: "no-store",
    ...init,
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { error?: string } | null;
    throw new Error(body?.error || `Hardware connector returned ${response.status}.`);
  }
  return response.json() as Promise<T>;
}

export function getLocalConnectorConfig(connectorUrl: string) {
  return connectorRequest<LocalConnectorConfig>(connectorUrl, "/config");
}

export function updateLocalConnectorConfig(
  connectorUrl: string,
  config: LocalConnectorConfig,
) {
  return connectorRequest<LocalConnectorConfig>(connectorUrl, "/config", {
    body: JSON.stringify(config),
    headers: { "Content-Type": "application/json" },
    method: "PUT",
  });
}

export function getLocalConnectorHealth(
  connectorUrl: string,
  signal?: AbortSignal,
) {
  return connectorRequest<LocalConnectorHealth>(connectorUrl, "/health", {
    signal,
  });
}

export function getLastConnectorBarcode(connectorUrl: string) {
  return connectorRequest<{ barcode: string | null }>(
    connectorUrl,
    "/scanner/last",
  );
}
