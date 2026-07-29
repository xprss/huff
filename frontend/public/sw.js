const CACHE_NAME = "indovena-shell-v1";
const APP_SHELL = ["/", "/manifest.json", "/icons/huff-icon.svg"];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))))
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  const url = new URL(request.url);

  if (request.method !== "GET" || url.pathname.startsWith("/api/")) {
    return;
  }

  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok && url.origin === self.location.origin) {
            const responseCopy = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put("/", responseCopy));
          }
          return response;
        })
        .catch(() => caches.match("/"))
    );
    return;
  }

  event.respondWith(
    fetch(request)
      .then((response) => {
        if (response.ok && url.origin === self.location.origin) {
          const responseCopy = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(request, responseCopy));
        }

        return response;
      })
      .catch(() => caches.match(request))
  );
});

self.addEventListener("push", (event) => {
  const payload = readPushPayload(event);
  const title = payload.title || "HexaQuot";

  event.waitUntil(
    self.registration.showNotification(title, {
      body: payload.body || "Nuova partita disponibile.",
      icon: payload.icon || "/icons/huff-icon.svg",
      badge: "/icons/huff-icon.svg",
      tag: payload.tag || "new-game",
      data: {
        url: payload.url || "/"
      }
    })
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const targetUrl = new URL(event.notification.data?.url || "/", self.location.origin).href;
  event.waitUntil(
    self.clients
      .matchAll({ type: "window", includeUncontrolled: true })
      .then((clients) => {
        const focusedClient = clients.find((client) => client.url === targetUrl);
        if (focusedClient) {
          return focusedClient.focus();
        }
        return self.clients.openWindow(targetUrl);
      })
  );
});

function readPushPayload(event) {
  if (!event.data) {
    return {};
  }

  try {
    return event.data.json();
  } catch (_error) {
    return {
      body: event.data.text()
    };
  }
}
