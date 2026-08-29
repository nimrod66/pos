import { useEffect, useRef, useState } from "react";
import { useCartStore } from "@/features/pos/store/cart-store";
import { LiveDrugInteractionGateway } from "./drug-interaction-gateway";
import type { DrugInteraction } from "./types";

const gateway = new LiveDrugInteractionGateway();

export function useDrugInteractions(): DrugInteraction[] {
  const lines = useCartStore((state) => state.lines);
  const [interactions, setInteractions] = useState<DrugInteraction[]>([]);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    const medicineIds = lines.map((l) => l.medicineId).filter(Boolean);
    if (medicineIds.length < 2) {
      setInteractions([]);
      return;
    }

    debounceRef.current = setTimeout(() => {
      void gateway.checkInteractions(medicineIds).then(setInteractions).catch(() => setInteractions([]));
    }, 300);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [lines]);

  return interactions;
}
