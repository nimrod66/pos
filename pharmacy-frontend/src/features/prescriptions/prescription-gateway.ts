import { apiRequest } from "@/lib/api-client";
import { DEMO_AUTH_ENABLED } from "@/lib/api-config";

type ApiPath = `/${string}`;

interface BackendPage<T> {
  content: T[];
  totalPages: number;
}

export interface PrescriptionItem {
  id: string;
  medicineId: string;
  medicineName: string;
  dosage: string | null;
  quantity: number;
}

export interface Prescription {
  id: string;
  branchId: string;
  approvedById: string;
  customerName: string;
  doctorName: string;
  doctorLicenseNumber: string;
  hospitalName: string | null;
  prescriptionNumber: string;
  diagnosis: string | null;
  issuedDate: string;
  status: "ACTIVE" | "DISPENSED" | "CANCELLED" | string;
  approvedAt: string;
  dispensedAt: string | null;
  items: PrescriptionItem[];
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionInput {
  customerName: string;
  doctorName: string;
  doctorLicenseNumber: string;
  hospitalName: string | null;
  prescriptionNumber: string;
  diagnosis: string | null;
  issuedDate: string;
  items: Array<{
    medicineId: string;
    dosage: string | null;
    quantity: number;
  }>;
}

interface PrescriptionGateway {
  create(input: PrescriptionInput): Promise<Prescription>;
  dispense(id: string): Promise<Prescription>;
  list(): Promise<Prescription[]>;
}

function path(value: string) {
  return value as ApiPath;
}

class LivePrescriptionGateway implements PrescriptionGateway {
  async create(input: PrescriptionInput) {
    return (await apiRequest<Prescription>("/prescriptions", {
      body: input,
      method: "POST",
    })).data;
  }

  async dispense(id: string) {
    return (await apiRequest<Prescription>(path(`/prescriptions/${id}/dispense`), {
      method: "PATCH",
    })).data;
  }

  async list() {
    const endpoint = "/prescriptions?size=100&sort=createdAt,desc";
    const first = await apiRequest<BackendPage<Prescription>>(endpoint, {
      cache: "no-store",
    });
    const rows = [...first.data.content];
    for (let page = 1; page < first.data.totalPages; page += 1) {
      const response = await apiRequest<BackendPage<Prescription>>(
        path(`${endpoint}&page=${page}`),
        { cache: "no-store" },
      );
      rows.push(...response.data.content);
    }
    return rows;
  }
}

const PREVIEW_KEY = "pharmacy-pos:prescriptions-preview";

function previewRows(): Prescription[] {
  if (typeof window === "undefined") return [];
  const stored = window.localStorage.getItem(PREVIEW_KEY);
  if (!stored) return [];
  try {
    return JSON.parse(stored) as Prescription[];
  } catch {
    return [];
  }
}

function savePreview(rows: Prescription[]) {
  window.localStorage.setItem(PREVIEW_KEY, JSON.stringify(rows));
}

class PreviewPrescriptionGateway implements PrescriptionGateway {
  async create(input: PrescriptionInput) {
    const now = new Date().toISOString();
    const prescription: Prescription = {
      ...input,
      approvedAt: now,
      approvedById: "staff-pharmacist",
      branchId: "preview-main",
      createdAt: now,
      dispensedAt: null,
      id: crypto.randomUUID(),
      items: input.items.map((item) => ({
        ...item,
        id: crypto.randomUUID(),
        medicineName: "Prescription medicine",
      })),
      status: "ACTIVE",
      updatedAt: now,
    };
    const rows = [prescription, ...previewRows()];
    savePreview(rows);
    return prescription;
  }

  async dispense(id: string) {
    const rows = previewRows();
    const prescription = rows.find((row) => row.id === id);
    if (!prescription) throw new Error("Prescription not found.");
    prescription.status = "DISPENSED";
    prescription.dispensedAt = new Date().toISOString();
    prescription.updatedAt = prescription.dispensedAt;
    savePreview(rows);
    return prescription;
  }

  async list() {
    return previewRows();
  }
}

export const prescriptionGateway: PrescriptionGateway = DEMO_AUTH_ENABLED
  ? new PreviewPrescriptionGateway()
  : new LivePrescriptionGateway();
