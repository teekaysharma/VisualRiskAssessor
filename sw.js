/**
 * VisualRiskAssessor service worker — caches the app shell so the page
 * itself (and the AI/COCO libraries it loads) still opens with no
 * connectivity. This is what makes the offline assessment queue usable:
 * without it, an assessor who loses signal after closing the tab would get
 * a blank/failed page instead of the app.
 *
 * Deliberately does NOT touch non-GET requests (POST calls to Groq / the
 * demo proxy) — those must always hit the network live, never be cached or
 * replayed, so a queued assessment is only ever analyzed for real.
 */
const CACHE_NAME = 'vra-shell-v1';
const APP_SHELL = ['./', './index.html', './risk-core.js', './manifest.json', './icon.svg'];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// Cache-first (fast, works offline) with a background network refresh, so a
// page that has ever loaded successfully keeps working with no signal, but
// still picks up updates whenever connectivity is actually available.
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;

  event.respondWith(
    caches.match(event.request).then(cached => {
      const networkFetch = fetch(event.request)
        .then(resp => {
          if (resp && resp.ok) {
            const copy = resp.clone();
            caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
          }
          return resp;
        })
        .catch(() => cached);
      return cached || networkFetch;
    })
  );
});
