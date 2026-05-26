import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Pin the workspace root to genba-frontend so Next does not pick up unrelated
  // lockfiles higher in the directory tree (e.g. pxro/package-lock.json).
  outputFileTracingRoot: path.join(__dirname),
  experimental: {
    optimizePackageImports: ["antd", "@ant-design/icons"],
  },
  env: {
    NEXT_PUBLIC_API_BASE_URL:
      process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8086/api",
    NEXT_PUBLIC_APP_TITLE: process.env.NEXT_PUBLIC_APP_TITLE ?? "Genba",
  },
};

export default nextConfig;
