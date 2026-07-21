import { createHmac, timingSafeEqual } from "node:crypto";
import { createServer } from "node:http";
import { spawn } from "node:child_process";

const PORT = Number.parseInt(process.env.PORT ?? "8091", 10);
const WEBHOOK_PATH = process.env.WEBHOOK_PATH ?? "/__hooks/github-deploy";
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET;
const TARGET_BRANCH = process.env.TARGET_BRANCH ?? "master";
const DEPLOY_SCRIPT = process.env.DEPLOY_SCRIPT ?? "/root/huff/scripts/webhook-deploy.sh";

if (!WEBHOOK_SECRET) {
  throw new Error("WEBHOOK_SECRET is required");
}

let deployInProgress = false;

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, { "Content-Type": "application/json" });
  response.end(JSON.stringify(payload));
}

function verifySignature(signatureHeader, payload) {
  if (!signatureHeader?.startsWith("sha256=")) {
    return false;
  }

  const received = Buffer.from(signatureHeader, "utf8");
  const digest = createHmac("sha256", WEBHOOK_SECRET).update(payload).digest("hex");
  const expected = Buffer.from(`sha256=${digest}`, "utf8");

  if (received.length !== expected.length) {
    return false;
  }

  return timingSafeEqual(received, expected);
}

function runDeploy() {
  return new Promise((resolve, reject) => {
    const child = spawn(DEPLOY_SCRIPT, {
      stdio: "inherit",
      env: process.env
    });

    child.on("exit", (code) => {
      if (code === 0) {
        resolve();
        return;
      }

      reject(new Error(`deploy script exited with code ${code ?? "unknown"}`));
    });

    child.on("error", reject);
  });
}

const server = createServer((request, response) => {
  if (request.method === "GET" && request.url === "/healthz") {
    sendJson(response, 200, { ok: true });
    return;
  }

  if (request.method !== "POST" || request.url !== WEBHOOK_PATH) {
    sendJson(response, 404, { error: "not_found" });
    return;
  }

  const chunks = [];
  request.on("data", (chunk) => chunks.push(chunk));
  request.on("end", async () => {
    try {
      const payload = Buffer.concat(chunks);
      const signature = request.headers["x-hub-signature-256"];
      const event = request.headers["x-github-event"];

      if (!verifySignature(Array.isArray(signature) ? signature[0] : signature, payload)) {
        sendJson(response, 401, { error: "invalid_signature" });
        return;
      }

      if (event === "ping") {
        sendJson(response, 200, { ok: true, event: "ping" });
        return;
      }

      if (event !== "push") {
        sendJson(response, 202, { ok: true, skipped: "unsupported_event" });
        return;
      }

      const body = JSON.parse(payload.toString("utf8"));
      const branchRef = `refs/heads/${TARGET_BRANCH}`;

      if (body.ref !== branchRef) {
        sendJson(response, 202, { ok: true, skipped: "branch_mismatch" });
        return;
      }

      if (deployInProgress) {
        sendJson(response, 202, { ok: true, skipped: "deploy_in_progress" });
        return;
      }

      deployInProgress = true;
      await runDeploy();
      sendJson(response, 200, { ok: true, deployed: true, branch: TARGET_BRANCH });
    } catch (error) {
      console.error(error);
      sendJson(response, 500, { error: "deploy_failed" });
    } finally {
      deployInProgress = false;
    }
  });
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`GitHub webhook listener running on 127.0.0.1:${PORT}${WEBHOOK_PATH}`);
});
