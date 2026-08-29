"use client";

import { useParams } from "next/navigation";

import { CustomerCreditPage } from "@/features/customers/components/customer-credit-page";

export default function Page() {
  const params = useParams<{ customerId: string }>();
  return <CustomerCreditPage customerId={params.customerId} />;
}
