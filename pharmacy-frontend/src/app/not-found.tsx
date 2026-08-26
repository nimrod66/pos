import { FileQuestion } from "lucide-react";
import Link from "next/link";

export default function RootNotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[var(--bg)] p-8 text-center">
      <div className="flex size-14 items-center justify-center rounded-full bg-[var(--surface-muted)] text-[var(--text-muted)]">
        <FileQuestion size={28} />
      </div>
      <h2 className="text-xl font-semibold">Page not found</h2>
      <p className="max-w-md text-sm text-[var(--text-muted)]">
        The page you requested does not exist.
      </p>
      <Link
        href="/dashboard"
        className="mt-2 rounded-md bg-[var(--brand)] px-4 py-2 text-sm font-medium text-white hover:bg-[var(--brand-strong)]"
      >
        Go to dashboard
      </Link>
    </div>
  );
}
