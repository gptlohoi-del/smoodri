'use strict';
/* SmooDri 8.0 — Đánh giá êm ái thử nghiệm, PWA offline. */
const $ = (id) => document.getElementById(id);
const clamp = (n, lo, hi) => Math.min(hi, Math.max(lo, n));
const finite = (v, fallback) => Number.isFinite(Number(v)) ? Number(v) : fallback;
const smoothFactor = (dt, tau) => 1 - Math.exp(-clamp(dt, 0, 0.3) / tau);
const STORAGE_KEY = 'smoodri_cfg_v8';
const IS_NATIVE_ANDROID = !!(window.SmooDriAndroid && typeof window.SmooDriAndroid.start === 'function');
window.SmooDriNative = {
  onStarted: null,
  onNativeSample(sample) {
    if (!isRunning || !sample) return;
    // APK: GPS and accelerometer originate from the same Android foreground service.
    // Do not request a second WebView geolocation permission and report it as Android's permission.
    if (IS_NATIVE_ANDROID) gpsNativeSample(sample);
    if (sample.ag) handleMotion({ accelerationIncludingGravity: sample.ag, acceleration: sample.lin || null });
  },
  onMonitorStopped(reason) {
    if (isRunning) stopApp('ĐÃ DỪNG DỊCH VỤ', reason || 'Dịch vụ Android đã ngừng theo dõi');
  },
  onAppResumed() {
    if (IS_NATIVE_ANDROID && isRunning && typeof window.SmooDriAndroid.monitoring==='function'
        && !window.SmooDriAndroid.monitoring()) {
      stopApp('ĐÃ DỪNG DỊCH VỤ', 'Dịch vụ theo dõi Android đã ngừng.');
    }
  }
};
function nativeStart() {
  if (!IS_NATIVE_ANDROID) return Promise.resolve(true);
  return new Promise((resolve) => {
    let resolved = false;
    const finish = (ok, reason) => {
      if (resolved) return;
      resolved = true;
      clearTimeout(timeout);
      window.SmooDriNative.onStarted = null;
      if (!ok) {
        setBanner('KHÔNG KHỞI ĐỘNG ĐƯỢC', reason || 'Kiểm tra quyền cảm biến và dịch vụ Android', 'warn-100');
        try { window.SmooDriAndroid.stop(); } catch (_) {}
      }
      resolve(Boolean(ok));
    };
    // Avoid waiting forever if Android cannot return a permission/service callback.
    const timeout = setTimeout(() => finish(false, 'Không nhận được phản hồi từ Android'), 45000);
    window.SmooDriNative.onStarted = finish;
    try { window.SmooDriAndroid.start(JSON.stringify(config)); }
    catch (_) { finish(false, 'Lỗi kết nối với dịch vụ Android'); }
  });
}
function syncNativeConfig() {
  if (IS_NATIVE_ANDROID) {
    try { window.SmooDriAndroid.updateConfig(JSON.stringify(config)); } catch (_) {}
  }
}



/* BẢNG CẤU HÌNH TẬP TRUNG: MIN/MAX/STEP/DEFAULT được chỉnh trên điện thoại.
   Không đặt giới hạn MIN/MAX ẩn. Chỉ kiểm tra tính hợp lệ toán học.
   Cấu hình mặc định gốc được giữ nguyên để phục hồi bất kỳ lúc nào. */
const NUMERIC_BASE = Object.freeze({
  comfortLevel: {label:'Độ nhạy đánh giá',min:20,max:100,step:5,default:60},
  volumePercent:{label:'Âm lượng cảnh báo',min:10,max:100,step:5,default:70},
  speedFontSize:{label:'Cỡ chữ tốc độ',min:32,max:100,step:2,default:64},
  gpsFallbackSeconds:{label:'Thời gian ước tính mất GPS (giây)',min:0,max:3,step:0.5,default:2},
  maxGa:{label:'Ngưỡng GA (G)',min:0.05,max:0.50,step:0.01,default:0.2},
  maxPhanh:{label:'Ngưỡng PHANH (G)',min:0.05,max:0.55,step:0.01,default:0.224},
  maxCua:{label:'Ngưỡng VÀO CUA (G)',min:0.05,max:0.50,step:0.01,default:0.2},
  potholeThreshold:{label:'Ngưỡng Ổ GÀ (G)',min:0.08,max:0.65,step:0.01,default:0.24},
  maxJerk:{label:'Ngưỡng LẠNG LÁCH (G/s)',min:0.08,max:0.70,step:0.01,default:0.24},
  startDelay:{label:'Thời gian chuẩn bị (giây)',min:1,max:5,step:0.5,default:1.5},
  peakHoldTime:{label:'Giữ giá trị cao nhất (giây)',min:0,max:5,step:0.5,default:1.5},
  alertHoldTime:{label:'Giữ cảnh báo (giây)',min:0.5,max:8,step:0.5,default:3},
  cupSize:{label:'Đường kính cốc G (px)',min:100,max:190,step:2,default:176},
  barHeight:{label:'Chiều cao bốn thanh chỉ số (px)',min:10,max:44,step:1,default:38},
  barFontSize:{label:'Cỡ chữ tên thanh (px)',min:10,max:18,step:0.5,default:12.5}
});
const NUMERIC_OVERRIDE_KEY='smoodri_numeric_limits_v1';
const NUMERIC_RANGES=Object.fromEntries(Object.entries(NUMERIC_BASE).map(([key,v])=>[key,{...v}]));
function rangeValid(key, candidate) {
  const base=NUMERIC_BASE[key];
  if(!base || !candidate) return false;
  const {min,max,step,default:defaultValue}=candidate;
  // Không có safeMin/safeMax: người dùng chủ động quyết định MIN và MAX.
  // Giữ các điều kiện bắt buộc để thanh trượt hoạt động chính xác.
  const span=max-min;
  return [min,max,step,defaultValue].every(Number.isFinite)
    && min<max && Number.isFinite(span) && step>0 && step<=span
    && Number.isFinite(span/step) && defaultValue>=min && defaultValue<=max;
}
function loadNumericLimits() {
  try {
    const values=JSON.parse(localStorage.getItem(NUMERIC_OVERRIDE_KEY)||'{}');
    for(const [key,value] of Object.entries(values)) {
      const merged={...NUMERIC_RANGES[key],...value};
      if(rangeValid(key,merged)) NUMERIC_RANGES[key]=merged;
    }
  }catch(_) { /* Giới hạn cũ không hợp lệ => dùng bản gốc. */ }
}
loadNumericLimits();
function rangeRound(key, value) {
  const r=NUMERIC_RANGES[key], n=Number(value);
  if(!r) return n;
  if(!Number.isFinite(n)) return r.default;
  const bounded=clamp(n,r.min,r.max);
  const out=clamp(r.min+Math.round((bounded-r.min)/r.step)*r.step,r.min,r.max);
  return Number(out.toFixed(6));
}
function rangeStep(key,delta,current) { return rangeRound(key,Number(current)+delta*NUMERIC_RANGES[key].step); }
function rangeDefaults() {return Object.fromEntries(Object.keys(NUMERIC_RANGES).map(key=>[key,NUMERIC_RANGES[key].default]));}
const RANGE_IDS={
  rngComfort:'comfortLevel',rngVolumeBoost:'volumePercent',rngSpeedSize:'speedFontSize',rngGpsFallback:'gpsFallbackSeconds',
  rngMaxGa:'maxGa',rngMaxPhanh:'maxPhanh',rngMaxCua:'maxCua',rngPotholeThreshold:'potholeThreshold',rngMaxJerk:'maxJerk',
  rngStartDelay:'startDelay',rngPeakHoldTime:'peakHoldTime',rngAlertHoldTime:'alertHoldTime'
};

const GRAVITY = 9.80665;
const BASE_THRESHOLDS = Object.freeze({
  maxGa: 0.2, maxPhanh: 0.2, maxCua: 0.18,
  potholeThreshold: 0.2, maxJerk: 0.2
}); // điều chỉnh mức độ êm ái của xe MỨC ÊM ÁI
const THRESHOLD_KEYS = Object.keys(BASE_THRESHOLDS);
const METRICS = ['xoc', 'phanh', 'cua', 'ga'];
const WAVE_NAMES = { square: 'Bíp rõ', triangle: 'Bíp dịu', sine: 'Bíp nhẹ' };
const POTHOLE_NAMES = { sawtooth: 'Âm trầm', square: 'Âm mạnh', sine: 'Âm nhẹ' };
const DEFAULTS = {
  comfortLevel: 60, customThresholds: false,
  maxGa: 0.20, maxPhanh: 0.224, maxCua: 0.20,
  potholeThreshold: 0.24, maxJerk: 0.24,
  visibleMetrics: ['xoc', 'phanh', 'cua', 'ga'],
  soundEnabled: true, volumePercent: 70,
  waveType: 'square', potholeWave: 'sawtooth',
  startDelay: 1.5,
  peakHoldTime: 1.5, alertHoldTime: 3,
  theme: 'light', chartType: 'bar',
  speedVisible: true, speedFontSize: 64, speedColor: '#009c73', speedWeight: 700, gpsFallbackSeconds: 2,
  cupSize:176,cupAlign:'left',speedAlign:'left',barHeight:38,barFontSize:12.5,
  colorXoc:'#e05b37',colorPhanh:'#dc2626',colorCua:'#0284c7',colorGa:'#059669'
};

function thresholdsFor(level) {
  // Nâng mức êm ái => giảm ngưỡng kích hoạt. 20 = linh hoạt; 100 = rất nhạy.
  const ratio = (140 - level) / 100;
  return {
    maxGa: +Math.max(0.05, BASE_THRESHOLDS.maxGa * ratio).toFixed(3),
    maxPhanh: +Math.max(0.05, BASE_THRESHOLDS.maxPhanh * ratio).toFixed(3),
    maxCua: +Math.max(0.05, BASE_THRESHOLDS.maxCua * ratio).toFixed(3),
    potholeThreshold: +Math.max(0.08, BASE_THRESHOLDS.potholeThreshold * ratio).toFixed(3),
    maxJerk: +Math.max(0.08, BASE_THRESHOLDS.maxJerk * ratio).toFixed(3)
  };
}
function boundedThresholds(level) {
  const computed=thresholdsFor(level);
  for(const key of THRESHOLD_KEYS){
    const bounds=NUMERIC_RANGES[key];
    computed[key]=clamp(computed[key],bounds.min,bounds.max); // Giữ chính xác 3 số lẻ khi tự tính; không ép vào STEP.
  }
  return computed;
}
function loadConfig() {
  let raw = null;
  let migrated = false;
  try {
    raw = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (!raw) {
      raw = JSON.parse(localStorage.getItem('smoodri_cfg_v7'));
      migrated = Boolean(raw);
    }
  } catch (_) { raw = null; }
  const cfg = { ...DEFAULTS, ...rangeDefaults(), ...boundedThresholds(NUMERIC_RANGES.comfortLevel.default) };
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return cfg;
  const savedLevel = migrated
    ? (140 - finite(raw.masterScale, 80))
    : finite(raw.comfortLevel, DEFAULTS.comfortLevel);
  cfg.comfortLevel = rangeRound('comfortLevel',savedLevel);
  Object.assign(cfg, boundedThresholds(cfg.comfortLevel));
  THRESHOLD_KEYS.forEach((key) => {
    if (raw[key] !== undefined) cfg[key] = rangeRound(key,finite(raw[key],cfg[key]));
  });
  cfg.customThresholds = migrated
    ? THRESHOLD_KEYS.some((k) => Math.abs(cfg[k] - thresholdsFor(cfg.comfortLevel)[k]) > 0.012)
    : Boolean(raw.customThresholds);
  cfg.speedVisible = raw.speedVisible !== false;
  cfg.speedFontSize = rangeRound('speedFontSize',finite(raw.speedFontSize,cfg.speedFontSize));
  cfg.speedWeight = [500,600,700,800].includes(Number(raw.speedWeight)) ? Number(raw.speedWeight) : 700;
  cfg.speedColor = /^#[0-9a-fA-F]{6}$/.test(raw.speedColor) ? raw.speedColor : '#009c73';
  cfg.gpsFallbackSeconds = rangeRound('gpsFallbackSeconds',finite(raw.gpsFallbackSeconds,cfg.gpsFallbackSeconds));
  cfg.theme = raw.theme === 'dark' ? 'dark' : 'light';
  cfg.chartType = raw.chartType === 'donut' ? 'donut' : 'bar';
  cfg.waveType = WAVE_NAMES[raw.waveType] ? raw.waveType : 'square';
  cfg.potholeWave = POTHOLE_NAMES[raw.potholeWave] ? raw.potholeWave : 'sawtooth';
  cfg.soundEnabled = raw.soundEnabled !== false;
  cfg.volumePercent = rangeRound('volumePercent',migrated ? finite(raw.volumeBoost,2.8)*25 : finite(raw.volumePercent,cfg.volumePercent));
  cfg.startDelay = rangeRound('startDelay',finite(raw.startDelay,cfg.startDelay));
  cfg.peakHoldTime = rangeRound('peakHoldTime',finite(raw.peakHoldTime,cfg.peakHoldTime));
  cfg.alertHoldTime = rangeRound('alertHoldTime',finite(raw.alertHoldTime,cfg.alertHoldTime));
  for(const key of ['cupSize','barHeight','barFontSize']) {
    cfg[key]=rangeRound(key,finite(raw[key],cfg[key]));
  }
  for(const key of ['colorXoc','colorPhanh','colorCua','colorGa']) {
    if(/^#[0-9a-fA-F]{6}$/.test(raw[key])) cfg[key]=raw[key];
  }
  if(['left','center','right'].includes(raw.cupAlign)) cfg.cupAlign=raw.cupAlign;
  if(['left','center','right'].includes(raw.speedAlign)) cfg.speedAlign=raw.speedAlign;
  if (!cfg.customThresholds) Object.assign(cfg,boundedThresholds(cfg.comfortLevel));
  const chosen = Array.isArray(raw.visibleMetrics) ? raw.visibleMetrics.filter((m) => METRICS.includes(m)) : METRICS;
  cfg.visibleMetrics = chosen.length ? [...new Set(chosen)] : [...METRICS];
  return cfg;
}
let config = loadConfig();
function saveConfig() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
    syncNativeConfig(); }
  catch (_) { /* Trình duyệt có thể chặn lưu trữ; ứng dụng vẫn đo được. */ }
}

/* Chỉ thay đổi phần hiển thị, không thay đổi mẫu cảm biến hoặc mức cảnh báo. */
function applyUiAppearance() {
  const root=document.documentElement;
  root.style.setProperty('--cup-size',config.cupSize+'px');
  root.style.setProperty('--cup-align',({left:'flex-start',center:'center',right:'flex-end'})[config.cupAlign]||'flex-start');
  root.style.setProperty('--speed-align',({left:'flex-start',center:'center',right:'flex-end'})[config.speedAlign]||'flex-start');
  root.style.setProperty('--ui-bar-height',config.barHeight+'px');
  root.style.setProperty('--ui-bar-font',config.barFontSize+'px');
  for(const metric of ['Xoc','Phanh','Cua','Ga']) root.style.setProperty('--ui-color-'+metric.toLowerCase(),config['color'+metric]);
}

/* GPS: chỉ đo khi phiên đo hoạt động. Không dùng tốc độ giả khi dữ liệu quá cũ. */
let gpsWatchId = null;
let gpsLast = null;
let gpsPrevious = null;
let gpsLastReceived = 0;
let gpsSmoothed = null;
let gpsSensorAccel = 0;
let gpsTicker = null;
let gpsLastError = '';
let nativeGpsSequence = -1;
function gpsNativeSample(sample) {
  const seq=Number(sample.gpsSeq);
  if(!Number.isFinite(seq) || seq<=0 || seq===nativeGpsSequence) return;
  nativeGpsSequence=seq;
  const age=Number(sample.gpsAgeMs);
  const speed=sample.gpsSpeed==null?null:Number(sample.gpsSpeed);
  const accuracy=sample.gpsAccuracy==null?null:Number(sample.gpsAccuracy);
  const t=performance.now()-Math.max(0,Number.isFinite(age)?age:0);
  const next={speed:Number.isFinite(speed)&&speed>=0?speed:null,
    accuracy:Number.isFinite(accuracy)&&accuracy>=0?accuracy:null,
    time:t,lat:null,lon:null};
  gpsPrevious=gpsLast;gpsLast=next;
  gpsLastReceived=t;
  gpsSmoothed=next.speed===null?null:(gpsSmoothed===null?next.speed:gpsSmoothed*.65+next.speed*.35);
  gpsLastError='';
  renderGpsSpeed();
}
function speedSettings() {
  $('speedPanel').classList.toggle('is-hidden', !config.speedVisible);
  $('speedPanel').style.setProperty('--speed-font-size', `${config.speedFontSize}px`);
  $('speedPanel').style.setProperty('--speed-font-weight', String(config.speedWeight));
  $('speedPanel').style.setProperty('--speed-font-color', config.speedColor);
  $('speedPanel').style.alignItems = ({left:'flex-start',center:'center',right:'flex-end'})[config.speedAlign]||'flex-start';
  $('chkSpeedVisible').checked = config.speedVisible;
  $('rngSpeedSize').value = config.speedFontSize;
  $('lblSpeedSize').textContent = `${config.speedFontSize}px`;
  $('selSpeedWeight').value = String(config.speedWeight);
  $('inpSpeedColor').value = config.speedColor;
  $('rngGpsFallback').value = config.gpsFallbackSeconds;
  $('lblGpsFallback').textContent = `${config.gpsFallbackSeconds} giây`;
}
function gpsMotionSample(e) {
  // Gia tốc đo theo trục điện thoại; chỉ dùng để ước lượng biến thiên ngắn hạn.
  // Không thể xác định chắc chắn chiều tăng/giảm tốc nếu không biết hướng máy.
  const a=e.acceleration;
  if (!a || ![a.x,a.y,a.z].some(Number.isFinite)) return;
  const mag=Math.hypot(Number(a.x)||0,Number(a.y)||0,Number(a.z)||0);
  gpsSensorAccel = gpsSensorAccel * .85 + Math.min(2.5,mag) * .15;
}
function gpsSuccess(position) {
  if (!isRunning) return;
  const t=performance.now();
  const c=position.coords;
  const speed=Number.isFinite(c.speed) && c.speed>=0 ? c.speed*3.6 : null;
  const sample={speed,accuracy:Number.isFinite(c.accuracy)?c.accuracy:null,time:t,lat:c.latitude,lon:c.longitude};
  // Khi hệ điều hành không cung cấp speed, dùng khoảng cách giữa 2 tọa độ hợp lệ.
  if(sample.speed===null && gpsLast && t-gpsLast.time>=600 && t-gpsLast.time<=5000 && c.accuracy<=30 && gpsLast.accuracy<=30){
    const r=Math.PI/180,dl=(sample.lon-gpsLast.lon)*r,dp=(sample.lat-gpsLast.lat)*r;
    const q=Math.sin(dp/2)**2+Math.cos(gpsLast.lat*r)*Math.cos(sample.lat*r)*Math.sin(dl/2)**2;
    const meters=6371000*2*Math.atan2(Math.sqrt(q),Math.sqrt(Math.max(0,1-q)));
    sample.speed=meters/((t-gpsLast.time)/1000)*3.6;
  }
  if(sample.speed!==null && sample.speed<=250 && sample.accuracy!==null && sample.accuracy<=100){
    gpsSmoothed=gpsSmoothed===null?sample.speed:gpsSmoothed*.65+sample.speed*.35;
  }
  gpsPrevious=gpsLast;gpsLast=sample;gpsLastReceived=t;gpsLastError='';
  document.documentElement.dataset.smoodriLastGpsAt = String(Date.now());
  renderGpsSpeed();
}
function gpsFailure(err) {
  gpsLastError=err && err.code===1?'GPS CHƯA ĐƯỢC CẤP QUYỀN':'GPS KHÔNG CÓ TÍN HIỆU';
  renderGpsSpeed();
}
function renderGpsSpeed() {
  const speedEl=$('speedValue'), status=$('gpsStatus');
  if(!isRunning){speedEl.textContent='--';status.textContent='GPS --';status.className='gps-status';return;}
  const elapsed=gpsLastReceived?(performance.now()-gpsLastReceived)/1000:Infinity;
  const hz=gpsLast&&gpsPrevious&&gpsLast.time>gpsPrevious.time ? 1000/(gpsLast.time-gpsPrevious.time):null;
  const accuracy=gpsLast&&Number.isFinite(gpsLast.accuracy)?Math.round(gpsLast.accuracy):null;
  if(elapsed<=2.5 && gpsSmoothed!==null){
    speedEl.textContent=String(Math.round(gpsSmoothed));
    status.textContent=`GPS ${accuracy!==null?'±'+accuracy+'m':'--'} ${hz!==null?hz.toFixed(1)+'Hz':'--Hz'}`;
    status.className='gps-status'+(accuracy>30?' gps-warning':'');
  }else if(gpsSmoothed!==null && elapsed<=2.5+config.gpsFallbackSeconds){
    // Giới hạn trôi: gia tốc đo theo trục không đủ xác định chiều chuyển động.
    // Chỉ ngoại suy rất ngắn theo xu hướng tốc độ GPS trước đó, được giới hạn
    // bởi biên độ gia tốc thiết bị. Hiển thị rõ đây là giá trị ước tính.
    let slope=0;
    if(gpsLast&&gpsPrevious&&gpsLast.speed!==null&&gpsPrevious.speed!==null){
      const dt=(gpsLast.time-gpsPrevious.time)/1000;
      if(dt>.3) slope=clamp((gpsLast.speed-gpsPrevious.speed)/dt,-gpsSensorAccel*3.6,gpsSensorAccel*3.6);
    }
    const estimate=clamp(gpsSmoothed+slope*Math.min(elapsed-2.5,config.gpsFallbackSeconds),0,250);
    speedEl.textContent=String(Math.round(estimate));
    status.textContent='ƯỚC TÍNH · GPS MẤT';status.className='gps-status gps-estimate';
  }else{
    speedEl.textContent='--';
    status.textContent=(IS_NATIVE_ANDROID && gpsLast && gpsLast.speed===null && elapsed<=3.5)
      ? 'GPS ĐÃ CÓ VỊ TRÍ · CHƯA CÓ TỐC ĐỘ' : (gpsLastError||'GPS MẤT');
    status.className='gps-status gps-warning';
  }
}
function startGps() {
  stopGps(); gpsLast=null;gpsPrevious=null;gpsSmoothed=null;gpsSensorAccel=0;gpsLastReceived=0;gpsLastError='';nativeGpsSequence=-1;
  if(IS_NATIVE_ANDROID) {
    // Native LocationManager already has ACCESS_FINE/COARSE_LOCATION from MainActivity.
    // Its samples are delivered via onNativeSample; do not duplicate watchPosition.
    gpsLastError='ĐANG CHỜ GPS';
  } else {
    window.addEventListener('devicemotion',gpsMotionSample);
    if(!navigator.geolocation){gpsLastError='GPS KHÔNG HỖ TRỢ';renderGpsSpeed();return;}
    try {gpsWatchId=navigator.geolocation.watchPosition(gpsSuccess,gpsFailure,{enableHighAccuracy:true,maximumAge:0,timeout:10000});}
    catch(_){gpsLastError='GPS KHÔNG KHỞI ĐỘNG';}
  }
  gpsTicker=setInterval(renderGpsSpeed,400);renderGpsSpeed();
}
function stopGps() {
  if(gpsWatchId!==null&&navigator.geolocation){navigator.geolocation.clearWatch(gpsWatchId);gpsWatchId=null;}
  if(gpsTicker!==null){clearInterval(gpsTicker);gpsTicker=null;}
  window.removeEventListener('devicemotion',gpsMotionSample);
}

/* Giao diện và cài đặt */
function applyTheme(theme, persist = true) {
  config.theme = theme === 'dark' ? 'dark' : 'light';
  document.documentElement.dataset.theme = config.theme;
  $('metaThemeColor').content = config.theme === 'light' ? '#f1f5f9' : '#0b1120';
  $('lblThemeName').textContent = config.theme === 'light' ? 'Sáng' : 'Tối';
  $('btnThemeLight').classList.toggle('selected', config.theme === 'light');
  $('btnThemeDark').classList.toggle('selected', config.theme === 'dark');
  if (persist) saveConfig();
}
function applyChartType(type, persist = true) {
  config.chartType = type === 'donut' ? 'donut' : 'bar';
  $('barsView').classList.toggle('hidden', config.chartType !== 'bar');
  $('donutsView').classList.toggle('hidden', config.chartType !== 'donut');
  $('btnChartBar').classList.toggle('selected', config.chartType === 'bar');
  $('btnChartDonut').classList.toggle('selected', config.chartType === 'donut');
  $('lblChartType').textContent = config.chartType === 'bar' ? 'Thanh ngang' : 'Vòng tròn';
  if (persist) saveConfig();
}
function applyMetricVisibility(persist = true) {
  document.querySelectorAll('[data-metric]').forEach((el) => {
    el.classList.toggle('hidden-metric', !config.visibleMetrics.includes(el.dataset.metric));
  });
  document.querySelectorAll('[data-visible-metric]').forEach((el) => {
    el.checked = config.visibleMetrics.includes(el.dataset.visibleMetric);
    el.disabled = el.checked && config.visibleMetrics.length === 1;
  });
  $('donutsView').style.setProperty('--metric-columns', String(Math.min(4, config.visibleMetrics.length)));
  if (persist) saveConfig();
}
function applyWave(wave, persist = true) {
  config.waveType = WAVE_NAMES[wave] ? wave : 'square';
  $('lblWaveType').textContent = WAVE_NAMES[config.waveType];
  document.querySelectorAll('[data-wave]').forEach((el) => el.classList.toggle('selected', el.dataset.wave === config.waveType));
  if (persist) saveConfig();
}
function applyPotholeWave(wave, persist = true) {
  config.potholeWave = POTHOLE_NAMES[wave] ? wave : 'sawtooth';
  $('lblPotholeWave').textContent = POTHOLE_NAMES[config.potholeWave];
  document.querySelectorAll('[data-pothole]').forEach((el) => el.classList.toggle('selected', el.dataset.pothole === config.potholeWave));
  if (persist) saveConfig();
}
function syncThresholdSliders() {
  const fields = [
    ['rngMaxGa', 'lblMaxGa', 'maxGa', 'G'],
    ['rngMaxPhanh', 'lblMaxPhanh', 'maxPhanh', 'G'],
    ['rngMaxCua', 'lblMaxCua', 'maxCua', 'G'],
    ['rngPotholeThreshold', 'lblPotholeThreshold', 'potholeThreshold', 'G'],
    ['rngMaxJerk', 'lblMaxJerk', 'maxJerk', 'G/s']
  ];
  fields.forEach(([sliderId, labelId, key, unit]) => {
    $(sliderId).value = config[key];
    $(labelId).textContent = `${Number(config[key]).toFixed(3)} ${unit}`;
  });
  $('customBadge').classList.toggle('hidden', !config.customThresholds);
}
function levelLabel(level) {
  if (level >= 85) return 'Rất nhạy – yêu cầu êm ái cao';
  if (level >= 65) return 'Êm ái – phát hiện sớm';
  if (level >= 45) return 'Cân bằng – phù hợp hằng ngày';
  return 'Linh hoạt – ít nhạy hơn';
}
function syncComfortUI() {
  const level = config.comfortLevel;
  $('mainScaleLabel').textContent = String(level);
  $('lblComfort').textContent = `${level}/100`;
  $('rngComfort').value = String(level);
  $('comfortDescription').textContent = `${levelLabel(level)}. Nút + yêu cầu êm ái hơn, nút − linh hoạt hơn.`;
  $('btnComfortMinus').disabled = level <= NUMERIC_RANGES.comfortLevel.min;
  $('btnComfortPlus').disabled = level >= NUMERIC_RANGES.comfortLevel.max;
  syncThresholdSliders();
}
function setComfortLevel(level) {
  config.comfortLevel = rangeRound('comfortLevel',finite(level,NUMERIC_RANGES.comfortLevel.default));
  Object.assign(config, boundedThresholds(config.comfortLevel));
  config.customThresholds = false;
  syncComfortUI();
  saveConfig();
}
function bindNumericSlider(id, labelId, cfgKey, suffix, decimals, custom = false) {
  const el = $(id);
  el.value = config[cfgKey];
  $(labelId).textContent = `${Number(config[cfgKey]).toFixed(decimals)}${suffix}`;
  el.addEventListener('input', () => {
    config[cfgKey] = rangeRound(cfgKey,el.value);
    el.value = config[cfgKey];
    $(labelId).textContent = `${Number(config[cfgKey]).toFixed(decimals)}${suffix}`;
    if (custom) {
      config.customThresholds = true;
      $('customBadge').classList.remove('hidden');
    }
    saveConfig();
  });
}

let audioCtx = null;
let lastNormalBurst = -Infinity;
let lastPotholeBurst = -Infinity;
let lastWeaveBurst = -Infinity;
function initAudioContext() {
  const Ctor = window.AudioContext || window.webkitAudioContext;
  if (!Ctor) return false;
  try {
    if (!audioCtx) audioCtx = new Ctor();
    if (audioCtx.state === 'suspended') audioCtx.resume().catch(() => {});
    return true;
  } catch (_) { return false; }
}
function tone(freq, ms, delayMs = 0, wave = config.waveType, volumeFactor = 1) {
  if (!audioCtx || audioCtx.state === 'closed') return;
  const when = audioCtx.currentTime + delayMs / 1000;
  const osc = audioCtx.createOscillator();
  const gain = audioCtx.createGain();
  osc.type = wave;
  osc.frequency.setValueAtTime(freq, when);
  const amplitude = clamp(config.volumePercent / 100, 0.1, 1) * 0.6 * volumeFactor;  // Điều chỉnh mức âm lượng của âm báo
  const end = when + ms / 1000;
  gain.gain.setValueAtTime(0.0001, when);
  gain.gain.linearRampToValueAtTime(amplitude, when + 0.008);
  gain.gain.exponentialRampToValueAtTime(0.0001, Math.max(when + 0.014, end));
  osc.connect(gain); gain.connect(audioCtx.destination);
  osc.start(when); osc.stop(end + 0.02);
  osc.addEventListener('ended', () => { try { osc.disconnect(); gain.disconnect(); } catch (_) {} }, { once: true });
}
function burst(wave = config.waveType, frequency = 1550) {
  if (!initAudioContext()) return;
  for (let i = 0; i < 4; i++) tone(frequency + (i % 2) * 350, 60, i * 105, wave);
}
function potholeBurst() {
  if (!initAudioContext()) return;
  tone(850, 95, 0, config.potholeWave);
  tone(600, 95, 135, config.potholeWave);
}
function handleSoundAlerts(now, { drivingPct, potholePct, potholeNew, weaving }) {
  // Khi màn hình mở: âm Web Audio gốc. Khi ra nền: Android Service phát PCM
  // dùng cùng tần số, dạng sóng và nhịp; không để hai bộ phát chồng tiếng.
  if (!config.soundEnabled || (IS_NATIVE_ANDROID && document.visibilityState !== 'visible')) return;
  if (weaving) {
    if (now - lastWeaveBurst >= 1900) { lastWeaveBurst = now; burst('square', 2000); }
  } else if (potholeNew && potholePct >= 100) {
    if (now - lastPotholeBurst >= 850) { lastPotholeBurst = now; potholeBurst(); }
  } else if (drivingPct >= 100 && now - lastNormalBurst >= 2000) {
    lastNormalBurst = now;
    burst(config.waveType, 1550);
  }
}

/* Lấy mẫu chuyển động và cân chỉnh tư thế điện thoại. */
let isRunning = false;
let isCountingDown = false;
let isCalibrated = false;
let countdownTimer = null;
let watchdogTimer = null;
let sessionGeneration = 0;
let wakeLock = null;
let lastSensorArrival = 0;
let lastSampleTime = 0;
let recentSamples = [];
let calibrationSamples = [];
let rawSensor = { x: 0, y: 0, z: 1 };
let manualOffset = { x: 0, y: 0, z: 1 };
let slopeBaselineLon = 0;
let slopeHasEstimate = false;
let vertBaseline = 0;
let smoothG = { x: 0, lon: 0, vert: 0 };
let prevLateralG = 0;
let lastSignX = 0;
let lastReversalAt = 0;
let reversalTimestamps = [];
let lastPotholePct = 0;
let lastSlopeUpdate = 0;
const peaks = { Ga: { val: 0, time: 0 }, Phanh: { val: 0, time: 0 }, Cua: { val: 0, time: 0 }, Xoc: { val: 0, time: 0 } };
const alertHold = { lastAt: -Infinity, peaks: { Ga: 0, Phanh: 0, Cua: 0, Xoc: 0 }, weaving: false };
function setBanner(title, subtitle, state = '') {
  $('alertBanner').className = state ? `alert-banner ${state}` : 'alert-banner';
  $('alertTitle').textContent = title;
  $('alertSub').textContent = subtitle;
}
function averageOffsets(samples) {
  const n = samples.length;
  if (n < 6) return null;
  const av = { x: 0, y: 0, z: 0 };
  for (const s of samples) { av.x += s.x; av.y += s.y; av.z += s.z; }
  av.x /= n; av.y /= n; av.z /= n;
  const variation = Math.sqrt(samples.reduce((sum, s) => sum +
    (s.x - av.x) ** 2 + (s.y - av.y) ** 2 + (s.z - av.z) ** 2, 0) / n);
  // Sai lệch lớn cho thấy máy đang rung/chuyển động, cần đứng yên để lấy gốc 0G.
  return variation <= 0.07 && Math.hypot(av.x, av.y, av.z) >= 0.55 ? av : null;
}
function resetMotionSamples() {
  slopeBaselineLon = 0; slopeHasEstimate = false; vertBaseline = 0;
  smoothG = { x: 0, lon: 0, vert: 0 };
  prevLateralG = 0; lastSignX = 0; lastReversalAt = 0; reversalTimestamps = [];
  lastPotholePct = 0;
  Object.values(peaks).forEach((pk) => { pk.val = 0; pk.time = 0; });
  alertHold.lastAt = -Infinity; alertHold.weaving = false;
  Object.keys(alertHold.peaks).forEach((k) => { alertHold.peaks[k] = 0; });

}
async function requestWakeLock() {
  if (!navigator.wakeLock || !isRunning) return;
  try { wakeLock = await navigator.wakeLock.request('screen'); } catch (_) {}
}
function stopApp(title = 'ĐÃ DỪNG', subtitle = 'Bấm Bắt đầu để đo chuyến đi mới') {
  isRunning = false; isCountingDown = false; isCalibrated = false;
  sessionGeneration++;
  clearTimeout(countdownTimer); countdownTimer = null;
  clearInterval(watchdogTimer); watchdogTimer = null;
  window.removeEventListener('devicemotion', handleMotion);
  if (IS_NATIVE_ANDROID) { try { window.SmooDriAndroid.stop(); } catch (_) {} }
  stopGps(); renderGpsSpeed();
  if (wakeLock) { wakeLock.release().catch(() => {}); wakeLock = null; }
  $('btnStart').classList.remove('running');
  $('btnStart').innerHTML = '<span>▶ BẮT ĐẦU</span>';
  setBanner(title, subtitle);
}
async function startApp() {
  if (isRunning) { stopApp(); return; }
  if (!IS_NATIVE_ANDROID && typeof DeviceMotionEvent !== 'undefined' && typeof DeviceMotionEvent.requestPermission === 'function') {
    try {
      const permission = await DeviceMotionEvent.requestPermission();
      if (permission !== 'granted') {
        setBanner('CHƯA CÓ QUYỀN CẢM BIẾN', 'Hãy cho phép cảm biến chuyển động trong trình duyệt', 'warn-100');
        return;
      }
    } catch (_) {
      setBanner('KHÔNG MỞ ĐƯỢC CẢM BIẾN', 'Hãy mở bằng HTTPS và cấp quyền chuyển động', 'warn-100');
      return;
    }
  }
  initAudioContext(); // Mở audio ngay trong thao tác chạm BẮT ĐẦU của người dùng.
  if (!(await nativeStart())) return;
  isRunning = true; isCountingDown = true; isCalibrated = false;
  const generation = ++sessionGeneration;
  calibrationSamples = []; recentSamples = [];
  lastSensorArrival = 0; lastSampleTime = 0;
  $('btnStart').classList.add('running');
  $('btnStart').innerHTML = '<span>⏹ DỪNG</span>';
  setBanner('ĐANG CÂN CHỈNH...', `Giữ điện thoại cố định ${config.startDelay.toFixed(1)} giây`, 'warn-70');
  if (!IS_NATIVE_ANDROID) window.addEventListener('devicemotion', handleMotion);
  startGps();
  requestWakeLock();
  countdownTimer = setTimeout(() => {
    if (!isRunning || generation !== sessionGeneration) return;
    const offsets = averageOffsets(calibrationSamples);
    if (!offsets) {
      stopApp('CHƯA CÂN CHỈNH ĐƯỢC', calibrationSamples.length < 6
        ? 'Không nhận cảm biến. Kiểm tra quyền, HTTPS và thiết bị.'
        : 'Điện thoại đang rung. Dừng xe và thử lại.');
      return;
    }
    manualOffset = offsets; isCountingDown = false; isCalibrated = true;
    lastSampleTime = performance.now();
    resetMotionSamples();
    setBanner('ĐANG THEO DÕI', 'Đã cân chỉnh • Lái xe nhẹ nhàng');
  }, config.startDelay * 1000);
  watchdogTimer = setInterval(() => {
    if (isRunning && !isCountingDown && performance.now() - lastSensorArrival > 3000) {
      setBanner('MẤT TÍN HIỆU CẢM BIẾN', 'Không có mẫu chuyển động mới; kiểm tra điện thoại', 'warn-100');
    }
  }, 1000);
}
function quickCalibrate() {
  if (!isRunning || isCountingDown || !isCalibrated) {
    setBanner('CHƯA THỂ ĐẶT LẠI 0G', 'Bắt đầu theo dõi và để máy đứng yên trước.');
    return;
  }
  const offsets = averageOffsets(recentSamples.slice(-35));
  if (!offsets) {
    setBanner('GIỮ ĐIỆN THOẠI ỔN ĐỊNH', 'Chỉ đặt lại 0G khi xe dừng, máy không rung', 'warn-70');
    return;
  }
  manualOffset = offsets;
  resetMotionSamples(); lastSampleTime = performance.now();
  setBanner('ĐÃ ĐẶT LẠI 0G', 'Giữ nguyên vị trí lắp điện thoại');
}
function updateSlopeBadge(degrees, valid, now) {
  if (now - lastSlopeUpdate < 650) return;
  lastSlopeUpdate = now;
  $('slopeBadge').classList.toggle('mountain-active', valid && Math.abs(degrees) >= 3);
  $('slopeText').textContent = valid
    ? `Dốc tương đối ≈ ${degrees > 0 ? '+' : ''}${degrees}°`
    : 'Độ dốc: chưa xác định';
}
function handleMotion(event) {
  if (!isRunning) return;
  const ag = event.accelerationIncludingGravity;
  if (!ag || ![ag.x, ag.y, ag.z].every((x) => typeof x === 'number' && Number.isFinite(x))) return;
  const now = performance.now();
  const sensor = { x: ag.x / GRAVITY, y: ag.y / GRAVITY, z: ag.z / GRAVITY };
  rawSensor = sensor; lastSensorArrival = now;
  recentSamples.push(sensor);
  if (recentSamples.length > 60) recentSamples.shift();
  if (isCountingDown) {
    calibrationSamples.push(sensor);
    if (calibrationSamples.length > 150) calibrationSamples.shift();
    return;
  }
  if (!isCalibrated) return;
  if (!lastSampleTime) { lastSampleTime = now; return; }
  const dt = clamp((now - lastSampleTime) / 1000, 0.012, 0.15);
  lastSampleTime = now;
  const dx = sensor.x - manualOffset.x;
  const dy = sensor.y - manualOffset.y;
  const dz = sensor.z - manualOffset.z;
  const normYZ = Math.hypot(manualOffset.y, manualOffset.z);
  const norm3 = Math.hypot(manualOffset.x, manualOffset.y, manualOffset.z) || 1;
  const toLong = (y, z) => normYZ > 0.25 ? (y * manualOffset.z - z * manualOffset.y) / normYZ : y;
  const lonCal = toLong(dy, dz);
  const vertCal = (dx * manualOffset.x + dy * manualOffset.y + dz * manualOffset.z) / norm3;
  vertBaseline += (vertCal - vertBaseline) * smoothFactor(dt, 0.55);
  let dynamicVert = Math.abs(vertCal - vertBaseline);
  let dynamicX = dx;
  let dynamicLon = lonCal;
  const lin = event.acceleration;
  const hasLin = lin && [lin.x, lin.y, lin.z].every((x) => typeof x === 'number' && Number.isFinite(x));
  if (hasLin) {
    const lx = lin.x / GRAVITY, ly = lin.y / GRAVITY, lz = lin.z / GRAVITY;
    const linLon = toLong(ly, lz);
    const linVert = Math.abs((lx * manualOffset.x + ly * manualOffset.y + lz * manualOffset.z) / norm3);
    // Trừ gia tốc tịnh tiến trước khi ước lượng độ dốc; không công bố là độ dốc tuyệt đối.
    if (Math.hypot(lx, ly, lz) < 0.12) {
      slopeBaselineLon += ((lonCal - linLon) - slopeBaselineLon) * smoothFactor(dt, 5);
      slopeHasEstimate = true;
    }
    dynamicX = 0.25 * dx + 0.75 * lx;
    dynamicLon = 0.25 * (lonCal - slopeBaselineLon) + 0.75 * linLon;
    dynamicVert = 0.35 * dynamicVert + 0.65 * linVert;
  } else {
    // Thiếu dữ liệu gia tốc tuyến tính => không suy đoán độ dốc từ lực tăng tốc kéo dài.
    dynamicLon = lonCal;
    slopeHasEstimate = false;
  }
  const deg = slopeHasEstimate ? Math.round(Math.asin(clamp(slopeBaselineLon, -1, 1)) * 180 / Math.PI) : 0;
  updateSlopeBadge(deg, slopeHasEstimate, now);
  if (dynamicVert < 0.015) dynamicVert = 0;
  smoothG.x += (dynamicX - smoothG.x) * smoothFactor(dt, 0.20);
  smoothG.lon += (dynamicLon - smoothG.lon) * smoothFactor(dt, 0.25);
  smoothG.vert += (dynamicVert - smoothG.vert) * smoothFactor(dt, 0.085);
  const jerkX = Math.abs(smoothG.x - prevLateralG) / dt;
  prevLateralG = smoothG.x;
  const currentSign = Math.abs(smoothG.x) >= config.maxCua * 0.52 ? Math.sign(smoothG.x) : 0;
  if (currentSign && lastSignX && currentSign !== lastSignX && now - lastReversalAt > 250 && jerkX > config.maxJerk * 0.25) {
    lastReversalAt = now;
    reversalTimestamps.push(now);
  }
  if (currentSign) lastSignX = currentSign;
  reversalTimestamps = reversalTimestamps.filter((t) => now - t <= 3200);
  const weaving = reversalTimestamps.length >= 2; // Không kết luận chỉ từ 1 cú cua hoặc 1 đỉnh jerk.
  evaluateAndRender(smoothG.x, smoothG.lon, smoothG.vert, weaving, now);
}
function levelColor(pct) {
  if (pct >= 100) return 'var(--red)';
  if (pct >= 85) return 'var(--orange)';
  if (pct >= 65) return 'var(--yellow)';
  return 'var(--green)';
}
function updateMetric(key, percentage, now) {
  const pk = peaks[key];
  const holdMs = config.peakHoldTime * 1000;
  if (percentage >= pk.val) { pk.val = percentage; if (percentage > 0) pk.time = now; }
  else if (holdMs <= 0 || now - pk.time >= holdMs) pk.val = percentage;
  const peakPct = Math.round(pk.val);
  const liveClamped = clamp(percentage, 0, 100);
  const peakClamped = clamp(peakPct, 0, 100);
  const bar = $(`bar${key}`);
  bar.style.width = `${liveClamped}%`; bar.style.backgroundColor = levelColor(percentage);
  $(`val${key}`).textContent = `${peakPct}%`;
  $(`val${key}`).style.color = peakPct >= 65 ? levelColor(peakPct) : 'var(--text)';
  const mark = $(`peak${key}`);
  mark.style.opacity = holdMs > 0 && peakPct >= 15 ? '1' : '0';
  mark.style.left = `calc(${Math.min(99, peakClamped)}% - 2px)`;
  mark.style.backgroundColor = levelColor(peakPct);
  const circumference = 2 * Math.PI * 30;
  const ring = $(`donut${key}`);
  ring.style.strokeDashoffset = String(circumference * (1 - peakClamped / 100));
  ring.style.stroke = levelColor(peakPct);
  $(`donutVal${key}`).textContent = `${peakPct}%`;
  $(`donutVal${key}`).style.color = peakPct >= 65 ? levelColor(peakPct) : 'var(--text)';
}
function evaluateAndRender(gx, glon, gvert, weaving, now) {
  const ga = Math.max(0, glon);
  const phanh = Math.max(0, -glon);
  const cua = Math.abs(gx);
  const values = {
    Ga: Math.min(150, Math.round(ga / config.maxGa * 100)),
    Phanh: Math.min(150, Math.round(phanh / config.maxPhanh * 100)),
    Cua: Math.min(150, Math.round(cua / config.maxCua * 100)),
    Xoc: Math.min(150, Math.round(gvert / config.potholeThreshold * 100))
  };
  for (const key of Object.keys(values)) updateMetric(key, values[key], now);
  const driveMax = Math.max(values.Ga, values.Phanh, values.Cua);
  const maxAll = Math.max(driveMax, values.Xoc);
  const potholeNew = values.Xoc >= 100 && lastPotholePct < 75;
  lastPotholePct = values.Xoc;
  handleSoundAlerts(now, { drivingPct: driveMax, potholePct: values.Xoc, potholeNew, weaving });

  const cup = $('cupCircle');
  const radius = Math.max(23, cup.clientWidth / 2 - 16);
  const limit = Math.max(config.maxGa, config.maxPhanh, config.maxCua);
  const nx = clamp(gx / limit, -1, 1);
  const ny = clamp(glon / limit, -1, 1);
  const color = weaving ? 'var(--red)' : levelColor(maxAll);
  $('gBall').style.transform = `translate(${(nx * radius).toFixed(1)}px, ${(-ny * radius).toFixed(1)}px)`;
  $('gBall').style.backgroundColor = color;
  $('gBall').style.boxShadow = `0 0 12px ${color}`;
  $('cupReadout').textContent = `${Math.hypot(gx, glon).toFixed(2)} G`;


  const alertHoldMs = config.alertHoldTime * 1000;
  const triggered = weaving || maxAll >= 100;
  if (triggered) {
    if (now - alertHold.lastAt > alertHoldMs) {
      for (const key of Object.keys(values)) alertHold.peaks[key] = 0;
      alertHold.weaving = false;
    }
    alertHold.lastAt = now;
    for (const key of Object.keys(values)) {
      if (values[key] >= 100) alertHold.peaks[key] = Math.max(alertHold.peaks[key], values[key]);
    }
    alertHold.weaving ||= weaving;
  }
  if (now - alertHold.lastAt <= alertHoldMs) {
    const texts = [];
    if (alertHold.weaving) texts.push('LẮC NGANG LIÊN TỤC');
    for (const [key, label] of [['Xoc', 'Xóc'], ['Phanh', 'Phanh'], ['Cua', 'Cua'], ['Ga', 'Ga']]) {
      if (alertHold.peaks[key] >= 100) texts.push(`${label}: ${alertHold.peaks[key]}%`);
    }
    setBanner(texts.join(' • ') || 'VƯỢT NGƯỠNG', triggered ? 'Hãy điều khiển êm hơn khi điều kiện an toàn cho phép' : 'Đang giữ cảnh báo vừa xuất hiện', 'warn-100');
  } else if (maxAll >= 85) {
    setBanner('GẦN NGƯỠNG', 'Giảm thao tác đột ngột nếu an toàn', 'warn-80');
  } else {
    setBanner('LÁI XE ÊM ÁI', 'Đang đo chuyển động của xe');
  }
}

/* Tất cả nút bấm/cài đặt */
$('chkSpeedVisible').addEventListener('change', e=>{config.speedVisible=e.target.checked;speedSettings();saveConfig();});
$('rngSpeedSize').addEventListener('input', e=>{config.speedFontSize=Number(e.target.value);speedSettings();saveConfig();});
$('selSpeedWeight').addEventListener('change', e=>{config.speedWeight=Number(e.target.value);speedSettings();saveConfig();});
$('inpSpeedColor').addEventListener('input', e=>{config.speedColor=e.target.value;speedSettings();saveConfig();});
$('rngGpsFallback').addEventListener('input', e=>{config.gpsFallbackSeconds=Number(e.target.value);speedSettings();saveConfig();renderGpsSpeed();});
$('btnStart').addEventListener('click', startApp);
if(IS_NATIVE_ANDROID && typeof window.SmooDriAndroid.testBackgroundSound==='function'){
  $('btnTestNativeBackground').hidden=false;
  $('btnTestNativeBackground').addEventListener('click',()=>{
    window.SmooDriAndroid.testBackgroundSound('normal',Math.round(config.volumePercent),config.waveType);
  });
}
$('btnAndroidDiagnostics').addEventListener('click',()=>{
  const output=$('androidDiagnostics');
  if(!IS_NATIVE_ANDROID || typeof window.SmooDriAndroid.diagnostics!=='function'){
    output.textContent='Trang web: âm cảnh báo hoạt động khi trang còn mở. Chạy âm nền cần APK Android.';
    return;
  }
  try{
    const d=JSON.parse(window.SmooDriAndroid.diagnostics());
    output.textContent=`APK ${d.version||'?'} · Dịch vụ: ${d.running?'ĐANG CHẠY':'CHƯA CHẠY'} · `+
      `Cảm biến: ${d.sensorReady?'CÓ':'CHƯA SẴN SÀNG'} · `+
      `Âm nền: ${d.soundEnabled?'BẬT':'TẮT'} · `+
      `Màn hình: ${d.uiVisible?'ĐANG MỞ':'ĐANG NỀN'} · `+
      `GPS: ${d.locationPermission?'ĐÃ CẤP':'KHÔNG BẮT BUỘC'} · `+
      `Lần cảnh báo: ${d.lastAlert||'CHƯA CÓ'}`+
      `${d.error?' · Lỗi dịch vụ: '+d.error:''}`+
      `${d.audioError?' · Lỗi âm nền: '+d.audioError:''}`+
      `${d.previewAudioError?' · Lỗi NGHE THỬ Android: '+d.previewAudioError:''}`;
  }catch(error){output.textContent='Không đọc được trạng thái Android: '+error.message;}
});
if (IS_NATIVE_ANDROID) {
  const button=$('btnBackgroundAudio');
  if(button){
    button.classList.remove('hidden');
    button.addEventListener('click',()=>{
      if(!isRunning){setBanner('CHƯA THEO DÕI','Nhấn BẮT ĐẦU trước khi chuyển ra nền.');return;}
      if(!config.soundEnabled){setBanner('ÂM THANH ĐANG TẮT','Hãy bật Âm thanh cảnh báo trong CÀI ĐẶT.');return;}
      window.SmooDriAndroid.goBackground();
    });
  }
}

$('btnQuickCalib').addEventListener('click', quickCalibrate);
$('btnComfortPlus').addEventListener('click', () => setComfortLevel(rangeStep('comfortLevel',1,config.comfortLevel)));
$('btnComfortMinus').addEventListener('click', () => setComfortLevel(rangeStep('comfortLevel',-1,config.comfortLevel)));
$('rngComfort').addEventListener('input', (e) => setComfortLevel(Number(e.target.value)));
$('btnSettings').addEventListener('click', () => {
  $('settingsModal').classList.add('open');
  $('btnCloseModal').focus();
});
function closeSettings() { $('settingsModal').classList.remove('open'); $('btnSettings').focus(); }
$('btnCloseModal').addEventListener('click', closeSettings);
$('settingsModal').addEventListener('click', (e) => { if (e.target.id === 'settingsModal') closeSettings(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && $('settingsModal').classList.contains('open')) closeSettings(); });
$('btnThemeLight').addEventListener('click', () => applyTheme('light'));
$('btnThemeDark').addEventListener('click', () => applyTheme('dark'));
$('btnChartBar').addEventListener('click', () => applyChartType('bar'));
$('btnChartDonut').addEventListener('click', () => applyChartType('donut'));
document.querySelectorAll('[data-visible-metric]').forEach((input) => input.addEventListener('change', () => {
  const checked = METRICS.filter((key) => document.querySelector(`[data-visible-metric="${key}"]`).checked);
  if (checked.length === 0) { input.checked = true; return; }
  config.visibleMetrics = checked;
  applyMetricVisibility();
}));
$('btnShowAll').addEventListener('click', () => { config.visibleMetrics = [...METRICS]; applyMetricVisibility(); });
$('chkSoundEnabled').addEventListener('change', (e) => { config.soundEnabled = e.target.checked; saveConfig(); });
for (const wave of ['square', 'triangle', 'sine']) {
  $({square:'btnWaveSquare',triangle:'btnWaveTriangle',sine:'btnWaveSine'}[wave]).addEventListener('click', () => {
    applyWave(wave); if (config.soundEnabled) burst(wave, 1200);
  });
}
for (const wave of ['sawtooth', 'square', 'sine']) {
  $({sawtooth:'btnPotholeSaw',square:'btnPotholeSquare',sine:'btnPotholeSine'}[wave]).addEventListener('click', () => {
    applyPotholeWave(wave); if (config.soundEnabled) previewSmooDriSound('pothole', potholeBurst);
  });
}
function previewSmooDriSound(kind, fallback) {
  // Các nút NGHE THỬ và âm trong lúc mở app đều dùng Web Audio gốc.
  fallback();
}
$('btnTestBeepHigh').addEventListener('click', () => previewSmooDriSound('normal', () => burst()));
$('btnTestPothole').addEventListener('click', () => previewSmooDriSound('pothole', potholeBurst));
function bindSliders() {
  bindNumericSlider('rngVolumeBoost', 'lblVolumeBoost', 'volumePercent', '%', 0);
  bindNumericSlider('rngStartDelay', 'lblStartDelay', 'startDelay', ' giây', 1);
  bindNumericSlider('rngPeakHoldTime', 'lblPeakHoldTime', 'peakHoldTime', ' giây', 1);
  bindNumericSlider('rngAlertHoldTime', 'lblAlertHoldTime', 'alertHoldTime', ' giây', 1);
  for (const [id, label, key, unit] of [
    ['rngMaxGa','lblMaxGa','maxGa',' G'],
    ['rngMaxPhanh','lblMaxPhanh','maxPhanh',' G'],
    ['rngMaxCua','lblMaxCua','maxCua',' G'],
    ['rngPotholeThreshold','lblPotholeThreshold','potholeThreshold',' G'],
    ['rngMaxJerk','lblMaxJerk','maxJerk',' G/s']
  ]) bindNumericSlider(id, label, key, unit, 3, true);
}
function syncAllSettings() {
  applyTheme(config.theme, false);
  applyChartType(config.chartType, false);
  applyMetricVisibility(false);
  applyWave(config.waveType, false);
  applyPotholeWave(config.potholeWave, false);
  $('chkSoundEnabled').checked = config.soundEnabled;
  for (const [id, label, key, suffix, decimals] of [
    ['rngVolumeBoost','lblVolumeBoost','volumePercent','%',0],
    ['rngStartDelay','lblStartDelay','startDelay',' giây',1],
    ['rngPeakHoldTime','lblPeakHoldTime','peakHoldTime',' giây',1],
    ['rngAlertHoldTime','lblAlertHoldTime','alertHoldTime',' giây',1]
  ]) {
    $(id).value = config[key]; $(label).textContent = `${Number(config[key]).toFixed(decimals)}${suffix}`;
  }
  syncComfortUI();
  speedSettings();
  applyUiAppearance();
}
$('btnResetDefaults').addEventListener('click', () => {
  if (!window.confirm('Khôi phục tất cả cài đặt về mặc định? Dữ liệu theo dõi đang chạy không bị xóa.')) return;
  config = { ...DEFAULTS, ...rangeDefaults(), visibleMetrics: [...METRICS], ...boundedThresholds(NUMERIC_RANGES.comfortLevel.default) };
  // Độ nhạy mặc định và ngưỡng G phải đồng bộ.
  config.comfortLevel=NUMERIC_RANGES.comfortLevel.default;
  syncAllSettings();
  saveConfig();
});
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && isRunning) {
    requestWakeLock();
    // Không tính dồn thời gian chạy ngầm thành 1 mẫu chuyển động.
    lastSampleTime = performance.now();
  }
});

// OTA và Service Worker được quản lý tập trung tại ota.js.
// Không đăng ký lần thứ hai ở đây để tránh cập nhật trùng nhau.

/* ===== Điều khiển -/+ và thanh trượt ngắn, chống chạm nhầm khi cuộn ===== */
function attachIntentionalSlider(range) {
  let tracking=null;
  range.addEventListener('pointerdown',event=>{
    if(event.button!==0 && event.pointerType==='mouse') return;
    tracking={id:event.pointerId,x:event.clientX,y:event.clientY,start:Number(range.value),active:false};
    // Ngăn click làm nhảy giá trị. Di chuyển theo chiều dọc vẫn để trình duyệt cuộn.
    event.preventDefault();
  });
  range.addEventListener('pointermove',event=>{
    if(!tracking || event.pointerId!==tracking.id) return;
    const dx=event.clientX-tracking.x,dy=event.clientY-tracking.y;
    if(!tracking.active && (Math.abs(dx)<9 || Math.abs(dx)<Math.abs(dy)*1.2)) return;
    tracking.active=true;
    const key=RANGE_IDS[range.id]||range.dataset.numericKey;
    const r=NUMERIC_RANGES[key];
    if(!r) return;
    const space=Math.max(20,range.getBoundingClientRect().width-18);
    const next=rangeRound(key,tracking.start+dx/space*(r.max-r.min));
    if(Number(range.value)!==next) {range.value=String(next);range.dispatchEvent(new Event('input',{bubbles:true}));}
  });
  const stop=()=>{tracking=null;};
  range.addEventListener('pointerup',stop);
  range.addEventListener('pointercancel',stop);
  range.addEventListener('click',event=>event.preventDefault());
  range.addEventListener('keydown',event=>{ // Bàn phím vẫn truy cập được.
    if(!['ArrowLeft','ArrowDown','ArrowRight','ArrowUp','Home','End'].includes(event.key)) return;
    event.preventDefault();
    const key=RANGE_IDS[range.id]||range.dataset.numericKey,r=NUMERIC_RANGES[key];
    if(!r) return;
    const next=event.key==='Home'?r.min:event.key==='End'?r.max:rangeStep(key,['ArrowLeft','ArrowDown'].includes(event.key)?-1:1,range.value);
    range.value=next;range.dispatchEvent(new Event('input',{bubbles:true}));
  });
}
function upgradeSettingsSliders(){
  for(const [id,key] of Object.entries(RANGE_IDS)) {
    const el=$(id);
    const label=el.previousElementSibling;
    // Chỉ chuyển một lần; nhãn gốc vẫn gắn với đúng input để giữ khả năng truy cập.
    const row=document.createElement('div');row.className='setting-range-row';
    const minus=document.createElement('button'),plus=document.createElement('button');
    minus.type=plus.type='button';minus.textContent='−';plus.textContent='+';
    minus.setAttribute('aria-label','Giảm '+NUMERIC_BASE[key].label);
    plus.setAttribute('aria-label','Tăng '+NUMERIC_BASE[key].label);
    let output=null;
    if(label && label.classList.contains('setting-label')){
      output=label.querySelector('strong, span[id^="lbl"]');
      if(output){output.classList.add('setting-number');}
    }
    el.replaceWith(row);
    row.append(minus,el,plus);
    if(output)row.append(output);
    minus.addEventListener('click',()=>{
      el.value=rangeStep(key,-1,el.value);el.dispatchEvent(new Event('input',{bubbles:true}));
    });
    plus.addEventListener('click',()=>{
      el.value=rangeStep(key,1,el.value);el.dispatchEvent(new Event('input',{bubbles:true}));
    });
    attachIntentionalSlider(el);
  }
}
function updateAllRangeLimits(){
  for(const [id,key] of Object.entries(RANGE_IDS)) {
    const range=$(id),r=NUMERIC_RANGES[key];
    range.min=r.min;range.max=r.max;range.step=r.step;
    range.value=String(rangeRound(key,config[key]));
  }
  const direct=$('uiEditSlider');
  if(direct.dataset.numericKey && NUMERIC_RANGES[direct.dataset.numericKey]){
    const r=NUMERIC_RANGES[direct.dataset.numericKey];
    direct.min=r.min;direct.max=r.max;direct.step=r.step;
  }
}
function applyRangeChangeToExisting(){
  for(const key of Object.keys(NUMERIC_RANGES)) {
    if(Object.prototype.hasOwnProperty.call(config,key)) config[key]=rangeRound(key,config[key]);
  }
  // Đồng bộ lại 5 ngưỡng nếu trước đó không dùng ngưỡng tùy chỉnh.
  if(!config.customThresholds) Object.assign(config,boundedThresholds(config.comfortLevel));
  syncAllSettings();updateAllRangeLimits();syncThresholdSliders();
  updateAllRangeLimits();saveConfig();
  if(uiEditMode && uiTarget) renderUiEditControls();
}
/* ===== Cấu hình giới hạn nhập trên điện thoại ===== */
function initRangeEditor(){
  const select=$('rangeConfigTarget');
  for(const [key,r] of Object.entries(NUMERIC_BASE))select.add(new Option(r.label,key));
  const inputs=['rangeEditMin','rangeEditMax','rangeEditStep','rangeEditDefault'];
  const msg=$('rangeEditMessage');
  function display(){
    const r=NUMERIC_RANGES[select.value];
    inputs.forEach((id,i)=>$(id).value=String(r[['min','max','step','default'][i]]));
    msg.textContent='Không có giới hạn MIN/MAX ẩn. Chỉ lưu khi bốn giá trị hợp lệ; có thể khôi phục mặc định bất kỳ lúc nào.';
    msg.dataset.error='false';
  }
  function persist(){
    const key=select.value;
    const candidate={...NUMERIC_RANGES[key],min:Number($('rangeEditMin').value),max:Number($('rangeEditMax').value),step:Number($('rangeEditStep').value),default:Number($('rangeEditDefault').value)};
    if(inputs.some(id=>$(id).value.trim()==='') || !rangeValid(key,candidate)){
      msg.textContent='Giá trị chưa hợp lệ: cần MIN < MAX, STEP > 0 và không lớn hơn (MAX−MIN), DEFAULT trong [MIN, MAX].';
      msg.dataset.error='true';return;
    }
    Object.assign(NUMERIC_RANGES[key],candidate);
    try{localStorage.setItem(NUMERIC_OVERRIDE_KEY,JSON.stringify(Object.fromEntries(Object.entries(NUMERIC_RANGES).map(([k,v])=>[k,{min:v.min,max:v.max,step:v.step,default:v.default}]))));}catch(e){console.warn('Không lưu được giới hạn',e);}
    applyRangeChangeToExisting();msg.textContent='Đã áp dụng và tự lưu '+NUMERIC_BASE[key].label+'.';msg.dataset.error='false';
  }
  select.addEventListener('change',display);
  for(const id of inputs)$(id).addEventListener('change',persist);
  $('btnResetCurrentRange').addEventListener('click',()=>{
    NUMERIC_RANGES[select.value]={...NUMERIC_BASE[select.value]};persistReset();display();
  });
  $('btnResetAllRanges').addEventListener('click',()=>{
    if(!confirm('Khôi phục giới hạn MIN/MAX/STEP/DEFAULT của tất cả thông số?'))return;
    for(const key of Object.keys(NUMERIC_BASE))NUMERIC_RANGES[key]={...NUMERIC_BASE[key]};
    persistReset();display();
  });
  function persistReset(){
    try{localStorage.setItem(NUMERIC_OVERRIDE_KEY,JSON.stringify(Object.fromEntries(Object.entries(NUMERIC_RANGES).map(([k,v])=>[k,{min:v.min,max:v.max,step:v.step,default:v.default}]))));}catch(e){}
    applyRangeChangeToExisting();
  }
  display();
}
/* ===== Chế độ chỉnh trực tiếp trên One View ===== */
const UI_EDIT_TARGETS={
  speed:{title:'TỐC ĐỘ GPS',options:[['speedFontSize','Cỡ chữ tốc độ'],['speedColor','Màu chữ tốc độ'],['speedAlign','Vị trí tốc độ']]},
  cup:{title:'CỐC G',options:[['cupSize','Đường kính cốc'],['cupAlign','Vị trí cốc G']]},
  xoc:{title:'Ổ GÀ & ĐƯỜNG XÓC',options:[['barHeight','Chiều cao 4 thanh'],['barFontSize','Cỡ chữ tên thanh'],['colorXoc','Màu thanh ổ gà']]},
  phanh:{title:'PHANH',options:[['barHeight','Chiều cao 4 thanh'],['barFontSize','Cỡ chữ tên thanh'],['colorPhanh','Màu thanh phanh']]},
  cua:{title:'VÀO CUA',options:[['barHeight','Chiều cao 4 thanh'],['barFontSize','Cỡ chữ tên thanh'],['colorCua','Màu thanh vào cua']]},
  ga:{title:'GA',options:[['barHeight','Chiều cao 4 thanh'],['barFontSize','Cỡ chữ tên thanh'],['colorGa','Màu thanh ga']]}
};
let uiEditMode=false,uiTarget=null;
function directUiElements(target){
  return target==='speed'?[$('speedPanel')]:target==='cup'?[$('cupCircle')]:[...document.querySelectorAll(`[data-metric="${target}"]`)];
}
function enterDirectEdit(){
  if(isRunning||isCountingDown){alert('Hãy DỪNG theo dõi và đỗ xe an toàn trước khi chỉnh giao diện.');return;}
  closeSettings();uiEditMode=true;document.body.classList.add('ui-editing');
  $('uiEditToolbar').hidden=false;$('uiEditPanel').hidden=true;uiTarget=null;
}
function leaveDirectEdit(){
  uiEditMode=false;uiTarget=null;document.body.classList.remove('ui-editing');
  $('uiEditToolbar').hidden=true;$('uiEditPanel').hidden=true;
  document.querySelectorAll('.ui-target-active').forEach(el=>el.classList.remove('ui-target-active'));
}
function chooseUiTarget(target){
  if(!uiEditMode||!UI_EDIT_TARGETS[target])return;
  uiTarget=target;
  document.querySelectorAll('.ui-target-active').forEach(el=>el.classList.remove('ui-target-active'));
  directUiElements(target).forEach(el=>el?.classList.add('ui-target-active'));
  $('uiEditTitle').textContent=UI_EDIT_TARGETS[target].title;
  const sel=$('uiEditSetting');sel.replaceChildren();
  UI_EDIT_TARGETS[target].options.forEach(([key,label])=>sel.add(new Option(label,key)));
  $('uiEditPanel').className='ui-edit-panel '+(['xoc','phanh','ga','cua'].includes(target)?'dock-top':'dock-bottom');
  $('uiEditPanel').hidden=false;renderUiEditControls();
}
function renderUiEditControls(){
  const key=$('uiEditSetting').value;
  const isNumber=Boolean(NUMERIC_RANGES[key]),isColor=/Color$/.test(key),isChoice=['cupAlign','speedAlign'].includes(key);
  $('uiEditNumeric').hidden=!isNumber;
  $('uiEditColorRow').hidden=!isColor;
  $('uiEditChoiceRow').hidden=!isChoice;
  if(isNumber){
    const r=NUMERIC_RANGES[key],el=$('uiEditSlider');el.dataset.numericKey=key;
    el.min=r.min;el.max=r.max;el.step=r.step;el.value=rangeRound(key,config[key]);
    $('uiEditValue').textContent=Number(config[key]).toFixed(r.step<1?1:0)+' px';
  }else{$('uiEditSlider').dataset.numericKey='';}
  if(isColor)$('uiEditColor').value=config[key];
  if(isChoice)$('uiEditChoice').value=config[key];
}
function updateUiValue(key,value){
  if(NUMERIC_RANGES[key])config[key]=rangeRound(key,value);
  else if(['cupAlign','speedAlign'].includes(key))config[key]=value;
  else if(/^#[0-9a-f]{6}$/i.test(value)) config[key]=value;
  else return;
  applyUiAppearance();
  if(['speedFontSize','speedColor','speedAlign'].includes(key))speedSettings();
  saveConfig();renderUiEditControls();
}
function initDirectUiEditor(){
  $('btnDirectEdit').addEventListener('click',enterDirectEdit);
  $('btnFinishUiEdit').addEventListener('click',leaveDirectEdit);
  $('btnCloseUiPanel').addEventListener('click',()=>{uiTarget=null;$('uiEditPanel').hidden=true;document.querySelectorAll('.ui-target-active').forEach(el=>el.classList.remove('ui-target-active'));});
  $('uiEditSetting').addEventListener('change',renderUiEditControls);
  for(const key of ['speed','cup','xoc','phanh','cua','ga']){
    for(const el of directUiElements(key)){
      if(!el)continue;
      el.addEventListener('click',event=>{if(!uiEditMode)return;event.preventDefault();event.stopPropagation();chooseUiTarget(key);},true);
    }
  }
  const slider=$('uiEditSlider');attachIntentionalSlider(slider);
  slider.addEventListener('input',()=>updateUiValue(slider.dataset.numericKey,Number(slider.value)));
  for(const [id,delta] of [['uiEditMinus',-1],['uiEditPlus',1]])$(id).addEventListener('click',()=>{
    const key=$('uiEditSetting').value;updateUiValue(key,rangeStep(key,delta,config[key]));
  });
  $('uiEditColor').addEventListener('input',e=>updateUiValue($('uiEditSetting').value,e.target.value));
  $('uiEditChoice').addEventListener('change',e=>updateUiValue($('uiEditSetting').value,e.target.value));
}
function initHelp(){
  $('btnHelpSmoodri').addEventListener('click',()=>{$('smoodriHelp').classList.add('open');});
  const close=()=>{$('smoodriHelp').classList.remove('open');};
  $('btnCloseHelpSmoodri').addEventListener('click',close);
  $('smoodriHelp').addEventListener('click',e=>{if(e.target.id==='smoodriHelp')close();});
  document.addEventListener('keydown',e=>{if(e.key==='Escape'){close();if(uiEditMode)leaveDirectEdit();}});
}
upgradeSettingsSliders();
initRangeEditor();
initDirectUiEditor();
initHelp();

bindSliders();
syncAllSettings();
updateAllRangeLimits();
applyUiAppearance();
$('slopeText').textContent = 'Độ dốc: chưa xác định';
