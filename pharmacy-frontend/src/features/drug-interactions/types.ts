export type InteractionSeverity = "MINOR" | "MODERATE" | "MAJOR" | "CONTRAINDICATED";

export interface DrugInteraction {
  id: string;
  medicine1Id: string;
  medicine1Name: string;
  medicine2Id: string;
  medicine2Name: string;
  severity: InteractionSeverity;
  description: string | null;
}

export const SEVERITY_STYLES: Record<InteractionSeverity, { bg: string; text: string; border: string }> = {
  MINOR: { bg: "bg-blue-50", text: "text-blue-700", border: "border-blue-200" },
  MODERATE: { bg: "bg-amber-50", text: "text-amber-700", border: "border-amber-200" },
  MAJOR: { bg: "bg-orange-50", text: "text-orange-700", border: "border-orange-200" },
  CONTRAINDICATED: { bg: "bg-red-50", text: "text-red-700", border: "border-red-200" },
};
