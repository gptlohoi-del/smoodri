/* SmooDri OTA - bản publish sẽ thay __SMOODRI_VERSION__ bằng Git commit.
 * Cache chỉ kích hoạt sau khi tải đủ toàn bộ file cốt lõi.
 */
const OTA_CACHE_PREFIX = 'smoodri-web-ota-';
const OTA_CACHE = OTA_CACHE_PREFIX + '__SMOODRI_VERSION__';
const OTA_FILES = [
  './', './index.html', './style.css', './app.js', './ota.js',
  './manifest.json', './version.json',
  './icons/icon-192.png', './icons/icon-512.png',
  './icons/icon-maskable.png', './icons/apple-touch-icon.png',
  './icons/favicon-32.png'
];
self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(OTA_CACHE);
    // Một file lỗi => không kích hoạt bản SW mới, vẫn giữ bản đang dùng.
    await cache.addAll(OTA_FILES);
    await self.skipWaiting();
  })());
});
self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const names = await caches.keys();
    await Promise.all(names.filter(name =>
      (name.startsWith(OTA_CACHE_PREFIX) || name.startsWith('smoodri-offline-')) && name !== OTA_CACHE
    ).map(name => caches.delete(name)));
    await self.clients.claim();
  })());
});
self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  const scope = new URL(self.registration.scope);
  if (url.origin !== scope.origin || !url.pathname.startsWith(scope.pathname)) return;
  event.respondWith((async () => {
    const cache = await caches.open(OTA_CACHE);
    try {
      // Online: ưu tiên web mới. Offline: trả lại bản đã lưu thành công.
      const response = await fetch(request);
      if (response.ok && response.type === 'basic') {
        event.waitUntil(cache.put(request, response.clone()).catch(() => {}));
      }
      return response;
    } catch (_) {
      const cached = await cache.match(request, { ignoreSearch: true });
      if (cached) return cached;
      if (request.mode === 'navigate') {
        const home = await cache.match('./index.html');
        if (home) return home;
      }
      return Response.error();
    }
  })());
});
