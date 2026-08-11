import { ShieldX } from "lucide-react";

import { SecondaryLink } from "@/components/ui/buttons";

export function AccessRestricted({ homePath }: { homePath?: string }) {
  return (
    <section className="rounded-md border border-[var(--border)] bg-white p-6">
      <ShieldX
        aria-hidden="true"
        className="text-[var(--danger)]"
        size={24}
      />
      <h1 className="mt-4 text-lg font-semibold">Access restricted</h1>
      <p className="mt-1 text-sm text-[var(--text-muted)]">
        Your active roles do not include permission for this workspace.
      </p>
      {homePath ? (
        <SecondaryLink href={homePath} className="mt-4">
          Return to workspace
        </SecondaryLink>
      ) : null}
    </section>
  );
}
