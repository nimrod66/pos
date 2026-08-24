import type { Medicine } from "@/features/workspace/types";

const liquidTerms = [
  "syrup",
  "suspension",
  "solution",
  "elixir",
  "drops",
  "liquid",
];
const topicalOrInjectionTerms = [
  "cream",
  "ointment",
  "gel",
  "lotion",
  "injection",
  "injectable",
  "vial",
  "ampoule",
];

export function medicineImage(
  medicine: Pick<Medicine, "brandName" | "genericName">,
) {
  const search = `${medicine.brandName} ${medicine.genericName}`.toLowerCase();
  if (liquidTerms.some((term) => search.includes(term))) {
    return "/medicines/liquid.png";
  }
  if (topicalOrInjectionTerms.some((term) => search.includes(term))) {
    return "/medicines/topical-injection.png";
  }
  return "/medicines/solid-dose.png";
}
