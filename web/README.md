# VisualRiskAssessor Web Version

## 🚀 How to Run Locally

### Quick Start (Python HTTP Server)

1. **Navigate to this directory** (where index.html is located)

2. **Start a local web server:**

   **Python 3 (Recommended):**
   ```bash
   python3 -m http.server 8000
   ```

   **Python 2 (if needed):**
   ```bash
   python -m SimpleHTTPServer 8000
   ```

   **Node.js (if you have Node installed):**
   ```bash
   npx http-server -p 8000
   ```

3. **Open your browser:**
   - Go to: `http://localhost:8000`
   - The web app will load automatically

### Alternative: Live Server (VS Code)

1. Install "Live Server" extension in VS Code
2. Right-click on `index.html` and select "Open with Live Server"
3. Automatic browser refresh when you make changes

## 📱 Features

 **Camera Capture** - Take photos using your device camera
 **Photo Upload** - Select images from your device library  
 **ML-Powered Analysis** - AI-based hazard detection using TensorFlow.js
 **Risk Assessment** - HSE risk scoring and matrix visualization
 **Safety Recommendations** - Context-specific safety advice
 **Export Reports** - Download assessment results as text files
 **Manual Override** - Adjust risk parameters manually

## 🔧 System Requirements

- **Modern Web Browser** (Chrome 80+, Firefox 75+, Safari 13+, Edge 80+)
- **Internet Connection** (for loading ML models from CDN)
- **Camera Access** (for photo capture functionality)

## 🛠️ Troubleshooting

### Camera Not Working?
- Use **localhost** or **https** (not file:// protocol)
- Grant **camera permissions** when prompted
- Try **different browsers** if issues persist

### ML Model Issues?
- Ensure **internet connection** is active
- Check **browser console** for errors
- Try **refreshing the page**

### Upload Problems?
- Use **JPEG, PNG, or WEBP** formats
- Keep files **under 10MB** for best performance

## 📋 Testing Images

Try uploading images of:
- Workplace environments
- Construction sites  
- Office spaces
- Industrial areas
- Any workplace scenarios

The app will analyze and provide risk assessments based on detected objects and potential hazards.

---

**Note:** This is a fully functional web application that works entirely in your browser - no installation required!

## Source of Truth for GitHub Pages

- Canonical web app file: `web/index.html`
- Root `index.html` is deployment mirror for GitHub Pages compatibility.
- Run `./scripts_sync_web.sh` after web edits to sync the root copy.


## Camera stability lock
- `web/index.html` contains a `CAMERA_STABILITY_LOCK` marker around the camera startup path.
- Keep that flow intact unless cross-device validated (Android Chrome, iOS Safari, desktop Chrome/Edge/Firefox).
- If you must refactor, preserve: basic `{video:true}` fallback, deviceId/facingMode retries, explicit `video.play()`, and frame-readiness checks.
- UI now includes **Open Camera (Compat)** to force minimal constraints when standard camera open fails.

- Runtime status banner now surfaces camera/file-picker errors directly in-page to aid troubleshooting.
- Upload validation now accepts extension-based image detection (including HEIC/HEIF) when MIME type is missing.
- `CAMERA_STATE_LOCK` in `web/index.html` protects deterministic state transitions (`idle/requesting_camera/camera_ready/...`).
- Added **Run Diagnostics** button to print origin/security/camera/file-input checks directly in-page for faster issue triage.
- Added **Copy Diagnostics** button to quickly share runtime checks during support/debugging.
