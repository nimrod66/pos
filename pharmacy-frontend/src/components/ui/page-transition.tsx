"use client";

import { motion, useReducedMotion } from "motion/react";

export function PageTransition({
  children,
  routeKey,
}: {
  children: React.ReactNode;
  routeKey: string;
}) {
  const prefersReducedMotion = useReducedMotion();

  return (
    <motion.div
      key={routeKey}
      animate={{ opacity: 1, y: 0 }}
      initial={prefersReducedMotion ? false : { opacity: 0, y: 4 }}
      transition={{ duration: 0.16, ease: "easeOut" }}
    >
      {children}
    </motion.div>
  );
}
