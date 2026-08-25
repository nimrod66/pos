"use client";

import { LogOut, ShieldX } from "lucide-react";

import { SecondaryButton, SecondaryLink } from "@/components/ui/buttons";
import { useAuthStore } from "@/features/auth/store/auth-store";

export function AccessRestricted({ homePath }: { homePath?: string }) {
  const signOut = useAuthStore((state) => state.signOut);
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
      <div className="mt-4 flex flex-wrap items-center gap-2">
        {homePath ? (
          <SecondaryLink href={homePath}>Return to workspace</SecondaryLink>
        ) : null}
        <SecondaryButton type="button" onClick={() => void signOut()}>
          <LogOut aria-hidden="true" size={15} /> Sign out
        </SecondaryButton>
      </div>
    </section>
  );
}
