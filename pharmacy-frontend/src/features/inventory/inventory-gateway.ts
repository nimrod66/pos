"use client";

import { apiRequest } from "@/lib/api-client";
import type {
  StockCount,
  StockCountInput,
  StockTransfer,
  StockTransferInput,
} from "@/features/workspace/types";

interface BackendPage<T> {
  content: T[];
  totalPages: number;
}

export const inventoryGateway = {
  async listStockTransfers(): Promise<StockTransfer[]> {
    const endpoint = "/api/v1/stock-transfers?size=100&sort=createdAt,desc";
    const first = await apiRequest<BackendPage<StockTransfer>>(endpoint, {
      cache: "no-store",
    });
    const rows = [...first.data.content];
    for (let page = 1; page < first.data.totalPages; page += 1) {
      const response = await apiRequest<BackendPage<StockTransfer>>(
        `${endpoint}&page=${page}` as `/${string}`,
        { cache: "no-store" },
      );
      rows.push(...response.data.content);
    }
    return rows;
  },

  async getStockTransfer(id: string): Promise<StockTransfer> {
    return (
      await apiRequest<StockTransfer>(`/api/v1/stock-transfers/${id}`, {
        cache: "no-store",
      })
    ).data;
  },

  async createStockTransfer(input: StockTransferInput): Promise<StockTransfer> {
    return (
      await apiRequest<StockTransfer>("/api/v1/stock-transfers", {
        method: "POST",
        body: input,
      })
    ).data;
  },

  async approveStockTransfer(id: string): Promise<StockTransfer> {
    return (
      await apiRequest<StockTransfer>(`/api/v1/stock-transfers/${id}/approve`, {
        method: "PATCH",
      })
    ).data;
  },

  async receiveStockTransfer(id: string): Promise<StockTransfer> {
    return (
      await apiRequest<StockTransfer>(`/api/v1/stock-transfers/${id}/receive`, {
        method: "PATCH",
      })
    ).data;
  },

  async listStockCounts(): Promise<StockCount[]> {
    const endpoint = "/stock-counts?size=100&sort=createdAt,desc";
    const first = await apiRequest<BackendPage<StockCount>>(endpoint, {
      cache: "no-store",
    });
    const rows = [...first.data.content];
    for (let page = 1; page < first.data.totalPages; page += 1) {
      const response = await apiRequest<BackendPage<StockCount>>(
        `${endpoint}&page=${page}` as `/${string}`,
        { cache: "no-store" },
      );
      rows.push(...response.data.content);
    }
    return rows;
  },

  async getStockCount(id: string): Promise<StockCount> {
    return (
      await apiRequest<StockCount>(`/stock-counts/${id}`, {
        cache: "no-store",
      })
    ).data;
  },

  async createStockCount(input: StockCountInput): Promise<StockCount> {
    return (
      await apiRequest<StockCount>("/stock-counts", {
        method: "POST",
        body: input,
      })
    ).data;
  },

  async completeStockCount(id: string): Promise<StockCount> {
    return (
      await apiRequest<StockCount>(`/stock-counts/${id}/complete`, {
        method: "PATCH",
      })
    ).data;
  },

  async reconcileStockCount(id: string): Promise<StockCount> {
    return (
      await apiRequest<StockCount>(`/stock-counts/${id}/reconcile`, {
        method: "PATCH",
      })
    ).data;
  },
};
