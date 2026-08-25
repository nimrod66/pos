import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { getConfigMock, getHealthMock, updateConfigMock, announceMock } =
  vi.hoisted(() => ({
    getConfigMock: vi.fn(),
    getHealthMock: vi.fn(),
    updateConfigMock: vi.fn(),
    announceMock: vi.fn(),
  }));

vi.mock("@/features/terminals/local-hardware-connector", () => ({
  announceHardwareStatusChanged: announceMock,
  getLocalConnectorConfig: getConfigMock,
  getLocalConnectorHealth: getHealthMock,
  updateLocalConnectorConfig: updateConfigMock,
}));

import { ConnectorConfigurationPanel } from "./connector-configuration-panel";

const sampleConfig = {
  printer: {
    type: "network" as const,
    host: "192.168.1.50",
    port: 9100,
    com_port: "COM1",
    baud_rate: 9600,
    vendor_id: 1208,
    product_id: 36864,
    width: 42,
  },
  scanner: { mode: "keyboard_wedge" as const, com_port: "COM3", baud_rate: 9600 },
  cash_drawer: { enabled: true, mode: "printer" as const, com_port: "COM4", pin: 0 },
  display: { enabled: false, com_port: "COM2", baud_rate: 9600, lines: 2, columns: 20 },
};

const sampleHealth = {
  status: "ok" as const,
  printer: { connected: true, type: "network", width: 42 },
  scanner: { ready: true, type: "keyboard_wedge", com_port: null, last_barcode: null },
  cash_drawer: { enabled: true, mode: "printer", ready: true },
  display: { enabled: false, connected: false, com_port: "COM2", lines: 2, columns: 20 },
};

describe("ConnectorConfigurationPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getConfigMock.mockResolvedValue(sampleConfig);
    getHealthMock.mockResolvedValue(sampleHealth);
    updateConfigMock.mockResolvedValue(sampleConfig);
  });

  it("loads the device profile and reports a connected connector", async () => {
    render(<ConnectorConfigurationPanel canManage connectorUrl="http://localhost:9100" />);

    expect(screen.getByText("Local device profile")).toBeVisible();
    expect(await screen.findByText("Connected")).toBeVisible();
    expect(getConfigMock).toHaveBeenCalledWith("http://localhost:9100");
    expect(screen.getByDisplayValue("192.168.1.50")).toBeVisible();
    expect(screen.getByText("Printer: online")).toBeVisible();
    expect(screen.getByText("Scanner: ready")).toBeVisible();
  });

  it("shows the offline fallback when the connector is unreachable", async () => {
    getConfigMock.mockRejectedValue(new Error("connection refused"));
    getHealthMock.mockRejectedValue(new Error("connection refused"));

    render(<ConnectorConfigurationPanel canManage={false} connectorUrl="http://localhost:9100" />);

    expect(await screen.findByText("Offline")).toBeVisible();
    expect(await screen.findByText("connection refused")).toBeVisible();
    expect(
      screen.getByText("Start the local connector to configure printer ports and serial devices."),
    ).toBeVisible();
  });

  it("saves an edited device profile through the local connector", async () => {
    const user = userEvent.setup();
    render(<ConnectorConfigurationPanel canManage connectorUrl="http://localhost:9100" />);

    const hostInput = await screen.findByDisplayValue("192.168.1.50");
    await user.clear(hostInput);
    await user.type(hostInput, "10.0.0.8");
    await user.click(screen.getByRole("button", { name: /save device profile/i }));

    await waitFor(() => expect(updateConfigMock).toHaveBeenCalled());
    const [url, payload] = updateConfigMock.mock.calls[0];
    expect(url).toBe("http://localhost:9100");
    expect(payload.printer.host).toBe("10.0.0.8");
    expect(announceMock).toHaveBeenCalled();
  });
});
