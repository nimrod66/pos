"use client";

import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { Check, Laptop, Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { toast } from "sonner";

const themes = [
  { label: "Light", value: "light", icon: Sun },
  { label: "Dark", value: "dark", icon: Moon },
  { label: "System", value: "system", icon: Laptop },
] as const;

export function ThemeToggle() {
  const { setTheme, theme } = useTheme();
  const currentTheme = theme ?? "system";
  const CurrentIcon =
    themes.find((item) => item.value === currentTheme)?.icon ?? Laptop;

  function changeTheme(value: "light" | "dark" | "system") {
    setTheme(value);
    toast.success(`Theme set to ${value}`);
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          type="button"
          aria-label="Change colour theme"
          className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] transition hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
        >
          <CurrentIcon aria-hidden="true" size={17} />
        </button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          className="z-50 min-w-36 rounded-md border border-[var(--border)] bg-[var(--surface-raised)] p-1 shadow-lg"
          sideOffset={8}
        >
          <DropdownMenu.Label className="px-2 py-1.5 text-xs font-medium text-[var(--text-muted)]">
            Appearance
          </DropdownMenu.Label>
          <DropdownMenu.RadioGroup
            value={currentTheme}
            onValueChange={(value) => changeTheme(value as "light" | "dark" | "system")}
          >
            {themes.map(({ icon: Icon, label, value }) => (
              <DropdownMenu.RadioItem
                key={value}
                value={value}
                className="flex h-9 cursor-pointer items-center gap-2 rounded px-2 text-sm outline-none data-[highlighted]:bg-[var(--surface-muted)]"
              >
                <Icon aria-hidden="true" size={16} />
                <span className="flex-1">{label}</span>
                <DropdownMenu.ItemIndicator>
                  <Check aria-hidden="true" size={15} />
                </DropdownMenu.ItemIndicator>
              </DropdownMenu.RadioItem>
            ))}
          </DropdownMenu.RadioGroup>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
