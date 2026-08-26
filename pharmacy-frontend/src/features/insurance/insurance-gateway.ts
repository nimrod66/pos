"use client";

import { apiRequest } from "@/lib/api-client";
import type {
  CreateInsurerInput,
  InsuranceClaim,
  InsuranceMember,
  Insurer,
} from "./types";

async function getAll<T>(url: `/${string}`): Promise<T[]> {
  const response = await apiRequest<{ data: T[]; pagination: { totalElements: number } }>(url, {
    cache: "no-store",
  });
  return response.data.data ?? [];
}

export const insuranceGateway = {
  // Insurers
  async listInsurers(): Promise<Insurer[]> {
    return getAll<Insurer>("/insurance/insurers?size=500");
  },

  async listActiveInsurers(): Promise<Insurer[]> {
    const response = await apiRequest<Insurer[]>("/insurance/insurers/active", {
      cache: "no-store",
    });
    return response.data;
  },

  async createInsurer(input: CreateInsurerInput): Promise<Insurer> {
    const response = await apiRequest<Insurer>("/insurance/insurers", {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async updateInsurer(id: string, input: Partial<CreateInsurerInput>): Promise<Insurer> {
    const response = await apiRequest<Insurer>(`/insurance/insurers/${id}`, {
      method: "PUT",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async deleteInsurer(id: string): Promise<void> {
    await apiRequest(`/insurance/insurers/${id}`, { method: "DELETE" });
  },

  // Members
  async listMembers(insurerId?: string): Promise<InsuranceMember[]> {
    const query = insurerId ? `?insurerId=${insurerId}&size=500` : "?size=500";
    return getAll<InsuranceMember>(`/insurance/members${query}` as `/${string}`);
  },

  async createMember(insurerId: string, member: Partial<InsuranceMember>): Promise<InsuranceMember> {
    const response = await apiRequest<InsuranceMember>(`/insurance/insurers/${insurerId}/members`, {
      method: "POST",
      body: JSON.stringify(member),
    });
    return response.data;
  },

  // Claims
  async listClaims(insurerId?: string, status?: string): Promise<InsuranceClaim[]> {
    const params = new URLSearchParams({ size: "500" });
    if (insurerId) params.set("insurerId", insurerId);
    if (status) params.set("status", status);
    return getAll<InsuranceClaim>(`/insurance/claims?${params.toString()}` as `/${string}`);
  },

  async updateClaimStatus(
    id: string,
    status: string,
    approved?: number,
    rejected?: number,
    reason?: string,
  ): Promise<InsuranceClaim> {
    const body: Record<string, unknown> = { status };
    if (approved !== undefined) body.approved = approved;
    if (rejected !== undefined) body.rejected = rejected;
    if (reason) body.reason = reason;
    const response = await apiRequest<InsuranceClaim>(`/insurance/claims/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
    return response.data;
  },
};
