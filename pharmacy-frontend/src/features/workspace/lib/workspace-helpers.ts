import type { Batch, Medicine } from "@/features/workspace/types";
import { moneyToCents } from "@/features/workspace/lib/money";

export function todayIsoDate() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Africa/Nairobi",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

export function availableBatches(batches: Batch[], medicineId: string) {
  const today = todayIsoDate();
  return batches
    .filter(
      (batch) =>
        batch.medicineId === medicineId &&
        batch.quantity > 0 &&
        batch.expiryDate > today,
    )
    .sort((left, right) => left.expiryDate.localeCompare(right.expiryDate));
}

export function stockForMedicine(batches: Batch[], medicineId: string) {
  return availableBatches(batches, medicineId).reduce(
    (total, batch) => total + batch.quantity,
    0,
  );
}

export function stockValue(medicines: Medicine[], batches: Batch[]) {
  const medicineIds = new Set(medicines.map((medicine) => medicine.id));
  const today = todayIsoDate();
  return batches
    .filter(
      (batch) =>
        medicineIds.has(batch.medicineId) &&
        batch.quantity > 0 &&
        batch.expiryDate > today,
    )
    .reduce(
      (total, batch) => total + moneyToCents(batch.unitCost) * batch.quantity,
      0,
    );
}

export function daysUntil(date: string) {
  const target = new Date(`${date}T00:00:00Z`).getTime();
  const today = new Date(`${todayIsoDate()}T00:00:00Z`).getTime();
  return Math.ceil((target - today) / 86_400_000);
}
