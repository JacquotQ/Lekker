import { buildSystemPrompt, type Profile } from "./prompt";

export interface Env {
  QWEN_API_KEY: string;
  RATE_LIMIT?: KVNamespace;
}

interface QueryRequest {
  query: string;
  profile: Profile;
  language: "zh" | "en";
  deviceId: string;
}

const DAILY_LIMIT = 50;
const MAX_QUERY_LENGTH = 200; // 防止超长 query 刷 token
const QWEN_API_URL =
  "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

function corsHeaders(): HeadersInit {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  };
}

function jsonError(msg: string, status: number): Response {
  return new Response(JSON.stringify({ error: msg }), {
    status,
    headers: { "Content-Type": "application/json", ...corsHeaders() },
  });
}

async function checkRateLimit(
  kv: KVNamespace | undefined,
  deviceId: string
): Promise<boolean> {
  if (!kv) return true; // no KV bound → skip rate limiting
  const today = new Date().toISOString().slice(0, 10);
  const key = `rate:${deviceId}:${today}`;
  const val = await kv.get(key);
  const count = val ? parseInt(val, 10) : 0;
  if (count >= DAILY_LIMIT) return false;
  await kv.put(key, String(count + 1), { expirationTtl: 86400 });
  return true;
}

function validateRequest(body: unknown): QueryRequest {
  if (!body || typeof body !== "object") throw new Error("Invalid request body");
  const b = body as Record<string, unknown>;
  if (typeof b.query !== "string" || !b.query.trim())
    throw new Error("Missing or empty 'query'");
  if ((b.query as string).length > MAX_QUERY_LENGTH)
    throw new Error(`Query too long (max ${MAX_QUERY_LENGTH} chars)`);
  if (typeof b.deviceId !== "string" || !b.deviceId.trim())
    throw new Error("Missing 'deviceId'");

  const lang = b.language === "en" ? "en" : "zh";

  const profile: Profile = {
    etymologyWeight: 1.0,
    storiesWeight: 1.0,
    connectionsWeight: 1.0,
    formalWeight: 1.0,
  };
  if (b.profile && typeof b.profile === "object") {
    const p = b.profile as Record<string, unknown>;
    if (typeof p.etymologyWeight === "number")
      profile.etymologyWeight = p.etymologyWeight;
    if (typeof p.storiesWeight === "number")
      profile.storiesWeight = p.storiesWeight;
    if (typeof p.connectionsWeight === "number")
      profile.connectionsWeight = p.connectionsWeight;
    if (typeof p.formalWeight === "number")
      profile.formalWeight = p.formalWeight;
  }

  return {
    query: b.query as string,
    profile,
    language: lang,
    deviceId: b.deviceId as string,
  };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(request.url);

    // Health check
    if (url.pathname === "/health") {
      return new Response(JSON.stringify({ status: "ok" }), {
        headers: { "Content-Type": "application/json", ...corsHeaders() },
      });
    }

    // Only POST /api/query
    if (request.method !== "POST" || url.pathname !== "/api/query") {
      return jsonError("Not found", 404);
    }

    // Parse & validate
    let req: QueryRequest;
    try {
      const body = await request.json();
      req = validateRequest(body);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Bad request";
      return jsonError(msg, 400);
    }

    // Rate limit
    const allowed = await checkRateLimit(env.RATE_LIMIT, req.deviceId);
    if (!allowed) {
      return jsonError("Daily query limit reached. Try again tomorrow.", 429);
    }

    // Build system prompt
    const systemPrompt = buildSystemPrompt(req.profile, req.language);

    // Call Qwen API with streaming
    const qwenBody = JSON.stringify({
      model: "qwen-turbo",
      max_tokens: 2500,
      stream: true,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: req.query },
      ],
    });

    let upstream: Response;
    try {
      upstream = await fetch(QWEN_API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${env.QWEN_API_KEY}`,
        },
        body: qwenBody,
      });
    } catch {
      return jsonError("Failed to reach AI service", 502);
    }

    if (!upstream.ok) {
      const errText = await upstream.text().catch(() => "Unknown error");
      console.error(`Qwen API error ${upstream.status}: ${errText}`);
      return jsonError(`AI service error: ${upstream.status}`, 502);
    }

    // Stream SSE pass-through
    return new Response(upstream.body, {
      status: 200,
      headers: {
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
        ...corsHeaders(),
      },
    });
  },
};
