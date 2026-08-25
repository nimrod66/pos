import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  async headers() {
    return [
      {
        // Authenticated app pages must never be cached by browsers or
        // intermediate caches. Stale prerendered shells break after every
        // deploy because they reference hashed chunks that no longer exist.
        source: "/((?!_next/static|_next/image|favicon.ico|medicines).*)",
        headers: [
          {
            key: "Cache-Control",
            value: "no-store, must-revalidate",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
