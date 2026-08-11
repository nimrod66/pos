import { Pill } from "lucide-react";

import { cn } from "@/lib/cn";

interface BrandMarkProps {
  className?: string;
  inverse?: boolean;
}

export function BrandMark({ className, inverse = false }: BrandMarkProps) {
  return (
    <div
      className={cn(
        "flex size-10 shrink-0 items-center justify-center rounded-md",
        inverse
          ? "bg-white text-[var(--brand-deep)]"
          : "bg-[var(--brand)] text-white",
        className,
      )}
    >
      <Pill aria-hidden="true" size={21} strokeWidth={2.3} />
    </div>
  );
}
