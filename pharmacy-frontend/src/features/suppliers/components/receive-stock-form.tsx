"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { PackagePlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { PrimaryButton, SecondaryLink } from "@/components/ui/buttons";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import { multiplyMoney, formatKes } from "@/features/workspace/lib/money";
import { todayIsoDate } from "@/features/workspace/lib/workspace-helpers";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { ApiClientError } from "@/lib/api-client";

const receiveSchema = z.object({
  supplierId: z.string().min(1, "Choose a supplier."),
  medicineId: z.string().min(1, "Choose a medicine."),
  batchNumber: z.string().trim().min(2, "Enter the supplier batch number."),
  expiryDate: z.string().min(1, "Choose an expiry date."),
  quantity: z.number().int().positive("Quantity must be at least one."),
  unitCost: z.string().trim().regex(/^\d+(\.\d{1,2})?$/, "Enter a valid amount."),
  supplierInvoiceNumber: z.string().trim().max(80),
  remarks: z.string().trim().max(250),
}).refine((values) => values.expiryDate > todayIsoDate(), {
  path: ["expiryDate"],
  message: "Expiry must be after today.",
});

type ReceiveFormValues = z.infer<typeof receiveSchema>;

export function ReceiveStockForm() {
  const router = useRouter();
  const medicines = useWorkspaceQuery((state) => state.medicines);
  const suppliers = useWorkspaceQuery((state) => state.suppliers);
  const canReceiveStock = usePermission(PERMISSIONS.INVENTORY_RECEIVE);
  const actor = useAuthStore((state) => state.session?.user.displayName ?? "Pharmacy user");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());
  const {
    control,
    formState: { errors, isSubmitting },
    getValues,
    handleSubmit,
    register,
    setValue,
  } = useForm<ReceiveFormValues>({
    resolver: zodResolver(receiveSchema),
    defaultValues: {
      supplierId: suppliers[0]?.id ?? "",
      medicineId: medicines[0]?.id ?? "",
      batchNumber: "",
      expiryDate: "",
      quantity: 1,
      unitCost: medicines[0]?.buyingPrice ?? "",
      supplierInvoiceNumber: "",
      remarks: "",
    },
  });
  useEffect(() => {
    const activeSupplier = suppliers.find((supplier) => supplier.status === "ACTIVE");
    const activeMedicine = medicines.find((medicine) => medicine.status === "ACTIVE");
    if (!getValues("supplierId") && activeSupplier) {
      setValue("supplierId", activeSupplier.id);
    }
    if (!getValues("medicineId") && activeMedicine) {
      setValue("medicineId", activeMedicine.id);
      setValue("unitCost", activeMedicine.buyingPrice);
    }
  }, [getValues, medicines, setValue, suppliers]);
  const quantity = useWatch({ control, name: "quantity" });
  const unitCost = useWatch({ control, name: "unitCost" });
  const estimatedTotal = Number.isInteger(quantity) && quantity > 0 && /^\d+(\.\d{1,2})?$/.test(unitCost ?? "")
    ? formatKes(multiplyMoney(unitCost, quantity))
    : "KES 0.00";

  async function onSubmit(values: ReceiveFormValues) {
    setSubmitError(null);
    if (!canReceiveStock) {
      setSubmitError("Your active roles do not permit stock receiving.");
      return;
    }
    try {
      const grn = await workspaceGateway.receiveStock(
        { ...values, idempotencyKey },
        actor,
      );
      setIdempotencyKey(crypto.randomUUID());
      router.push(`/inventory?received=${encodeURIComponent(grn)}`);
    } catch (error) {
      if (!(error instanceof ApiClientError) || error.status !== 0) {
        setIdempotencyKey(crypto.randomUUID());
      }
      setSubmitError(
        getWorkspaceErrorMessage(error, "Stock could not be received."),
      );
    }
  }

  if (!canReceiveStock) {
    return <AccessRestricted homePath="/inventory" />;
  }

  return (
    <div className="max-w-4xl">
      <PageHeader
        eyebrow="Goods received note"
        title="Receive stock"
        description="Add one medicine batch. The system records its supplier, cost, expiry, and stock movement."
      />
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <FormError message={submitError} />
        <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Supplier" required error={errors.supplierId?.message}>
              <Select {...register("supplierId")}>
                {suppliers.filter((supplier) => supplier.status === "ACTIVE").map((supplier) => <option key={supplier.id} value={supplier.id}>{supplier.name}</option>)}
              </Select>
            </Field>
            <Field label="Medicine" required error={errors.medicineId?.message}>
              <Select {...register("medicineId")}>
                {medicines.filter((medicine) => medicine.status === "ACTIVE").map((medicine) => <option key={medicine.id} value={medicine.id}>{medicine.brandName} · {medicine.sku}</option>)}
              </Select>
            </Field>
            <Field label="Batch number" required error={errors.batchNumber?.message}>
              <Input autoFocus autoCapitalize="characters" {...register("batchNumber")} />
            </Field>
            <Field label="Expiry date" required error={errors.expiryDate?.message}>
              <Input type="date" min={todayIsoDate()} {...register("expiryDate")} />
            </Field>
            <Field label="Quantity received" required error={errors.quantity?.message}>
              <Input type="number" min={1} step={1} {...register("quantity", { valueAsNumber: true })} />
            </Field>
            <Field label="Unit cost (KES)" required error={errors.unitCost?.message}>
              <Input inputMode="decimal" placeholder="0.00" {...register("unitCost")} />
            </Field>
            <Field
              label="Supplier invoice number"
              error={errors.supplierInvoiceNumber?.message}
            >
              <Input autoCapitalize="characters" {...register("supplierInvoiceNumber")} />
            </Field>
            <Field label="Receiving notes" error={errors.remarks?.message}>
              <Input {...register("remarks")} />
            </Field>
          </div>
          <div className="mt-5 flex items-center justify-between border-t border-[var(--border)] pt-4 text-sm">
            <span className="text-[var(--text-muted)]">Estimated GRN total</span>
            <span className="font-semibold">{estimatedTotal}</span>
          </div>
        </section>
        <div className="flex justify-end gap-2">
          <SecondaryLink href="/inventory">Cancel</SecondaryLink>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            <PackagePlus aria-hidden="true" size={17} />
            Receive batch
          </PrimaryButton>
        </div>
      </form>
    </div>
  );
}
