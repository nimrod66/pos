import type { ComponentProps, ReactNode } from "react";

import { cn } from "@/lib/cn";

export const controlClassName =
  "min-h-10 w-full rounded-md border border-[var(--border-strong)] bg-white px-3 text-sm text-[var(--text)] outline-none transition focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand-ring)] disabled:cursor-not-allowed disabled:bg-[var(--surface-muted)] disabled:text-[var(--text-subtle)]";

export function Field({
  children,
  error,
  hint,
  label,
  required,
}: {
  children: ReactNode;
  error?: string;
  hint?: string;
  label: string;
  required?: boolean;
}) {
  return (
    <label className="block text-sm font-medium text-[var(--text)]">
      <span className="mb-1.5 block">
        {label}
        {required ? <span className="text-[var(--danger)]"> *</span> : null}
      </span>
      {children}
      {error ? (
        <span className="mt-1.5 block text-xs text-[var(--danger)]">{error}</span>
      ) : hint ? (
        <span className="mt-1.5 block text-xs font-normal text-[var(--text-muted)]">
          {hint}
        </span>
      ) : null}
    </label>
  );
}

export function Input({ className, ...props }: ComponentProps<"input">) {
  return <input className={cn(controlClassName, className)} {...props} />;
}

export function Select({ className, ...props }: ComponentProps<"select">) {
  return <select className={cn(controlClassName, className)} {...props} />;
}

export function Textarea({ className, ...props }: ComponentProps<"textarea">) {
  return (
    <textarea
      className={cn(controlClassName, "min-h-24 resize-y py-2.5", className)}
      {...props}
    />
  );
}

export function FormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div
      role="alert"
      className="rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)]"
    >
      {message}
    </div>
  );
}
