import type { SystemStatus } from "@/types/api";
import { apiRequest } from "@/lib/api-client";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

type ApiPath = `/${string}`;

export type TerminalType =
  | "WEB"
  | "WINDOWS"
  | "ANDROID_HANDHELD"
  | "ANDROID_TABLET"
  | "IOS"
  | "API";
export type TerminalStatus = "PENDING" | "ACTIVE" | "DEACTIVATED" | "BLOCKED";
export type PeripheralType =
  | "PRINTER"
  | "SCANNER"
  | "CASH_DRAWER"
  | "SCALE"
  | "DISPLAY"
  | "FINGERPRINT"
  | "NFC"
  | "CAMERA"
  | "RFID"
  | "SECOND_DISPLAY"
  | "BARCODE_PRINTER";
export type ConnectionType =
  | "NETWORK"
  | "USB"
  | "BLUETOOTH"
  | "SERIAL"
  | "WEDGE"
  | "PRINTER_PORT";

export interface BranchSummary {
  id: string;
  branchName: string;
  branchCode: string;
  status: string;
}

export interface HardwarePeripheral {
  id: string;
  type: PeripheralType;
  manufacturer: string | null;
  model: string | null;
  connectionType: ConnectionType;
  status: "ONLINE" | "OFFLINE" | "UNKNOWN" | "ERROR";
  configuration: string | null;
}

export interface Terminal {
  id: string;
  terminalId: string;
  name: string;
  terminalType: TerminalType;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  platform: string | null;
  osVersion: string | null;
  firmwareVersion: string | null;
  status: TerminalStatus;
  branchId: string;
  branchName: string | null;
  registeredBy: string | null;
  registeredAt: string;
  lastSeenAt: string | null;
  appVersion: string | null;
  supportedApiVersion: string | null;
  lastUpdate: string | null;
  minimumBackendVersion: string | null;
  peripherals: HardwarePeripheral[];
}

export interface TerminalInput {
  name: string;
  terminalType: TerminalType;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  platform: string | null;
  osVersion: string | null;
  firmwareVersion: string | null;
  branchId: string;
}

export interface PeripheralInput {
  type: PeripheralType;
  manufacturer: string | null;
  model: string | null;
  connectionType: ConnectionType;
  configuration: string | null;
}

export interface HardwareBridgeConfig {
  connectorUrl: string;
  endpoints: {
    print: string;
    cashDrawer: string;
    display: string;
    health: string;
  };
  printerType: string;
  receiptWidth: number;
  scannerMode: string;
}

export interface SystemStatusSnapshot {
  requestId: string | null;
  status: SystemStatus;
}

interface TerminalGateway {
  addPeripheral(terminalId: string, input: PeripheralInput): Promise<HardwarePeripheral>;
  approveTerminal(terminalId: string): Promise<Terminal>;
  blockTerminal(terminalId: string): Promise<Terminal>;
  deactivateTerminal(terminalId: string): Promise<Terminal>;
  getHardwareConfig(): Promise<HardwareBridgeConfig>;
  getSystemStatus(signal?: AbortSignal): Promise<SystemStatusSnapshot>;
  getTerminal(terminalId: string): Promise<Terminal>;
  heartbeat(terminalId: string): Promise<void>;
  listBranches(pharmacyId: string): Promise<BranchSummary[]>;
  listTerminals(): Promise<Terminal[]>;
  registerTerminal(input: TerminalInput): Promise<Terminal>;
  regenerateApiKey(terminalId: string): Promise<Terminal>;
  removePeripheral(peripheralId: string): Promise<void>;
  updateTerminal(terminalId: string, input: TerminalInput): Promise<Terminal>;
}

function path(value: string) {
  return value as ApiPath;
}

class LiveTerminalGateway implements TerminalGateway {
  async addPeripheral(terminalId: string, input: PeripheralInput) {
    const response = await apiRequest<HardwarePeripheral>(
      path(`/terminals/${terminalId}/peripherals`),
      { body: input, method: "POST" },
    );
    return response.data;
  }

  async approveTerminal(terminalId: string) {
    const response = await apiRequest<Terminal>(
      path(`/terminals/${terminalId}/approve`),
      { method: "POST" },
    );
    return response.data;
  }

  async blockTerminal(terminalId: string) {
    const response = await apiRequest<Terminal>(
      path(`/terminals/${terminalId}/block`),
      { method: "POST" },
    );
    return response.data;
  }

  async deactivateTerminal(terminalId: string) {
    const response = await apiRequest<Terminal>(
      path(`/terminals/${terminalId}/deactivate`),
      { method: "POST" },
    );
    return response.data;
  }

  async getHardwareConfig() {
    return (await apiRequest<HardwareBridgeConfig>("/hardware/config")).data;
  }

  async getSystemStatus(signal?: AbortSignal): Promise<SystemStatusSnapshot> {
    const response = await apiRequest<SystemStatus>("/system/status", {
      cache: "no-store",
      signal,
    });
    return { requestId: response.meta.requestId, status: response.data };
  }

  async getTerminal(terminalId: string) {
    return (
      await apiRequest<Terminal>(path(`/terminals/${terminalId}`), {
        cache: "no-store",
      })
    ).data;
  }

  async heartbeat(terminalId: string) {
    await apiRequest(path(`/terminals/${terminalId}/heartbeat`), {
      body: {
        networkType: typeof navigator !== "undefined" && navigator.onLine
          ? "ONLINE"
          : "OFFLINE",
        terminalId,
        timestamp: new Date().toISOString().slice(0, 19),
        uptimeMinutes:
          typeof performance === "undefined"
            ? null
            : Math.floor(performance.now() / 60_000),
      },
      method: "POST",
    });
  }

  async listBranches(pharmacyId: string) {
    return (
      await apiRequest<BranchSummary[]>(
        path(`/branches?pharmacyId=${encodeURIComponent(pharmacyId)}`),
        { cache: "no-store" },
      )
    ).data;
  }

  async listTerminals() {
    return (
      await apiRequest<Terminal[]>("/terminals", { cache: "no-store" })
    ).data;
  }

  async registerTerminal(input: TerminalInput) {
    return (
      await apiRequest<Terminal>("/terminals/register", {
        body: input,
        method: "POST",
      })
    ).data;
  }

  async regenerateApiKey(terminalId: string) {
    return (
      await apiRequest<Terminal>(
        path(`/terminals/${terminalId}/regenerate-key`),
        { method: "POST" },
      )
    ).data;
  }

  async removePeripheral(peripheralId: string) {
    await apiRequest<void>(path(`/terminals/peripherals/${peripheralId}`), {
      method: "DELETE",
    });
  }

  async updateTerminal(terminalId: string, input: TerminalInput) {
    return (
      await apiRequest<Terminal>(path(`/terminals/${terminalId}`), {
        body: input,
        method: "PUT",
      })
    ).data;
  }
}

interface PreviewTerminalState {
  terminals: Terminal[];
}

const PREVIEW_KEY = "pharmacy-pos:terminals-preview";
export const LOCAL_TERMINAL_KEY = "pharmacy-pos:local-terminal-id";

function previewState(): PreviewTerminalState {
  if (typeof window !== "undefined") {
    const stored = window.localStorage.getItem(PREVIEW_KEY);
    if (stored) {
      try {
        return JSON.parse(stored) as PreviewTerminalState;
      } catch {
        // Fall through to a fresh preview node.
      }
    }
  }
  const now = new Date().toISOString();
  return {
    terminals: [
      {
        appVersion: "0.1.0",
        branchId: "preview-main",
        branchName: "Main branch",
        firmwareVersion: null,
        id: "terminal-preview-1",
        lastSeenAt: now,
        lastUpdate: now,
        manufacturer: null,
        minimumBackendVersion: "0.0.1",
        model: null,
        name: "Front counter",
        osVersion: null,
        peripherals: [],
        platform: "Browser",
        registeredAt: now,
        registeredBy: "owner@preview.local",
        serialNumber: null,
        status: "ACTIVE",
        supportedApiVersion: "v1",
        terminalId: "T-PREVIEW",
        terminalType: "WEB",
      },
    ],
  };
}

function savePreview(state: PreviewTerminalState) {
  window.localStorage.setItem(PREVIEW_KEY, JSON.stringify(state));
}

function previewTerminal(terminalId: string) {
  const terminal = previewState().terminals.find(
    (candidate) => candidate.terminalId === terminalId,
  );
  if (!terminal) throw new Error("Terminal not found.");
  return terminal;
}

class PreviewTerminalGateway implements TerminalGateway {
  async addPeripheral(terminalId: string, input: PeripheralInput) {
    const state = previewState();
    const terminal = state.terminals.find(
      (candidate) => candidate.terminalId === terminalId,
    );
    if (!terminal) throw new Error("Terminal not found.");
    const peripheral: HardwarePeripheral = {
      ...input,
      id: crypto.randomUUID(),
      status: "UNKNOWN",
    };
    terminal.peripherals.push(peripheral);
    savePreview(state);
    return peripheral;
  }

  async approveTerminal(terminalId: string) {
    return this.changeStatus(terminalId, "ACTIVE");
  }

  async blockTerminal(terminalId: string) {
    return this.changeStatus(terminalId, "BLOCKED");
  }

  async deactivateTerminal(terminalId: string) {
    return this.changeStatus(terminalId, "DEACTIVATED");
  }

  async getHardwareConfig() {
    return {
      connectorUrl: "http://localhost:9100",
      endpoints: {
        cashDrawer: "POST /cash-drawer/open",
        display: "POST /display/show",
        health: "GET /health",
        print: "POST /print",
      },
      printerType: "esc_pos",
      receiptWidth: 42,
      scannerMode: "keyboard_wedge",
    };
  }

  async getSystemStatus(): Promise<SystemStatusSnapshot> {
    return {
      requestId: null,
      status: {
        api: "UP",
        application: "Pharmacy POS preview",
        checkedAt: new Date().toISOString(),
        database: "UP",
        databaseName: "preview",
        version: "0.0.1",
      },
    };
  }

  async getTerminal(terminalId: string) {
    return previewTerminal(terminalId);
  }

  async heartbeat(terminalId: string) {
    const state = previewState();
    const terminal = state.terminals.find(
      (candidate) => candidate.terminalId === terminalId,
    );
    if (!terminal) return;
    terminal.lastSeenAt = new Date().toISOString();
    savePreview(state);
  }

  async listBranches() {
    return [
      {
        branchCode: "MAIN",
        branchName: "Main branch",
        id: "preview-main",
        status: "ACTIVE",
      },
    ];
  }

  async listTerminals() {
    return previewState().terminals;
  }

  async registerTerminal(input: TerminalInput) {
    const state = previewState();
    const now = new Date().toISOString();
    const terminal: Terminal = {
      ...input,
      appVersion: null,
      branchName: input.branchId === "preview-main" ? "Main branch" : null,
      id: crypto.randomUUID(),
      lastSeenAt: null,
      lastUpdate: null,
      minimumBackendVersion: "0.0.1",
      peripherals: [],
      registeredAt: now,
      registeredBy: "owner@preview.local",
      status: "PENDING",
      supportedApiVersion: "v1",
      terminalId: `T-${crypto.randomUUID().slice(0, 8).toUpperCase()}`,
    };
    state.terminals.push(terminal);
    savePreview(state);
    return terminal;
  }

  async regenerateApiKey(terminalId: string) {
    const state = previewState();
    const terminal = state.terminals.find(
      (candidate) => candidate.terminalId === terminalId,
    );
    if (!terminal) throw new Error("Terminal not found.");
    terminal.lastUpdate = new Date().toISOString();
    savePreview(state);
    return terminal;
  }

  async removePeripheral(peripheralId: string) {
    const state = previewState();
    for (const terminal of state.terminals) {
      terminal.peripherals = terminal.peripherals.filter(
        (peripheral) => peripheral.id !== peripheralId,
      );
    }
    savePreview(state);
  }

  async updateTerminal(terminalId: string, input: TerminalInput) {
    const state = previewState();
    const terminal = state.terminals.find(
      (candidate) => candidate.terminalId === terminalId,
    );
    if (!terminal) throw new Error("Terminal not found.");
    Object.assign(terminal, input, { lastUpdate: new Date().toISOString() });
    savePreview(state);
    return terminal;
  }

  private async changeStatus(terminalId: string, status: TerminalStatus) {
    const state = previewState();
    const terminal = state.terminals.find(
      (candidate) => candidate.terminalId === terminalId,
    );
    if (!terminal) throw new Error("Terminal not found.");
    terminal.status = status;
    terminal.lastUpdate = new Date().toISOString();
    savePreview(state);
    return terminal;
  }
}

export function getLocalTerminalId() {
  return typeof window === "undefined"
    ? null
    : window.localStorage.getItem(LOCAL_TERMINAL_KEY);
}

export function setLocalTerminalId(terminalId: string | null) {
  if (terminalId) window.localStorage.setItem(LOCAL_TERMINAL_KEY, terminalId);
  else window.localStorage.removeItem(LOCAL_TERMINAL_KEY);
  window.dispatchEvent(new Event("pharmacy-pos:terminal-assignment"));
}

export function isTerminalOnline(terminal: Pick<Terminal, "lastSeenAt" | "status">) {
  return (
    terminal.status === "ACTIVE" &&
    Boolean(
      terminal.lastSeenAt &&
        new Date(terminal.lastSeenAt).getTime() > Date.now() - 10 * 60_000,
    )
  );
}

export const terminalGateway: TerminalGateway = DEMO_AUTH_ENABLED
  ? new PreviewTerminalGateway()
  : new LiveTerminalGateway();
