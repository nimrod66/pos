"use client";

import { X } from "lucide-react";
import { useEffect, useId, useRef } from "react";
import { createPortal } from "react-dom";

import { cn } from "@/lib/cn";

interface ModalProps {
  open: boolean;
  title: string;
  /** Accessible description of what the dialog is for. */
  description?: string;
  onClose(): void;
  /** Focus trap keeps keyboard users inside; Escape and backdrop close. */
  children: React.ReactNode;
  footer?: React.ReactNode;
  maxWidthClass?: string;
  /** Extra classes for the dialog panel (e.g. print variants). */
  className?: string;
  /** Extra classes for the fixed backdrop (e.g. print:static). */
  overlayClassName?: string;
  /** Extra classes for the header row (e.g. print:hidden). */
  headerClassName?: string;
  /** Extra classes for the footer row (e.g. print:hidden). */
  footerClassName?: string;
}

const FOCUSABLE =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

export function Modal({
  open,
  title,
  description,
  onClose,
  children,
  footer,
  maxWidthClass = "max-w-md",
  className,
  overlayClassName,
  headerClassName,
  footerClassName,
}: ModalProps) {
  const titleId = useId();
  const descriptionId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement as HTMLElement | null;

    // Initial focus: first focusable element inside the dialog.
    requestAnimationFrame(() => {
      const first = dialogRef.current?.querySelector<HTMLElement>(FOCUSABLE);
      first?.focus();
    });

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.stopPropagation();
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE);
      if (!focusable || focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      previouslyFocused.current?.focus?.();
    };
  }, [open, onClose]);

  if (!open || typeof document === "undefined") return null;

  return createPortal(
    <div
      className={cn(
        "fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4",
        overlayClassName,
      )}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        className={cn(
          "w-full",
          maxWidthClass,
          "rounded-md border border-[var(--border)] bg-white shadow-xl",
          className,
        )}
      >
        <div
          className={cn(
            "flex items-start justify-between gap-3 border-b border-[var(--border)] px-5 py-4",
            headerClassName,
          )}
        >
          <div>
            <h2 id={titleId} className="text-base font-semibold">
              {title}
            </h2>
            {description ? (
              <p id={descriptionId} className="mt-0.5 text-xs text-[var(--text-muted)]">
                {description}
              </p>
            ) : null}
          </div>
          <button
            type="button"
            aria-label="Close dialog"
            onClick={onClose}
            className="flex size-9 shrink-0 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]"
          >
            <X aria-hidden="true" size={16} />
          </button>
        </div>
        <div className="max-h-[70vh] overflow-y-auto p-5">{children}</div>
        {footer ? (
          <div
            className={cn(
              "flex justify-end gap-2 border-t border-[var(--border)] px-5 py-4",
              footerClassName,
            )}
          >
            {footer}
          </div>
        ) : null}
      </div>
    </div>,
    document.body,
  );
}
