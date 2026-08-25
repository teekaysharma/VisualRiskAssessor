# Demo proxy — "Try Demo (no key needed)"

Lets a visitor to the live app run one AI analysis without pasting their
own Groq API key — for demoing to a prospective client who doesn't have
one handy. It works by holding **your own free Groq key** on a small
Cloudflare Worker, which the app calls instead of Groq directly for that
one button.

**Not for production.** This is a convenience for demos and evaluation,
not a backend to build a paying client's workflow on:
- It uses *your* Groq free-tier quota (shared across every visitor who
  clicks the button — roughly 1,000 requests/day, 30/minute at time of
  writing). Once a client is paying, they should use their own key (the
  existing "paste your key" mode) or a properly provisioned backend.
- There's no per-visitor rate limiting, storage, or logging.
- If your free-tier quota is exhausted, the button shows an error —
  it doesn't fall back to anything.

## One-time setup (~5 minutes, no credit card)

1. **Get a free Groq API key**, if you don't have one already:
   [console.groq.com/keys](https://console.groq.com/keys) → Create API Key.
   Keep it somewhere safe — you'll paste it once in step 4, nowhere else.

2. **Create a free Cloudflare account** (no billing required for this):
   [dash.cloudflare.com/sign-up](https://dash.cloudflare.com/sign-up)

3. **Create the Worker:**
   - In the Cloudflare dashboard: **Workers & Pages → Create → Create Worker**
   - Give it any name (e.g. `visualriskassessor-demo`) → Deploy
   - Click **Edit code**, delete the placeholder content, and paste in the
     entire contents of [`worker.js`](./worker.js) from this folder
   - Click **Deploy**

4. **Add your Groq key as a secret** (never as plain code):
   - On the Worker's page: **Settings → Variables and Secrets**
   - Add variable: name `GROQ_API_KEY`, type **Secret**, value = the key
     from step 1 → Save and deploy

5. **Copy the Worker's URL** — shown at the top of the Worker's page,
   looks like `https://visualriskassessor-demo.<your-subdomain>.workers.dev`

6. **Point the app at it** — open `index.html` in this repo, find the line:

   ```js
   const DEMO_PROXY_URL = ''; // e.g. 'https://visualriskassessor-demo.yoursubdomain.workers.dev'
   ```

   and paste your Worker's URL between the quotes. Commit and push (or
   open a PR) — once merged, GitHub Pages redeploys and the "Try Demo"
   button on the live site starts working.

If `DEMO_PROXY_URL` is left blank, the button stays visible but shows a
"demo not configured yet" message instead of failing with a confusing
network error — it never silently pretends to work.

## Rotating or revoking the demo key

Delete/regenerate the key at [console.groq.com/keys](https://console.groq.com/keys),
then update the `GROQ_API_KEY` secret on the Worker (step 4) with the new
value. The old key stops working immediately; nothing else needs to change.
