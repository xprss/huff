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

export default defineConfig({
  plugins: [react()],
  define: {
    "import.meta.env.VITE_APP_VERSION": JSON.stringify(resolveAppVersion())
  },
  server: {
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
      "/q": "http://localhost:8080",
      "/auth": "http://localhost:8080"
    }
  },
  build: {
    outDir: "dist",
    emptyOutDir: true
  }
});
