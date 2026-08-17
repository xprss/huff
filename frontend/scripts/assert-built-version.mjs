import { readdir, readFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const expectedVersion = process.argv[2] ?? process.env.VITE_APP_VERSION;

if (!expectedVersion) {
  throw new Error("Pass the expected version as an argument or VITE_APP_VERSION");
}

const distDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "../dist");

async function containsVersion(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const entryPath = join(directory, entry.name);
    if (entry.isDirectory()) {
      if (await containsVersion(entryPath)) {
        return true;
      }
      continue;
    }

    if (/\.(?:css|html|js|json)$/.test(entry.name)) {
      const contents = await readFile(entryPath, "utf8");
      if (contents.includes(expectedVersion)) {
        return true;
      }
    }
  }

  return false;
}

if (!(await containsVersion(distDirectory))) {
  throw new Error(`Built frontend does not contain version ${expectedVersion}`);
}

console.log(`Built frontend contains version ${expectedVersion}`);
