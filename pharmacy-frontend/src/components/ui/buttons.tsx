import Link from "next/link";
import type { ComponentProps } from "react";

import { cn } from "@/lib/cn";

export const primaryButtonClassName =
  "inline-flex min-h-10 items-center justify-center gap-2 rounded-md bg-[var(--brand)] px-4 text-sm font-semibold text-white transition hover:bg-[var(--brand-strong)] focus:outline-none focus:ring-2 focus:ring-[var(--brand-ring)] disabled:cursor-not-allowed disabled:opacity-55";

export const secondaryButtonClassName =
  "inline-flex min-h-10 items-center justify-center gap-2 rounded-md border border-[var(--border-strong)] bg-white px-4 text-sm font-semibold text-[var(--text)] transition hover:bg-[var(--surface-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--brand-ring)] disabled:cursor-not-allowed disabled:opacity-55";

export function PrimaryButton({
  className,
  ...props
}: ComponentProps<"button">) {
  return <button className={cn(primaryButtonClassName, className)} {...props} />;
}

export function SecondaryButton({
  className,
  ...props
}: ComponentProps<"button">) {
  return <button className={cn(secondaryButtonClassName, className)} {...props} />;
}

export function PrimaryLink({
  className,
  ...props
}: ComponentProps<typeof Link>) {
  return <Link className={cn(primaryButtonClassName, className)} {...props} />;
}

export function SecondaryLink({
  className,
  ...props
}: ComponentProps<typeof Link>) {
  return <Link className={cn(secondaryButtonClassName, className)} {...props} />;
}
