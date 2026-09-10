import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import { AppProviders } from "@/components/providers/app-providers";
import { AuthBootstrap } from "@/features/auth/components/auth-bootstrap";
import { CartBootstrap } from "@/features/pos/components/cart-bootstrap";
import { TerminalHeartbeat } from "@/features/terminals/components/terminal-heartbeat";
import { WorkspaceBootstrap } from "@/features/workspace/components/workspace-bootstrap";

import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "Pharmacy POS",
    template: "%s | Pharmacy POS",
  },
  description: "Single-branch pharmacy sales and inventory operations.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <AppProviders>
          <AuthBootstrap />
          <CartBootstrap />
          <TerminalHeartbeat />
          <WorkspaceBootstrap />
          {children}
        </AppProviders>
      </body>
    </html>
  );
}
