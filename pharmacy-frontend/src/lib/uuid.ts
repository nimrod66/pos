/**
 * Generates a RFC 4122 v4 UUID. Falls back to crypto.getRandomValues
 * when crypto.randomUUID is unavailable (non-HTTPS contexts such as
 * LAN deployments at http://192.168.x.x:3000).
 */
export function uuid(): string {
  if (
    typeof crypto !== "undefined" &&
    typeof crypto.randomUUID === "function"
  ) {
    return crypto.randomUUID();
  }
  return "10000000-1000-4000-8000-100000000000".replace(
    /[018]/g,
    (c) =>
      (
        Number(c) ^
        (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (Number(c) / 4)))
      ).toString(16),
  );
}
