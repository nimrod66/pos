"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { Command } from "cmdk";
import type { LucideIcon } from "lucide-react";
import { Search } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export interface CommandPaletteItem {
  href: string;
  icon: LucideIcon;
  label: string;
  section: string;
}

export function CommandPalette({ items }: { items: CommandPaletteItem[] }) {
  const [open, setOpen] = useState(false);
  const router = useRouter();

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setOpen((current) => !current);
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  function navigate(href: string) {
    setOpen(false);
    router.push(href);
  }

  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          className="hidden h-9 w-64 items-center gap-2 rounded-md border border-[var(--border)] bg-[var(--surface)] px-3 text-left text-xs text-[var(--text-muted)] shadow-sm transition hover:bg-[var(--surface-muted)] lg:flex"
        >
          <Search aria-hidden="true" size={15} />
          <span className="flex-1">Search pages</span>
          <kbd className="rounded border border-[var(--border)] bg-[var(--surface-muted)] px-1.5 py-0.5 font-mono text-[10px]">
            Ctrl K
          </kbd>
        </button>
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/40 backdrop-blur-[1px]" />
        <Dialog.Content className="fixed left-1/2 top-[18vh] z-50 w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--surface-raised)] shadow-2xl focus:outline-none">
          <Dialog.Title className="sr-only">Search pharmacy workspace</Dialog.Title>
          <Command label="Search pharmacy workspace" className="text-[var(--text)]">
            <div className="flex items-center gap-2 border-b border-[var(--border)] px-3">
              <Search aria-hidden="true" className="text-[var(--text-muted)]" size={18} />
              <Command.Input
                autoFocus
                placeholder="Search available pages…"
                className="h-12 w-full bg-transparent text-sm outline-none placeholder:text-[var(--text-subtle)]"
              />
            </div>
            <Command.List className="max-h-[min(420px,60vh)] overflow-y-auto p-2">
              <Command.Empty className="px-3 py-8 text-center text-sm text-[var(--text-muted)]">
                No available page found.
              </Command.Empty>
              {(["Workspace", "Operations", "Management"] as const).map((section) => {
                const sectionItems = items.filter((item) => item.section === section);
                if (!sectionItems.length) return null;
                return (
                  <Command.Group
                    key={section}
                    heading={section}
                    className="mb-2 text-xs font-medium text-[var(--text-subtle)] [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-2"
                  >
                    {sectionItems.map(({ href, icon: Icon, label }) => (
                      <Command.Item
                        key={href}
                        value={`${label} ${section}`}
                        onSelect={() => navigate(href)}
                        className="flex cursor-pointer items-center gap-3 rounded-md px-2 py-2.5 text-sm outline-none data-[selected=true]:bg-[var(--brand-soft)] data-[selected=true]:text-[var(--brand-strong)]"
                      >
                        <Icon aria-hidden="true" size={17} />
                        {label}
                      </Command.Item>
                    ))}
                  </Command.Group>
                );
              })}
            </Command.List>
          </Command>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
