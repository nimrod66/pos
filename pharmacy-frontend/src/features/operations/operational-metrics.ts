import { apiRequest } from "@/lib/api-client";

export type OperationalEventType =
  | "CHECKOUT"
  | "PAYMENT"
  | "BACKUP"
  | "RESTORE"
  | "OFFLINE_QUEUE"
  | "HARDWARE"
  | "LOGIN"
  | "PERMISSION_DENIED"
  | "SYNC"
  | "INSTALLER"
  | "PILOT_VALIDATION";

export type OperationalEventStatus =
  | "ATTEMPTED"
  | "SUCCESS"
  | "FAILED"
  | "WARNING"
  | "STALE"
  | "PENDING";

export async function recordOperationalEvent(input: {
  eventType: OperationalEventType;
  status: OperationalEventStatus;
  reasonCode?: string;
  source?: string;
  terminalId?: string | null;
  resourceId?: string;
  idempotencyKey?: string;
  latencyMs?: number;
  details?: string;
}) {
  try {
    await apiRequest<void>("/operations/metrics/client-event", {
      method: "POST",
      body: input,
    });
  } catch {
    // Metrics must never interrupt checkout or hardware workflows.
  }
}
