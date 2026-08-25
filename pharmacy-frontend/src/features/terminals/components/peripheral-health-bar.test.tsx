import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { getTerminalIdMock, getHealthMock, getBridgeConfigMock, getConnectorHealthMock } =
  vi.hoisted(() => ({
    getTerminalIdMock: vi.fn(),
    getHealthMock: vi.fn(),
    getBridgeConfigMock: vi.fn(),
    getConnectorHealthMock: vi.fn(),
  }));

vi.mock("@/features/terminals/terminal-gateway", () => ({
  getLocalTerminalId: getTerminalIdMock,
  terminalGateway: {
    getHardwareConfig: getBridgeConfigMock,
    getTerminalHealth: getHealthMock,
  },
}));

vi.mock("@/features/terminals/local-hardware-connector", () => ({
  HARDWARE_STATUS_CHANGED_EVENT: "pharmacy-pos:hardware-status-changed",
  getLocalConnectorHealth: getConnectorHealthMock,
}));

import { PeripheralHealthBar } from "./peripheral-health-bar";

const terminalHealth = {
  terminalId: "TERM-01",
  online: true,
  lastSeenAt: "2026-08-25T10:00:00Z",
  peripherals: [
    { id: "p1", type: "PRINTER", manufacturer: null, model: null, connectionType: "NETWORK", configuration: null, status: "UNKNOWN" },
    { id: "p2", type: "SCANNER", manufacturer: null, model: null, connectionType: "WEDGE", configuration: null, status: "OFFLINE" },
  ],
};

const bridgeConfig = { connectorUrl: "http://localhost:9100" };

const connectorHealth = {
  status: "ok" as const,
  printer: { connected: true, type: "network", width: 42 },
  scanner: { ready: true, type: "keyboard_wedge", com_port: null, last_barcode: null },
  cash_drawer: { enabled: false, mode: "printer", ready: false },
  display: { enabled: false, connected: false, com_port: "COM2", lines: 2, columns: 20 },
};

describe("PeripheralHealthBar", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getTerminalIdMock.mockReturnValue("TERM-01");
    getHealthMock.mockResolvedValue(terminalHealth);
    getBridgeConfigMock.mockResolvedValue(bridgeConfig);
    getConnectorHealthMock.mockResolvedValue(connectorHealth);
  });

  it("merges backend peripherals with live connector health", async () => {
    render(<PeripheralHealthBar canConfigure />);

    expect(await screen.findByText("TERM-01")).toBeVisible();
    expect(screen.getByText("Connector online")).toBeVisible();
    expect(screen.getByText("printer online")).toBeVisible();
    expect(screen.getByText("scanner online")).toBeVisible();
    expect(getHealthMock).toHaveBeenCalledWith("TERM-01");
    expect(getConnectorHealthMock).toHaveBeenCalledWith(
      "http://localhost:9100",
      expect.anything(),
    );
  });

  it("prompts to assign the register when no terminal is assigned", async () => {
    getTerminalIdMock.mockReturnValue(null);

    render(<PeripheralHealthBar canConfigure={false} />);

    expect(await screen.findByText("Register not assigned")).toBeVisible();
    expect(getHealthMock).not.toHaveBeenCalled();
    expect(screen.queryByText(/Connector /)).not.toBeInTheDocument();
  });

  it("marks devices offline when the connector does not answer", async () => {
    getConnectorHealthMock.mockRejectedValue(new Error("timeout"));

    render(<PeripheralHealthBar canConfigure={false} />);

    expect(await screen.findByText("TERM-01")).toBeVisible();
    expect(screen.getByText("Connector offline")).toBeVisible();
    expect(screen.getByText("printer offline")).toBeVisible();
    expect(screen.getByText("scanner offline")).toBeVisible();
  });
});
