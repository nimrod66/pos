import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import { AuthBootstrap } from "@/features/auth/components/auth-bootstrap";
import { CartBootstrap } from "@/features/pos/components/cart-bootstrap";
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
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full">
        <AuthBootstrap />
        <CartBootstrap />
        <WorkspaceBootstrap />
        {children}
      </body>
    </html>
  );
}
