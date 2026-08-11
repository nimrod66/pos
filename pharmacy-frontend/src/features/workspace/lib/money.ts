export function moneyToCents(value: string | number) {
  const normalized = String(value).replace(/,/g, "").trim();
  const match = normalized.match(/^(-?)(\d+)(?:\.(\d{0,2}))?$/);

  if (!match) {
    throw new Error(`Invalid money value: ${value}`);
  }

  const sign = match[1] === "-" ? -1 : 1;
  const whole = Number.parseInt(match[2], 10);
  const fraction = Number.parseInt((match[3] ?? "").padEnd(2, "0"), 10);
  return sign * (whole * 100 + fraction);
}

export function centsToMoney(cents: number) {
  const sign = cents < 0 ? "-" : "";
  const absolute = Math.abs(Math.round(cents));
  return `${sign}${Math.floor(absolute / 100)}.${String(absolute % 100).padStart(2, "0")}`;
}

export function addMoney(...values: Array<string | number>) {
  return centsToMoney(
    values.reduce<number>((total, value) => total + moneyToCents(value), 0),
  );
}

export function multiplyMoney(value: string, quantity: number) {
  return centsToMoney(moneyToCents(value) * quantity);
}

export function formatKes(value: string | number) {
  return new Intl.NumberFormat("en-KE", {
    style: "currency",
    currency: "KES",
    minimumFractionDigits: 2,
  }).format(moneyToCents(value) / 100);
}
