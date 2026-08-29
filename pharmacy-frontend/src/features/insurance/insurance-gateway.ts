"use client";

import { apiRequest } from "@/lib/api-client";
import type {
  Authorization,
  ClaimBatch,
  CreateAuthorizationInput,
  CreateBatchInput,
  CreateInsurerInput,
  CreatePaymentInput,
  CreateSchemeInput,
  InsuranceClaim,
  InsuranceMember,
  InsurancePayment,
  InsuranceScheme,
  Insurer,
  InsurerReport,
  Reconciliation,
} from "./types";

async function getAll<T>(url: `/${string}`): Promise<T[]> {
  const response = await apiRequest<{ content: T[]; totalElements: number }>(url, {
    cache: "no-store",
  });
  return response.data.content ?? [];
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

  // Schemes
  async listSchemes(insurerId?: string): Promise<InsuranceScheme[]> {
    const query = insurerId ? `?insurerId=${insurerId}&size=500` : "?size=500";
    return getAll<InsuranceScheme>(`/insurance/schemes${query}` as `/${string}`);
  },

  async createScheme(insurerId: string, input: CreateSchemeInput): Promise<InsuranceScheme> {
    const response = await apiRequest<InsuranceScheme>(`/insurance/insurers/${insurerId}/schemes`, {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  // Authorizations
  async listAuthorizations(insurerId?: string): Promise<Authorization[]> {
    const query = insurerId ? `?insurerId=${insurerId}&size=500` : "?size=500";
    return getAll<Authorization>(`/insurance/authorizations${query}` as `/${string}`);
  },

  async getAuthorization(id: string): Promise<Authorization> {
    const response = await apiRequest<Authorization>(`/insurance/authorizations/${id}`, {
      cache: "no-store",
    });
    return response.data;
  },

  async createAuthorization(insurerId: string, input: CreateAuthorizationInput): Promise<Authorization> {
    const response = await apiRequest<Authorization>(`/insurance/insurers/${insurerId}/authorizations`, {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  // Batches
  async listBatches(insurerId?: string): Promise<ClaimBatch[]> {
    const query = insurerId ? `?insurerId=${insurerId}&size=500` : "?size=500";
    return getAll<ClaimBatch>(`/insurance/batches${query}` as `/${string}`);
  },

  async createBatch(insurerId: string, input: CreateBatchInput): Promise<ClaimBatch> {
    const response = await apiRequest<ClaimBatch>(`/insurance/insurers/${insurerId}/batches`, {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async submitBatch(batchId: string): Promise<ClaimBatch> {
    const response = await apiRequest<ClaimBatch>(`/insurance/batches/${batchId}/submit`, {
      method: "POST",
    });
    return response.data;
  },

  // Payments
  async listPayments(insurerId?: string): Promise<InsurancePayment[]> {
    const query = insurerId ? `?insurerId=${insurerId}&size=500` : "?size=500";
    return getAll<InsurancePayment>(`/insurance/payments${query}` as `/${string}`);
  },

  async createPayment(insurerId: string, input: CreatePaymentInput): Promise<InsurancePayment> {
    const response = await apiRequest<InsurancePayment>(`/insurance/insurers/${insurerId}/payments`, {
      method: "POST",
      body: JSON.stringify(input),
    });
    return response.data;
  },

  async linkPayment(paymentId: string, claimIds: string[]): Promise<void> {
    await apiRequest(`/insurance/payments/${paymentId}/link`, {
      method: "POST",
      body: JSON.stringify({ claimIds }),
    });
  },

  // Reports & Reconciliation
  async getInsurerReport(insurerId: string): Promise<InsurerReport> {
    const response = await apiRequest<InsurerReport>(`/insurance/reports/${insurerId}`, {
      cache: "no-store",
    });
    return response.data;
  },

  async listReconciliations(): Promise<Reconciliation[]> {
    return getAll<Reconciliation>("/insurance/reconciliations?size=500");
  },

  async runReconciliation(insurerId: string, periodFrom: string, periodTo: string): Promise<Reconciliation> {
    const response = await apiRequest<Reconciliation>("/insurance/reconcile", {
      method: "POST",
      body: JSON.stringify({ insurerId, periodFrom, periodTo }),
    });
    return response.data;
  },
};
