import type { LucideIcon } from "lucide-react";

export function EmptyState({
  description,
  icon: Icon,
  title,
}: {
  description: string;
  icon: LucideIcon;
  title: string;
}) {
  return (
    <div className="flex min-h-48 flex-col items-center justify-center px-5 py-10 text-center">
      <span className="flex size-10 items-center justify-center rounded-md bg-[var(--surface-muted)] text-[var(--text-subtle)]">
        <Icon aria-hidden="true" size={19} />
      </span>
      <p className="mt-3 text-sm font-medium">{title}</p>
      <p className="mt-1 max-w-sm text-xs text-[var(--text-muted)]">{description}</p>
    </div>
  );
}
