/**
 * Upload dictionary entries from lekker-dict/final/ to Cloudflare KV.
 *
 * Usage:
 *   node scripts/upload-dict.js --kv-id <KV_NAMESPACE_ID>
 *
 * This reads all JSON files from ../lekker-dict/final/zh/ and ../lekker-dict/final/en/,
 * converts them to Cloudflare KV bulk format, and uploads via wrangler.
 *
 * KV key format: "{lang}:{word}" e.g. "zh:lekker", "en:lekker"
 * KV value: JSON string {"response": "...", "summary": "..."}
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const args = process.argv.slice(2);
const kvIdIdx = args.indexOf("--kv-id");
const KV_ID = kvIdIdx >= 0 && args[kvIdIdx + 1] ? args[kvIdIdx + 1] : null;

if (!KV_ID) {
  console.error("Usage: node scripts/upload-dict.js --kv-id <KV_NAMESPACE_ID>");
  console.error("\nGet your KV namespace ID by running:");
  console.error("  npx wrangler kv:namespace create DICT");
  process.exit(1);
}

const DICT_BASE = path.resolve(__dirname, "../../lekker-dict/final");
const BATCH_SIZE = 10000; // Cloudflare bulk put limit per call
const TMP_DIR = path.join(__dirname, "../.tmp");

fs.mkdirSync(TMP_DIR, { recursive: true });

function loadEntries(lang) {
  const dir = path.join(DICT_BASE, lang);
  const files = fs.readdirSync(dir).filter((f) => f.endsWith(".json"));
  const entries = [];

  for (const f of files) {
    try {
      const data = JSON.parse(fs.readFileSync(path.join(dir, f), "utf8"));
      const word = data.word || f.replace(".json", "");
      entries.push({
        key: `${lang}:${word.toLowerCase().trim()}`,
        value: JSON.stringify({
          response: data.response || "",
          summary: data.summary || "",
        }),
      });
    } catch (e) {
      console.warn(`  Skipping ${lang}/${f}: ${e.message}`);
    }
  }

  return entries;
}

function uploadBatch(entries, batchNum) {
  const tmpFile = path.join(TMP_DIR, `batch-${batchNum}.json`);
  fs.writeFileSync(tmpFile, JSON.stringify(entries));

  try {
    execSync(
      `npx wrangler kv:bulk put --namespace-id ${KV_ID} "${tmpFile}"`,
      { stdio: "inherit", cwd: path.join(__dirname, "..") }
    );
  } finally {
    fs.unlinkSync(tmpFile);
  }
}

// ── Main ─────────────────────────────────────────────────
console.log("Loading dictionary entries...\n");

const zhEntries = loadEntries("zh");
console.log(`  ZH: ${zhEntries.length} words`);

const enEntries = loadEntries("en");
console.log(`  EN: ${enEntries.length} words`);

const allEntries = [...zhEntries, ...enEntries];
console.log(`  Total: ${allEntries.length} KV entries\n`);

// Upload in batches
const totalBatches = Math.ceil(allEntries.length / BATCH_SIZE);
for (let i = 0; i < totalBatches; i++) {
  const batch = allEntries.slice(i * BATCH_SIZE, (i + 1) * BATCH_SIZE);
  console.log(
    `Uploading batch ${i + 1}/${totalBatches} (${batch.length} entries)...`
  );
  uploadBatch(batch, i);
}

// Cleanup
fs.rmSync(TMP_DIR, { recursive: true, force: true });

console.log(`\nDone! Uploaded ${allEntries.length} entries to KV.`);
