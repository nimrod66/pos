"use client";

import { AlertTriangle } from "lucide-react";
import { useEffect } from "react";

import { PrimaryButton } from "@/components/ui/buttons";

export default function AppError({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-[var(--danger-soft)] text-[var(--danger)]">
        <AlertTriangle size={28} />
      </div>
      <h2 className="text-xl font-semibold">Something went wrong</h2>
      <p className="max-w-md text-sm text-[var(--text-muted)]">
        {error.message || "An unexpected error occurred. Please try again."}
      </p>
      {error.digest ? (
        <p className="font-mono text-xs text-[var(--text-subtle)]">
          Error ID: {error.digest}
        </p>
      ) : null}
      <PrimaryButton type="button" onClick={retry} className="mt-2">
        Try again
      </PrimaryButton>
    </div>
  );
}
