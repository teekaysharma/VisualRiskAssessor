# VisualRiskAssessor Web Version

## 🚀 How to Run Locally

### Quick Start (Python HTTP Server)

1. **Navigate to this directory** (where index.html is located)
2. **Start a local web server:**

   **Python 3 (Recommended):**
   ```bash
   python3 -m http.server 8000
   ```

   **Node.js (if installed):**
   ```bash
   npx http-server -p 8000
   ```

3. **Open your browser:**
   - Go to: `http://localhost:8000`

## 🧠 ML asset loading behavior

The web app now uses **repo-vendored pinned loader scripts**:

- `web/vendor/tfjs-4.10.0.min.js`
- `web/vendor/mobilenet-2.1.0.min.js`

These loader scripts pin exact TensorFlow.js/MobileNet versions and request CDN assets with `crossorigin="anonymous"` semantics. This keeps imports stable and explicit while allowing browser cache reuse.

### Optional local fallback model (offline/lab)

A local fallback path is supported:

- Default local path: `web/models/mobilenet/model.json`
- Enable via query param: `?localModel=1`

When enabled, `mobilenet.load({ modelUrl })` is pointed at the local model file. If local model files are missing or invalid, diagnostics will display **Model load failed**.

## 🩺 Startup diagnostics UI

A diagnostics panel is shown on startup with separate statuses for:

- **Browser support** (`Supported` / `Unsupported browser`)
- **Model status** (`Loaded CDN model`, `Loaded local model`, `Model load failed`)
- **Camera status** (`Not requested`, `Requesting camera permission…`, `Camera denied`, etc.)

This explicitly distinguishes model issues from camera permission issues and browser capability issues.

## 🗃️ Recommended cache strategy

For deployment behind a web server or CDN:

1. Serve `web/vendor/*.js` with long-lived cache headers (`Cache-Control: public, max-age=31536000, immutable`) because filenames are version-pinned.
2. Serve `web/models/mobilenet/*` with versioned filenames or cache-busting on model updates.
3. Keep `index.html` on shorter cache TTL so model/config changes are discovered quickly.
4. If using a service worker, pre-cache `web/vendor/*.js` and optionally pre-cache `web/models/mobilenet/*` for offline lab setups.

## 🔧 System Requirements

- Modern browser (Chrome/Edge/Firefox/Safari with camera and canvas support)
- Camera permission (for capture flow)
- Internet connection for CDN model path, unless local fallback model files are provided

## 🛠️ Troubleshooting

### Camera not working
- Use `localhost` or `https://` origin
- Grant camera permission
- Check diagnostics panel for exact state (`Camera denied`, `No camera available`, etc.)

### Model load failed
- Confirm internet access for CDN mode
- Or provide local files under `web/models/mobilenet/` and run with `?localModel=1`
- Check browser dev tools network tab for blocked model shard requests
