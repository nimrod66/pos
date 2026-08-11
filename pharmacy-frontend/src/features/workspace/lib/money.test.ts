import { describe, expect, it } from "vitest";

import {
  addMoney,
  centsToMoney,
  moneyToCents,
  multiplyMoney,
} from "@/features/workspace/lib/money";

describe("money helpers", () => {
  it("keeps arithmetic in integer cents", () => {
    expect(moneyToCents("1,234.50")).toBe(123450);
    expect(centsToMoney(123450)).toBe("1234.50");
    expect(addMoney("0.10", "0.20")).toBe("0.30");
    expect(multiplyMoney("12.35", 3)).toBe("37.05");
  });

  it("supports negative adjustments", () => {
    expect(addMoney("100.00", "-24.50")).toBe("75.50");
    expect(centsToMoney(-75)).toBe("-0.75");
  });

  it("rejects values with more than two decimal places", () => {
    expect(() => moneyToCents("1.005")).toThrow("Invalid money value");
  });
});
