import { apiRequest } from "@/lib/api-client";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

type ApiPath = `/${string}`;

interface BackendPage<T> {
  content: T[];
  totalPages: number;
}

export interface PharmacyNotification {
  id: string;
  title: string;
  message: string;
  type: "LOW_STOCK" | "EXPIRY_WARNING" | "SALE_COMPLETED" | "SHIFT_REMINDER" | "SYSTEM_ALERT";
  status: "UNREAD" | "READ" | "DISMISSED";
  referenceId: string | null;
  referenceType: string | null;
  branchId: string;
  createdAt: string;
}

interface NotificationGateway {
  dismiss(id: string): Promise<void>;
  list(branchId: string, unreadOnly?: boolean): Promise<PharmacyNotification[]>;
  markRead(id: string): Promise<PharmacyNotification>;
}

function path(value: string) {
  return value as ApiPath;
}

class LiveNotificationGateway implements NotificationGateway {
  async dismiss(id: string) {
    await apiRequest<void>(path(`/notifications/${id}/dismiss`), { method: "PATCH" });
  }

  async list(branchId: string, unreadOnly = false) {
    const endpoint = `/notifications?branchId=${encodeURIComponent(branchId)}&unreadOnly=${unreadOnly}&size=50&sort=createdAt,desc`;
    const response = await apiRequest<BackendPage<PharmacyNotification>>(path(endpoint), {
      cache: "no-store",
    });
    return response.data.content;
  }

  async markRead(id: string) {
    return (await apiRequest<PharmacyNotification>(path(`/notifications/${id}/read`), {
      method: "PATCH",
    })).data;
  }
}

const PREVIEW_KEY = "pharmacy-pos:notifications-preview";

function previewRows(): PharmacyNotification[] {
  if (typeof window === "undefined") return [];
  const stored = window.localStorage.getItem(PREVIEW_KEY);
  if (stored) {
    try {
      return JSON.parse(stored) as PharmacyNotification[];
    } catch {
      // Replace malformed preview data with the default notification.
    }
  }
  const rows: PharmacyNotification[] = [{
    branchId: "preview-main",
    createdAt: new Date().toISOString(),
    id: "notification-preview-stock",
    message: "Amoxicillin 500 mg has reached its reorder level.",
    referenceId: "medicine-amoxicillin",
    referenceType: "MEDICINE",
    status: "UNREAD",
    title: "Low stock",
    type: "LOW_STOCK",
  }];
  window.localStorage.setItem(PREVIEW_KEY, JSON.stringify(rows));
  return rows;
}

function savePreview(rows: PharmacyNotification[]) {
  window.localStorage.setItem(PREVIEW_KEY, JSON.stringify(rows));
}

class PreviewNotificationGateway implements NotificationGateway {
  async dismiss(id: string) {
    const rows = previewRows();
    const row = rows.find((candidate) => candidate.id === id);
    if (row) row.status = "DISMISSED";
    savePreview(rows);
  }

  async list(branchId: string, unreadOnly = false) {
    return previewRows().filter((row) =>
      row.branchId === branchId && row.status !== "DISMISSED"
        && (!unreadOnly || row.status === "UNREAD"),
    );
  }

  async markRead(id: string) {
    const rows = previewRows();
    const row = rows.find((candidate) => candidate.id === id);
    if (!row) throw new Error("Notification not found.");
    row.status = "READ";
    savePreview(rows);
    return row;
  }
}

export const notificationGateway: NotificationGateway = DEMO_AUTH_ENABLED
  ? new PreviewNotificationGateway()
  : new LiveNotificationGateway();
