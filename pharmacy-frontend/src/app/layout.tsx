import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

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
  // Runtime backend URL: set POS_API_BASE_URL on the container to repoint
  // an existing build at a different API host without rebuilding.
  const runtimeApiBaseUrl =
    process.env.POS_API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    "http://localhost:9090/api/v1";
  const bootstrapConfig = `window.__POS_CONFIG=${JSON.stringify({
    apiBaseUrl: runtimeApiBaseUrl,
  })};`;

  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <script
          id="pos-runtime-config"
          dangerouslySetInnerHTML={{ __html: bootstrapConfig }}
        />
        <AuthBootstrap />
        <CartBootstrap />
        <TerminalHeartbeat />
        <WorkspaceBootstrap />
        {children}
      </body>
    </html>
  );
}
