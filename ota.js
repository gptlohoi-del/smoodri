'use strict';
/* OTA cho Web/PWA và shell Android WebView.
   Không tự tải lại trang khi đang đo, tránh ngắt dữ liệu chuyến đi. */
(() => {
  const $ota = (id) => document.getElementById(id);
  const status = $ota('otaStatus');
  const version = $ota('otaVersion');
  const base = new URL('./', location.href);
  const localBuild = document.querySelector('meta[name="smoodri-web-version"]')?.content || 'builtin';
  let refreshPending = false;
  let refreshTimer = null;
  let checking = false;
  const reloadKey = 'smoodri_ota_last_reload';

  function say(message) { if (status) status.textContent = message; }
  function isMonitoring() {
    // isRunning thuộc app.js; không can thiệp logic cảm biến.
    try { return typeof isRunning !== 'undefined' && Boolean(isRunning); }
    catch (_) { return true; }
  }
  function safeToReload() {
    return !isMonitoring() && document.visibilityState === 'visible';
  }
  function releaseName(value) { return String(value || 'không rõ').slice(0, 16); }

  function tryActivate() {
    if (!refreshPending || !safeToReload()) return;
    // Chặn reload vô hạn khi trình duyệt thông báo controllerchange nhiều lần.
    const last = Number(sessionStorage.getItem(reloadKey) || 0);
    if (Date.now() - last < 12000) {
      say('Có bản web mới. Đóng rồi mở lại ứng dụng để kích hoạt.');
      return;
    }
    sessionStorage.setItem(reloadKey, String(Date.now()));
    refreshPending = false;
    say('Đã tải bản web mới; đang kích hoạt…');
    location.reload();
  }
  function requestActivation() {
    refreshPending = true;
    if (isMonitoring()) {
      say('Đã tải bản web mới; sẽ kích hoạt sau khi DỪNG theo dõi hoặc lần mở sau.');
    } else {
      tryActivate();
    }
    if (!refreshTimer) refreshTimer = setInterval(() => {
      if (!refreshPending) { clearInterval(refreshTimer); refreshTimer = null; return; }
      tryActivate();
    }, 1800);
  }
  async function checkOTA() {
    if (checking) return;
    if (!navigator.onLine) { say('Đang ngoại tuyến — sử dụng dữ liệu web đã lưu.'); return; }
    checking = true;
    say('Đang kiểm tra bản web trên GitHub Pages…');
    try {
      // Chỉ kiểm tra thông tin từ máy chủ; Service Worker cũng ưu tiên mạng.
      const response = await fetch(new URL('version.json?time=' + Date.now(), base), { cache: 'no-store' });
      if (!response.ok) throw new Error('HTTP ' + response.status);
      const release = await response.json();
      const serverVersion = String(release.version || '');
      if (!serverVersion) throw new Error('Dữ liệu phiên bản không hợp lệ');
      if (version) version.textContent = 'Web đang mở: ' + releaseName(localBuild) + ' • Máy chủ: ' + releaseName(serverVersion);
      if (serverVersion === localBuild) {
        say('Đang sử dụng bản web mới nhất.');
      } else {
        say('Có nội dung mới, đang chuẩn bị cập nhật tự động…');
      }
      if ('serviceWorker' in navigator) {
        const registration = await navigator.serviceWorker.getRegistration('./');
        if (registration) await registration.update();
      }
    } catch (error) {
      say('Chưa kiểm tra được cập nhật (' + error.message + '). Ứng dụng vẫn dùng bản hiện tại.');
    } finally {
      checking = false;
    }
  }
  async function setupOTA() {
    if (version) version.textContent = 'Web đang mở: ' + releaseName(localBuild);
    if (!('serviceWorker' in navigator) || location.protocol !== 'https:') {
      say('Trình duyệt này không hỗ trợ OTA ngoại tuyến.');
      return;
    }
    navigator.serviceWorker.addEventListener('controllerchange', requestActivation);
    try {
      const registration = await navigator.serviceWorker.register('./sw.js', { updateViaCache: 'none' });
      // Sau lần cài đầu, SW có thể chưa điều khiển trang ngay.
      if (registration.waiting && !navigator.serviceWorker.controller) requestActivation();
      await checkOTA();
    } catch (error) {
      say('Chưa cài được cập nhật ngoại tuyến: ' + error.message);
    }
  }
  $ota('btnCheckOta')?.addEventListener('click', checkOTA);
  window.addEventListener('online', checkOTA);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      tryActivate();
      if (!refreshPending) checkOTA();
    }
  });
  window.addEventListener('load', setupOTA);
})();
