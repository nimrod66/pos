import { Loader2 } from "lucide-react";

export default function PosLoading() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--bg)]">
      <Loader2 className="size-6 animate-spin text-[var(--text-muted)]" />
    </div>
  );
}
