"use client";

import {
  ArrowLeft,
  CheckCircle2,
  Printer,
  RotateCcw,
  ShoppingCart,
} from "lucide-react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import {
  PrimaryButton,
  PrimaryLink,
  SecondaryButton,
  SecondaryLink,
} from "@/components/ui/buttons";
import { Field, FormError, Input, Select } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { PERMISSIONS } from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import { addMoney, formatKes, multiplyMoney } from "@/features/workspace/lib/money";
import type { PaymentMethod } from "@/features/workspace/types";
import { cn } from "@/lib/cn";
import { formatDateTime } from "@/lib/format";
import { ApiClientError } from "@/lib/api-client";
import { uuid } from "../../../lib/uuid";

export function SaleDetailPage() {
  const params = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const sales = useWorkspaceQuery((state) => state.sales);
  const settings = useWorkspaceQuery((state) => state.settings);
  const loadStatus = useWorkspaceQuery((state) => state.loadStatus);
  const canReturnSale = usePermission(PERMISSIONS.SALE_RETURN);
  const canReprintReceipt = usePermission(PERMISSIONS.SALE_RECEIPT_REPRINT);
  const canSell = usePermission(PERMISSIONS.POS_SELL);
  const sale = sales.find((candidate) => candidate.id === params.id);
  const canReturnStatus =
    sale?.status === "COMPLETED" || sale?.status === "PARTIALLY_RETURNED";
  const returnableItems = canReturnStatus
    ? sale?.items.filter((item) => item.returnedQuantity < item.quantity) ?? []
    : [];
  const [showReturn, setShowReturn] = useState(false);
  const [saleItemId, setSaleItemId] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [reason, setReason] = useState("Customer returned item");
  const [refundMethod, setRefundMethod] = useState<PaymentMethod | "">("");
  const [refundReference, setRefundReference] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [printedAt, setPrintedAt] = useState<string | null>(null);
  const [returnIdempotencyKey, setReturnIdempotencyKey] = useState(() =>
    uuid(),
  );
  const selectedSaleItemId = returnableItems.some((item) => item.id === saleItemId)
    ? saleItemId
    : returnableItems[0]?.id || "";
  const selectedRefundMethod =
    refundMethod || sale?.payments[0]?.method || "CASH";
  const selectedItem = sale?.items.find((item) => item.id === selectedSaleItemId);
  const maxReturn = selectedItem
    ? selectedItem.quantity - selectedItem.returnedQuantity
    : 0;
  const canPrintReceipt =
    canReprintReceipt || (canSell && searchParams.get("completed") === "1");
  const autoPrintStarted = useRef(false);

  useEffect(() => {
    if (!sale || searchParams.get("autoprint") !== "1" || autoPrintStarted.current) {
      return;
    }
    autoPrintStarted.current = true;
    const timer = window.setTimeout(() => {
      window.print();
      setPrintedAt(new Date().toISOString());
    }, 350);
    return () => window.clearTimeout(timer);
  }, [sale, searchParams]);

  function printReceipt() {
    window.print();
    setPrintedAt(new Date().toISOString());
  }

  if (!sale && (loadStatus === "idle" || loadStatus === "loading")) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6">
        <h1 className="text-lg font-semibold">Loading receipt</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          Fetching the transaction from the pharmacy node.
        </p>
      </div>
    );
  }

  if (!sale) {
    return (
      <div className="rounded-md border border-[var(--border)] bg-white p-6">
        <h1 className="text-lg font-semibold">Receipt not found</h1>
        <p className="mt-1 text-sm text-[var(--text-muted)]">
          This receipt is not available in the active branch.
        </p>
        <SecondaryLink href="/sales" className="mt-4">
          <ArrowLeft aria-hidden="true" size={17} />
          Back to sales
        </SecondaryLink>
      </div>
    );
  }

  async function handleReturn(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!canReturnSale) {
      setError("Your active roles do not permit sale returns.");
      return;
    }
    try {
      await workspaceGateway.returnSaleItem({
        idempotencyKey: returnIdempotencyKey,
        quantity,
        reason,
        refundMethod: selectedRefundMethod,
        refundReference,
        resalable: false,
        saleId: params.id,
        saleItemId: selectedSaleItemId,
      });
      setReturnIdempotencyKey(uuid());
      setSuccess(
        `${quantity} item${quantity === 1 ? "" : "s"} returned. Refund due: ${formatKes(
          multiplyMoney(selectedItem?.unitPrice ?? "0.00", quantity),
        )}.`,
      );
      setShowReturn(false);
      setSaleItemId("");
      setRefundReference("");
    } catch (caught) {
      if (!(caught instanceof ApiClientError) || caught.status !== 0) {
        setReturnIdempotencyKey(uuid());
      }
      setError(
        getWorkspaceErrorMessage(caught, "The return could not be recorded."),
      );
    }
  }

  const isReprint = searchParams.get("completed") !== "1";
  const itemsCount = sale.items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <div>
      <div className="print:hidden">
        <PageHeader
          eyebrow={isReprint ? "Receipt copy" : "Sales receipt"}
          title={sale.receiptNumber}
          description={`Completed ${formatDateTime(sale.completedAt)} by ${sale.cashierName}.`}
          actions={
            <>
              {canSell ? (
                <PrimaryLink href="/pos">
                  <ShoppingCart aria-hidden="true" size={17} />
                  New sale
                </PrimaryLink>
              ) : null}
              {canPrintReceipt ? (
                <SecondaryButton type="button" onClick={printReceipt}>
                  <Printer aria-hidden="true" size={17} />
                  {printedAt ? "Print again" : "Print receipt"}
                </SecondaryButton>
              ) : null}
              {isReprint && canPrintReceipt ? (
                <span className="self-center text-xs font-semibold text-[var(--warning)]">
                  Copies are watermarked COPY
                </span>
              ) : null}
              {returnableItems.length > 0 && canReturnSale ? (
                <PrimaryButton
                  type="button"
                  onClick={() => setShowReturn((open) => !open)}
                >
                  <RotateCcw aria-hidden="true" size={17} />
                  Return item
                </PrimaryButton>
              ) : null}
            </>
          }
        />
        {searchParams.get("completed") === "1" ? (
          <div className="mb-6 flex items-center gap-3 rounded-md border border-[var(--border)] bg-[var(--success-soft)] p-3 text-sm text-[var(--success)]">
            <CheckCircle2 aria-hidden="true" size={18} />
            <span>
              <strong>Sale completed.</strong> Stock and shift totals have been
              updated.
              {canPrintReceipt ? (
                printedAt ? (
                  <span className="block text-xs text-[var(--text-muted)]">
                    Receipt printed at {formatDateTime(printedAt)}.
                  </span>
                ) : (
                  <span className="block text-xs text-[var(--text-muted)]">
                    Receipt not printed yet — use the print button above.
                  </span>
                )
              ) : null}
            </span>
          </div>
        ) : null}
        {success ? (
          <div
            role="status"
            className="mb-6 rounded-md bg-[var(--success-soft)] p-3 text-sm text-[var(--success)]"
          >
            {success}
          </div>
        ) : null}
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px] print:block">
        <section
          className={cn(
            "receipt-print relative mx-auto w-full max-w-2xl overflow-hidden rounded-md border border-[var(--border)] bg-white p-5 shadow-sm sm:p-8 print:rounded-none print:border-0 print:shadow-none",
            settings.receiptPaperWidth === "58MM"
              ? "receipt-print--58mm"
              : "receipt-print--80mm",
          )}
        >
          {isReprint ? (
            <span
              aria-hidden="true"
              className="pointer-events-none absolute inset-0 flex select-none items-center justify-center text-[6rem] font-black uppercase tracking-widest text-[var(--border)]/60 -rotate-12"
            >
              Copy
            </span>
          ) : null}
          <div className="relative text-center">
            <h2 className="text-lg font-bold">{settings.pharmacyName}</h2>
            {settings.address ? (
              <p className="text-sm">{settings.address}</p>
            ) : null}
            <p className="mt-1 text-sm">{settings.branchName}</p>
            {settings.kraPin ? (
              <p className="text-xs">PIN: {settings.kraPin}</p>
            ) : null}
            {settings.phone ? (
              <p className="text-xs text-[var(--text-muted)]">{settings.phone}</p>
            ) : null}
          </div>
          {isReprint ? (
            <div className="relative mt-3 border-y border-dashed border-[var(--warning)] py-1.5 text-center">
              <p className="text-base font-black uppercase text-[var(--warning)]" style={{ fontSize: "1.5rem" }}>
                Copy
              </p>
              <p className="text-xs font-bold uppercase">This is not an official receipt</p>
            </div>
          ) : null}
          {sale.status !== "COMPLETED" ? (
            <p className="relative mt-3 border-y border-dashed border-[var(--danger)] py-1.5 text-center text-xs font-bold text-[var(--danger)]">
              {sale.status.replaceAll("_", " ")}
            </p>
          ) : null}
          <div className="relative my-5 border-y border-dashed border-[var(--border-strong)] py-3 text-xs">
            <div className="flex justify-between gap-4">
              <span>Receipt</span>
              <strong>{sale.receiptNumber}</strong>
            </div>
            <div className="mt-1 flex justify-between gap-4">
              <span>Date</span>
              <span>{formatDateTime(sale.completedAt)}</span>
            </div>
            <div className="mt-1 flex justify-between gap-4">
              <span>Cashier</span>
              <span>{sale.cashierName}</span>
            </div>
            {sale.customerKraPin ? (
              <div className="mt-1 flex justify-between gap-4">
                <span>Buyer PIN</span>
                <span>{sale.customerKraPin}</span>
              </div>
            ) : null}
          </div>
          <table className="relative w-full text-sm">
            <thead className="border-b border-[var(--border)] text-left text-xs text-[var(--text-muted)]">
              <tr>
                <th className="pb-2 font-semibold">Item</th>
                <th className="pb-2 text-right font-semibold">Qty</th>
                <th className="pb-2 text-right font-semibold">Price</th>
                <th className="pb-2 text-right font-semibold">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {sale.items.map((item) => (
                <tr key={item.id}>
                  <td className="py-3">
                    <p className="font-medium">{item.medicineName}</p>
                    {item.returnedQuantity ? (
                      <p className="text-xs text-[var(--danger)]">
                        {item.returnedQuantity} returned
                      </p>
                    ) : (
                      <p className="text-xs text-[var(--text-muted)]">
                        Batch {item.allocations.map((allocation) => allocation.batchNumber).join(", ")}
                      </p>
                    )}
                  </td>
                  <td className="py-3 text-right">{item.quantity}</td>
                  <td className="py-3 text-right">{formatKes(item.unitPrice)}</td>
                  <td className="py-3 text-right font-medium">
                    {formatKes(item.lineTotal)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="relative ml-auto mt-5 max-w-xs space-y-2 text-sm">
            <div className="flex justify-between gap-6">
              <span className="text-[var(--text-muted)]">Subtotal</span>
              <span>{formatKes(sale.subtotal)}</span>
            </div>
            <div className="flex justify-between gap-6">
              <span className="text-[var(--text-muted)]">Included tax</span>
              <span>{formatKes(sale.taxTotal)}</span>
            </div>
            {sale.refundTotal !== "0.00" ? (
              <div className="flex justify-between gap-6 text-[var(--danger)]">
                <span>Refunds</span>
                <span>-{formatKes(sale.refundTotal)}</span>
              </div>
            ) : null}
            <div className="flex justify-between gap-6 border-t border-[var(--border)] pt-2 text-base font-bold">
              <span>Net total</span>
              <span>{formatKes(addMoney(sale.total, `-${sale.refundTotal}`))}</span>
            </div>
            <div className="flex justify-between gap-6 text-xs text-[var(--text-muted)]">
              <span>Items number</span>
              <span>{itemsCount}</span>
            </div>
          </div>
          <div className="relative mt-6 border-y border-dashed border-[var(--border-strong)] py-3 text-sm">
            {sale.payments.map((payment, index) => (
              <div className={index ? "mt-2" : ""} key={`${payment.method}-${index}`}>
                <div className="flex justify-between gap-4">
                  <span>{payment.method === "MPESA" ? "M-Pesa" : "Cash"}</span>
                  <strong>{formatKes(payment.amount)}</strong>
                </div>
                {payment.reference ? (
                  <div className="mt-1 flex justify-between gap-4 text-xs">
                    <span>Reference</span>
                    <span className="font-mono">{payment.reference}</span>
                  </div>
                ) : null}
              </div>
            ))}
            {sale.cashTendered != null && sale.changeDue != null ? (
              <div className="mt-3 flex justify-between gap-4 text-xs text-[var(--text-muted)]">
                <span>Cash tendered</span>
                <span className="font-mono">{formatKes(sale.cashTendered)}</span>
              </div>
            ) : null}
            {sale.cashTendered != null && sale.changeDue != null && Number(sale.changeDue) > 0 ? (
              <div className="mt-1 flex justify-between gap-4 font-semibold">
                <span>Change due</span>
                <span>{formatKes(sale.changeDue)}</span>
              </div>
            ) : null}
          </div>
          <p className="relative mt-6 text-center text-xs text-[var(--text-muted)]">
            {settings.receiptFooter}
          </p>
        </section>

        <aside className="print:hidden">
          {showReturn ? (
            <form
              onSubmit={handleReturn}
              className="rounded-md border border-[var(--border)] bg-white p-5"
            >
              <h2 className="text-base font-semibold">Record item return</h2>
              <p className="mt-1 text-sm text-[var(--text-muted)]">
                Returned medicine is quarantined and is not added to saleable stock.
              </p>
              <div className="mt-5 space-y-4">
                <Field label="Item" required>
                  <Select
                    value={selectedSaleItemId}
                    onChange={(event) => {
                      setSaleItemId(event.target.value);
                      setQuantity(1);
                    }}
                  >
                    {returnableItems.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.medicineName} / {item.quantity - item.returnedQuantity} available
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Quantity" required>
                  <Input
                    type="number"
                    min={1}
                    max={maxReturn}
                    step={1}
                    value={quantity}
                    onChange={(event) => setQuantity(Number(event.target.value))}
                  />
                </Field>
                <Field label="Reason" required>
                  <Select value={reason} onChange={(event) => setReason(event.target.value)}>
                    <option>Customer returned item</option>
                    <option>Wrong item dispensed</option>
                    <option>Damaged packaging</option>
                    <option>Product quality concern</option>
                  </Select>
                </Field>
                <Field label="Refund method" required>
                  <Select
                    value={selectedRefundMethod}
                    onChange={(event) =>
                      setRefundMethod(event.target.value as PaymentMethod)
                    }
                  >
                    <option value="CASH">Cash</option>
                    <option value="MPESA">M-Pesa</option>
                  </Select>
                </Field>
                {selectedRefundMethod === "MPESA" ? (
                  <Field label="M-Pesa refund reference" required>
                    <Input
                      autoCapitalize="characters"
                      value={refundReference}
                      onChange={(event) =>
                        setRefundReference(event.target.value.toUpperCase())
                      }
                    />
                  </Field>
                ) : null}
                <div className="flex justify-between border-y border-[var(--border)] py-3 text-sm">
                  <span className="text-[var(--text-muted)]">Refund due</span>
                  <strong>
                    {formatKes(
                      multiplyMoney(
                        selectedItem?.unitPrice ?? "0.00",
                        Number.isFinite(quantity) ? quantity : 0,
                      ),
                    )}
                  </strong>
                </div>
                <FormError message={error} />
                <PrimaryButton
                  type="submit"
                  disabled={
                    !selectedSaleItemId ||
                    quantity < 1 ||
                    quantity > maxReturn ||
                    (selectedRefundMethod === "MPESA" && !refundReference.trim())
                  }
                  className="w-full"
                >
                  <RotateCcw aria-hidden="true" size={17} />
                  Confirm return
                </PrimaryButton>
              </div>
            </form>
          ) : (
            <div className="rounded-md border border-[var(--border)] bg-white p-5">
              <h2 className="text-sm font-semibold">Transaction state</h2>
              <div className="mt-3">
                <StatusBadge
                  tone={
                    sale.status === "COMPLETED"
                      ? "success"
                      : sale.status === "RETURNED"
                        ? "danger"
                        : "warning"
                  }
                >
                  {sale.status.replaceAll("_", " ").toLowerCase()}
                </StatusBadge>
              </div>
              <p className="mt-3 text-sm text-[var(--text-muted)]">
                {sale.refundTotal === "0.00"
                  ? "No returns have been recorded on this receipt."
                  : `${formatKes(sale.refundTotal)} has been refunded.`}
              </p>
            </div>
          )}
          <Link
            href="/sales"
            className="mt-4 flex items-center gap-2 text-sm font-semibold text-[var(--brand-strong)] hover:underline"
          >
            <ArrowLeft aria-hidden="true" size={16} />
            Back to sales
          </Link>
        </aside>
      </div>
    </div>
  );
}
