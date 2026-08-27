"use client";

import { useParams } from "next/navigation";

import { CustomerHistoryPage } from "@/features/customers/components/customer-history-page";

export default function Page() {
  const params = useParams<{ customerId: string }>();
  return <CustomerHistoryPage customerId={params.customerId} />;
}
