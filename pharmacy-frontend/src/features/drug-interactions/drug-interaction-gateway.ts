import { apiRequest } from "@/lib/api-client";
import type { DrugInteraction } from "./types";

export interface DrugInteractionGateway {
  checkInteractions(medicineIds: string[]): Promise<DrugInteraction[]>;
}

export class LiveDrugInteractionGateway implements DrugInteractionGateway {
  async checkInteractions(medicineIds: string[]): Promise<DrugInteraction[]> {
    if (medicineIds.length < 2) return [];
    const params = medicineIds.map((id) => `medicineIds=${encodeURIComponent(id)}`).join("&");
    const response = await apiRequest<DrugInteraction[]>(
      `/drug-interactions/check?${params}`,
    );
    return response.data ?? [];
  }
}

export class PreviewDrugInteractionGateway implements DrugInteractionGateway {
  async checkInteractions(): Promise<DrugInteraction[]> {
    return [];
  }
}
