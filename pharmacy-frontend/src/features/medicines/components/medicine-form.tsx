"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import type { Resolver } from "react-hook-form";
import { ArrowLeft, Save } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { PrimaryButton, SecondaryLink } from "@/components/ui/buttons";
import {
  Field,
  FormError,
  Input,
  Select,
} from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";

const medicineSchema = z.object({
  sku: z.string().trim().min(2, "Enter a SKU."),
  barcode: z
    .string()
    .trim()
    .refine(
      (value) => value === "" || value.length >= 4,
      "Enter at least 4 characters or leave this blank.",
    ),
  brandName: z.string().trim().min(2, "Enter the brand name."),
  genericName: z.string().trim().min(2, "Enter the generic name."),
  categoryId: z.string().min(1, "Choose a category."),
  unitId: z.string().min(1, "Choose a dispensing unit."),
  buyingUnitId: z.string().nullable().default(null),
  packSize: z.number().int().min(1).nullable().default(null),
  manufacturer: z.string().trim().min(2, "Enter the manufacturer."),
  taxCategory: z.enum(["EXEMPT", "VAT_16", "ZERO_RATED"]),
  prescriptionRequired: z.boolean(),
  controlledDrug: z.boolean(),
  buyingPrice: z
    .string()
    .trim()
    .regex(/^\d+(\.\d{1,2})?$/, "Enter a valid amount."),
  sellingPrice: z
    .string()
    .trim()
    .regex(/^\d+(\.\d{1,2})?$/, "Enter a valid amount."),
  reorderLevel: z.number().int().min(0, "Use zero or a positive quantity."),
  status: z.enum(["ACTIVE", "INACTIVE"]),
});

type MedicineFormValues = z.infer<typeof medicineSchema>;

export function MedicineForm({ medicineId }: { medicineId?: string }) {
  const router = useRouter();
  const categories = useWorkspaceQuery((state) => state.categories);
  const manufacturers = useWorkspaceQuery((state) => state.manufacturers);
  const taxCategories = useWorkspaceQuery((state) => state.taxCategories);
  const units = useWorkspaceQuery((state) => state.units);
  const medicine = useWorkspaceQuery((state) =>
    state.medicines.find((candidate) => candidate.id === medicineId),
  );
  const canWrite = usePermission(PERMISSIONS.MEDICINE_WRITE);
  const canSetPrice = usePermission(PERMISSIONS.MEDICINE_PRICE_WRITE);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
    getValues,
    setValue,
  } = useForm<MedicineFormValues>({
    resolver: zodResolver(medicineSchema) as Resolver<MedicineFormValues>,
    defaultValues: medicine
      ? {
          sku: medicine.sku,
          barcode: medicine.barcode,
          brandName: medicine.brandName,
          genericName: medicine.genericName,
          categoryId: medicine.categoryId,
          unitId: medicine.unitId,
          buyingUnitId: medicine.buyingUnitId ?? null,
          packSize: medicine.packSize ?? null,
          manufacturer: medicine.manufacturer,
          taxCategory: medicine.taxCategory,
          prescriptionRequired: medicine.prescriptionRequired,
          controlledDrug: medicine.controlledDrug ?? false,
          buyingPrice: medicine.buyingPrice,
          sellingPrice: medicine.sellingPrice,
          reorderLevel: medicine.reorderLevel,
          status: medicine.status,
        }
      : {
          sku: "",
          barcode: "",
          brandName: "",
          genericName: "",
          categoryId: categories[0]?.id ?? "",
          unitId: units[0]?.id ?? "",
          buyingUnitId: null,
          packSize: null,
          manufacturer: manufacturers[0]?.name ?? "",
          taxCategory: "EXEMPT",
          prescriptionRequired: false,
          controlledDrug: false,
          buyingPrice: "",
          sellingPrice: "",
          reorderLevel: 10,
          status: "ACTIVE",
        },
  });

  useEffect(() => {
    if (medicine) return;
    if (!getValues("categoryId") && categories[0]) {
      setValue("categoryId", categories[0].id);
    }
    if (!getValues("unitId") && units[0]) {
      setValue("unitId", units[0].id);
    }
    if (!getValues("manufacturer") && manufacturers[0]) {
      setValue("manufacturer", manufacturers[0].name);
    }
    if (taxCategories[0]) {
      const selected = taxCategories.some(
        (tax) => tax.code === getValues("taxCategory"),
      );
      if (!selected) setValue("taxCategory", taxCategories[0].code);
    }
  }, [categories, getValues, manufacturers, medicine, setValue, taxCategories, units]);

  async function onSubmit(values: MedicineFormValues) {
    setSubmitError(null);
    if (!canWrite || (!medicineId && !canSetPrice)) {
      setSubmitError("Your active roles do not permit this catalogue change.");
      return;
    }
    if (
      medicine &&
      !canSetPrice &&
      (values.buyingPrice !== medicine.buyingPrice ||
        values.sellingPrice !== medicine.sellingPrice)
    ) {
      setSubmitError("Your active roles do not permit price changes.");
      return;
    }
    try {
      if (medicineId) {
        await workspaceGateway.updateMedicine(medicineId, values);
        router.push(`/medicines/${medicineId}?saved=1`);
      } else {
        const createdId = await workspaceGateway.addMedicine(values);
        router.push(`/medicines/${createdId}?created=1`);
      }
    } catch (error) {
      setSubmitError(
        getWorkspaceErrorMessage(error, "The medicine could not be saved."),
      );
    }
  }

  if (medicineId && !medicine) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6">
        <h1 className="text-lg font-semibold">Medicine not found</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          This record may have been removed or the preview data was reset.
        </p>
        <SecondaryLink href="/medicines" className="mt-4">
          <ArrowLeft aria-hidden="true" size={17} />
          Back to medicines
        </SecondaryLink>
      </div>
    );
  }

  if (!canWrite || (!medicineId && !canSetPrice)) {
    return <AccessRestricted homePath="/medicines" />;
  }

  return (
    <div className="max-w-5xl">
      <PageHeader
        eyebrow="Medicine catalogue"
        title={medicine ? `Edit ${medicine.brandName}` : "Add medicine"}
        description="Set the sale identity, pricing, dispensing rules, and stock threshold."
      />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <FormError message={submitError} />

        <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
          <h2 className="text-base font-semibold">Identity</h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Field label="Brand name" required error={errors.brandName?.message}>
              <Input autoFocus {...register("brandName")} />
            </Field>
            <Field label="Generic name" required error={errors.genericName?.message}>
              <Input {...register("genericName")} />
            </Field>
            <Field label="Manufacturer" required error={errors.manufacturer?.message}>
              <Select {...register("manufacturer")}>
                {manufacturers.map((manufacturer) => (
                  <option key={manufacturer.id} value={manufacturer.name}>
                    {manufacturer.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="SKU" required error={errors.sku?.message}>
              <Input autoCapitalize="characters" {...register("sku")} />
            </Field>
            <Field
              label="Barcode"
              error={errors.barcode?.message}
              hint="Scan the retail pack when available. Supplier product codes are stored separately."
            >
              <Input inputMode="numeric" {...register("barcode")} />
            </Field>
            <Field label="Status" required error={errors.status?.message}>
              <Select {...register("status")}>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </Select>
            </Field>
          </div>
        </section>

        <section className="rounded-md border border-[var(--border)] bg-white p-4 sm:p-6">
          <h2 className="text-base font-semibold">Dispensing and pricing</h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Field label="Category" required error={errors.categoryId?.message}>
              <Select {...register("categoryId")}>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Dispensing unit" required error={errors.unitId?.message}>
              <Select {...register("unitId")}>
                {units.map((unit) => (
                  <option key={unit.id} value={unit.id}>
                    {unit.name} ({unit.symbol})
                  </option>
                ))}
              </Select>
            </Field>
            <Field
              label="Buying unit"
              hint="Unit used by suppliers (e.g. Box, Strip). Leave blank if same as dispensing."
              error={errors.buyingUnitId?.message}
            >
              <Select {...register("buyingUnitId")}>
                <option value="">Same as dispensing unit</option>
                {units.map((unit) => (
                  <option key={unit.id} value={unit.id}>
                    {unit.name} ({unit.symbol})
                  </option>
                ))}
              </Select>
            </Field>
            <Field
              label="Pack size"
              hint="How many dispensing units in one buying unit (e.g. 100 for a box of 100 tablets)."
              error={errors.packSize?.message}
            >
              <Input
                type="number"
                min={1}
                step={1}
                placeholder="e.g. 100"
                {...register("packSize", { valueAsNumber: true })}
              />
            </Field>
            <Field label="Tax category" required error={errors.taxCategory?.message}>
              <Select {...register("taxCategory")}>
                {taxCategories.map((tax) => (
                  <option key={tax.id} value={tax.code}>
                    {tax.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Buying price (KES)" required error={errors.buyingPrice?.message}>
              <Input inputMode="decimal" placeholder="0.00" readOnly={!canSetPrice} {...register("buyingPrice")} />
            </Field>
            <Field label="Selling price (KES)" required error={errors.sellingPrice?.message}>
              <Input inputMode="decimal" placeholder="0.00" readOnly={!canSetPrice} {...register("sellingPrice")} />
            </Field>
            <Field
              label="Reorder level"
              required
              error={errors.reorderLevel?.message}
              hint="An alert appears when usable stock reaches this quantity."
            >
              <Input
                type="number"
                min={0}
                step={1}
                {...register("reorderLevel", { valueAsNumber: true })}
              />
            </Field>
          </div>
          <label className="mt-5 flex items-start gap-3 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 size-4 accent-[var(--brand)]"
              {...register("prescriptionRequired")}
            />
            <span>
              <span className="block font-medium">Prescription medicine</span>
              <span className="mt-0.5 block text-xs text-[var(--text-muted)]">
                Checkout requires a pharmacist approval confirmation.
              </span>
            </span>
          </label>
          <label className="mt-3 flex items-start gap-3 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 size-4 accent-[var(--brand)]"
              {...register("controlledDrug")}
            />
            <span>
              <span className="block font-medium">Controlled drug</span>
              <span className="mt-0.5 block text-xs text-[var(--text-muted)]">
                Dispensing is recorded in the controlled drugs register.
              </span>
            </span>
          </label>
        </section>

        <div className="flex flex-wrap justify-end gap-2">
          <SecondaryLink href="/medicines">Cancel</SecondaryLink>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            <Save aria-hidden="true" size={17} />
            {medicine ? "Save changes" : "Add medicine"}
          </PrimaryButton>
        </div>
      </form>
    </div>
  );
}
