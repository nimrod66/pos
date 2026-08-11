import type { Metadata } from "next";

import { DashboardPage } from "@/features/dashboard/components/dashboard-page";

export const metadata: Metadata = {
  title: "Dashboard",
};

export default function Dashboard() {
  return <DashboardPage />;
}
