"use client";

import { AlertTriangle } from "lucide-react";
import { useEffect, useId, useRef } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";

interface ConfirmDialogProps {
  busy?: boolean;
  confirmLabel: string;
  description: string;
  onCancel(): void;
  onConfirm(): void;
  open: boolean;
  title: string;
}

export function ConfirmDialog({
  busy = false,
  confirmLabel,
  description,
  onCancel,
  onConfirm,
  open,
  title,
}: ConfirmDialogProps) {
  const titleId = useId();
  const descriptionId = useId();
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    cancelButtonRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !busy) onCancel();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busy, onCancel, open]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !busy) onCancel();
      }}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="w-full max-w-md rounded-md border border-[var(--border)] bg-white p-5 shadow-xl"
      >
        <div className="flex items-start gap-3">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-[var(--danger-soft)] text-[var(--danger)]">
            <AlertTriangle aria-hidden="true" size={18} />
          </div>
          <div>
            <h2 id={titleId} className="text-base font-semibold">
              {title}
            </h2>
            <p id={descriptionId} className="mt-1 text-sm text-[var(--text-muted)]">
              {description}
            </p>
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <SecondaryButton
            ref={cancelButtonRef}
            type="button"
            disabled={busy}
            onClick={onCancel}
          >
            Cancel
          </SecondaryButton>
          <PrimaryButton
            type="button"
            disabled={busy}
            onClick={onConfirm}
            className="bg-[var(--danger)] hover:bg-[#a9342b]"
          >
            {busy ? "Deleting..." : confirmLabel}
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
