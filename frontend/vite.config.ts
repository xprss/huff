import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const APP_VERSION_PATTERN =
  /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(\+gha\.[1-9]\d*\.[0-9a-f]{7})?$/;

function resolveAppVersion(): string {
  const configuredVersion = process.env.VITE_APP_VERSION?.trim();
  const appVersion =
    configuredVersion || readFileSync(resolve(__dirname, "../VERSION"), "utf8").trim();

  if (!APP_VERSION_PATTERN.test(appVersion)) {
    throw new Error(`Invalid VITE_APP_VERSION: ${appVersion}`);
  }

  return appVersion;
}

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET?.trim() || "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  define: {
    "import.meta.env.VITE_APP_VERSION": JSON.stringify(resolveAppVersion())
  },
  server: {
    port: 5173,
    watch: process.env.VITE_WATCH_POLLING === "true" ? { usePolling: true, interval: 300 } : undefined,
    proxy: {
      "/api": apiProxyTarget,
      "/q": apiProxyTarget,
      "/auth": apiProxyTarget
    }
  },
  build: {
    outDir: "dist",
    emptyOutDir: true
  }
});
