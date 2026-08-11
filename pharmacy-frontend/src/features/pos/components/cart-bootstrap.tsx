"use client";

import { useEffect } from "react";

import { useCartStore } from "@/features/pos/store/cart-store";

export function CartBootstrap() {
  useEffect(() => {
    void useCartStore.persist.rehydrate();
  }, []);
  return null;
}
