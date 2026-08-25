/**
 * VisualRiskAssessor demo proxy — Cloudflare Worker.
 *
 * Purpose: let a visitor try AI hazard analysis WITHOUT pasting their own
 * Groq API key, for demoing the app to a prospective client. This worker
 * holds the key server-side (as a Worker Secret, never in this file, never
 * in the repo) and forwards exactly one request shape to Groq.
 *
 * This is a demo convenience, not a production inference backend — see
 * "Not for production" in demo-proxy/README.md before relying on this for
 * real client work.
 *
 * Deploy: see demo-proxy/README.md. Nothing in this file needs editing
 * except ALLOWED_ORIGIN below.
 */

// Restrict to the exact origin serving the app. Prevents some other site
// from embedding <script> that calls this worker and burning your quota.
const ALLOWED_ORIGIN = "https://teekaysharma.github.io";

// Hard caps so a single request (or a runaway loop) can't exhaust the
// free Groq quota or run up an unexpected bill if you later add a paid key.
const MAX_IMAGE_BYTES = 4 * 1024 * 1024; // ~4MB base64 image
// meta-llama/llama-4-scout-17b-16e-instruct was deprecated by Groq;
// qwen/qwen3.6-27b is the current documented default vision model
// (console.groq.com/docs/vision, checked 2026-08).
const GROQ_MODEL = "qwen/qwen3.6-27b";
const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

function corsHeaders(origin) {
  const allow = origin === ALLOWED_ORIGIN ? origin : ALLOWED_ORIGIN;
  return {
    "Access-Control-Allow-Origin": allow,
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  };
}

export default {
  async fetch(request, env) {
    const origin = request.headers.get("Origin") || "";
    const headers = corsHeaders(origin);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers });
    }

    if (request.method !== "POST") {
      return new Response(JSON.stringify({ error: "POST only" }), {
        status: 405,
        headers: { ...headers, "Content-Type": "application/json" },
      });
    }

    if (origin !== ALLOWED_ORIGIN) {
      return new Response(JSON.stringify({ error: "Origin not allowed" }), {
        status: 403,
        headers: { ...headers, "Content-Type": "application/json" },
      });
    }

    if (!env.GROQ_API_KEY) {
      // The site owner hasn't set the secret yet — fail clearly rather than
      // pass through an empty Authorization header to Groq.
      return new Response(
        JSON.stringify({ error: "Demo proxy is deployed but GROQ_API_KEY secret is not set" }),
        { status: 500, headers: { ...headers, "Content-Type": "application/json" } }
      );
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
        status: 400,
        headers: { ...headers, "Content-Type": "application/json" },
      });
    }

    const { prompt, dataUrl } = body || {};
    if (typeof prompt !== "string" || typeof dataUrl !== "string") {
      return new Response(JSON.stringify({ error: "Expected { prompt: string, dataUrl: string }" }), {
        status: 400,
        headers: { ...headers, "Content-Type": "application/json" },
      });
    }
    if (dataUrl.length > MAX_IMAGE_BYTES) {
      return new Response(JSON.stringify({ error: "Image too large for the demo endpoint" }), {
        status: 413,
        headers: { ...headers, "Content-Type": "application/json" },
      });
    }

    const groqBody = {
      model: GROQ_MODEL,
      messages: [
        {
          role: "user",
          content: [
            { type: "text", text: prompt },
            { type: "image_url", image_url: { url: dataUrl } },
          ],
        },
      ],
      temperature: 0.4,
      top_p: 0.9,
      max_tokens: 2048,
      // qwen/qwen3.6-27b is a reasoning model that otherwise wraps its
      // answer in <think>...</think> before the JSON, breaking the
      // client's JSON.parse. 'hidden' returns only the final answer.
      // (console.groq.com/docs/reasoning)
      reasoning_format: "hidden",
      // Hidden reasoning still consumes max_tokens even though it's never
      // shown — on a solid-color test image, qwen3.6-27b spent its entire
      // 2048-token budget on reasoning and returned an EMPTY answer
      // (finish_reason: 'length', reasoning_tokens: 2048). This task needs
      // structured extraction, not chain-of-thought, so disable reasoning
      // entirely rather than just raising max_tokens.
      reasoning_effort: "none",
    };

    const groqResp = await fetch(GROQ_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${env.GROQ_API_KEY}`,
      },
      body: JSON.stringify(groqBody),
    });

    // Pass Groq's response straight through — the client already knows how
    // to parse this exact shape (it's the same shape a direct Groq call
    // returns), so this proxy stays a thin, low-maintenance forwarder.
    const text = await groqResp.text();
    return new Response(text, {
      status: groqResp.status,
      headers: { ...headers, "Content-Type": "application/json" },
    });
  },
};
