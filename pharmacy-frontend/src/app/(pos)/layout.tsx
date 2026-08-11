import { PosShell } from "@/features/pos/components/pos-shell";

export default function Layout({ children }: { children: React.ReactNode }) {
  return <PosShell>{children}</PosShell>;
}
