"use client";

import { useParams } from "next/navigation";

import { MedicineForm } from "@/features/medicines/components/medicine-form";

export default function Page() {
  const params = useParams<{ id: string }>();
  return <MedicineForm medicineId={params.id} />;
}
