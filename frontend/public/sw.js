const CACHE_NAME = "my-fitness-static-v2";
const PRECACHE_URLS = [
  "/manifest.webmanifest",
  "/icon-192.png",
  "/icon-512.png",
  "/icon-maskable-512.png",
  "/apple-touch-icon.png",
];

const NETWORK_ONLY_PATHS = [
  "/api/",
  "/oauth2/",
  "/login/",
  "/logout",
  "/actuator/",
];

const CACHEABLE_DESTINATIONS = new Set([
  "style",
  "script",
  "font",
  "image",
]);

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(PRECACHE_URLS)),
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter(
            (key) =>
              key.startsWith("my-fitness-") && key !== CACHE_NAME,
          )
          .map((key) => caches.delete(key)),
      ),
    ),
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const request = event.request;

  if (request.method !== "GET") {
    return;
  }

  const url = new URL(request.url);

  if (url.origin !== self.location.origin) {
    return;
  }

  if (
    request.mode === "navigate" ||
    NETWORK_ONLY_PATHS.some((path) => url.pathname.startsWith(path))
  ) {
    return;
  }

  const cacheable =
    PRECACHE_URLS.includes(url.pathname) ||
    url.pathname.startsWith("/_next/static/") ||
    CACHEABLE_DESTINATIONS.has(request.destination);

  if (!cacheable) {
    return;
  }

  event.respondWith(staleWhileRevalidate(request));
});

async function staleWhileRevalidate(request) {
  const cache = await caches.open(CACHE_NAME);
  const cached = await cache.match(request);

  const network = fetch(request)
    .then(async (response) => {
      if (response.ok && response.type === "basic") {
        await cache.put(request, response.clone());
      }
      return response;
    })
    .catch((error) => {
      if (cached) {
        return cached;
      }
      throw error;
    });

  return cached ?? network;
}
