import { FileQuestion } from "lucide-react";
import Link from "next/link";

import { SecondaryButton } from "@/components/ui/buttons";

export default function AppNotFound() {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 p-8 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-[var(--surface-muted)] text-[var(--text-muted)]">
        <FileQuestion size={28} />
      </div>
      <h2 className="text-xl font-semibold">Page not found</h2>
      <p className="max-w-md text-sm text-[var(--text-muted)]">
        The page you are looking for does not exist or has been moved.
      </p>
      <Link href="/dashboard" className="mt-2">
        <SecondaryButton type="button">Back to dashboard</SecondaryButton>
      </Link>
    </div>
  );
}
