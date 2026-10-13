const API = '/api';

// Enums & Dictionaries
const SHIP_STATUS = {
  ACTIVE: 'Hoạt động',
  MAINTENANCE: 'Bảo trì kỹ thuật',
  RETIRED: 'Ngừng hoạt động'
};

const ASTRO_STATUS = {
  READY: 'Sẵn sàng',
  ON_MISSION: 'Đang làm nhiệm vụ',
  RETIRED: 'Nghỉ hưu'
};

const MISSION_STATUS = {
  PLANNING: 'Lập kế hoạch',
  COUNTDOWN: 'Đang đếm ngược',
  IN_TRANSIT: 'Đang hành trình',
  ORBITING: 'Trên quỹ đạo',
  LANDED: 'Đã đổ bộ',
  COMPLETED: 'Hoàn tất',
  ABORTED: 'Đã hủy'
};

const MISSION_PRIORITY = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  CRITICAL: 'Khẩn cấp tối cao'
};

// Global State
const state = {
  ships: [],
  astronauts: [],
  missions: [],
  stats: {},
  auditLogs: [],
  crises: [],
  role: 'DIRECTOR',
  soundEnabled: true,
  searchMission: '',
  filterMissionStatus: '',
  searchShip: '',
  filterShipStatus: '',
  searchAstro: '',
  filterAstroStatus: '',
};

const $ = (id) => document.getElementById(id);

// ================= AUDIO SYNTHESIZER (Sci-Fi FX) =================
let audioCtx = null;
function playSound(type = 'click') {
  if (!state.soundEnabled) return;
  try {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    const now = audioCtx.currentTime;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain);
    gain.connect(audioCtx.destination);

    if (type === 'click') {
      osc.type = 'sine';
      osc.frequency.setValueAtTime(800, now);
      osc.frequency.exponentialRampToValueAtTime(1200, now + 0.05);
      gain.gain.setValueAtTime(0.15, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.05);
      osc.start(now);
      osc.stop(now + 0.05);
    } else if (type === 'success') {
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(523, now); // C5
      osc.frequency.setValueAtTime(659, now + 0.08); // E5
      osc.frequency.setValueAtTime(783, now + 0.16); // G5
      gain.gain.setValueAtTime(0.2, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);
      osc.start(now);
      osc.stop(now + 0.25);
    } else if (type === 'alert') {
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(880, now);
      osc.frequency.exponentialRampToValueAtTime(440, now + 0.18);
      gain.gain.setValueAtTime(0.25, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.2);
      osc.start(now);
      osc.stop(now + 0.2);
    }
  } catch (e) {
    // AudioContext might be restricted until user interaction
  }
}

// ================= UTILITIES & DOM HELPERS =================
function h(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === 'class') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else node.setAttribute(key, value);
  }
  for (const child of children) {
    if (child !== null && child !== undefined) {
      if (typeof child === 'string' || typeof child === 'number') {
        node.appendChild(document.createTextNode(String(child)));
      } else {
        node.appendChild(child);
      }
    }
  }
  return node;
}

let toastTimer;
function toast(message, isError = false) {
  const el = $('toast');
  el.textContent = message;
  el.className = 'nusu-toast show' + (isError ? ' error' : '');
  playSound(isError ? 'alert' : 'success');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.className = 'nusu-toast'; }, 3400);
}

// REST API Client
async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Actor': state.role + ' Operator',
    ...(options.headers || {})
  };

  const response = await fetch(API + path, {
    ...options,
    headers
  });

  if (!response.ok) {
    let message = 'Lỗi hệ thống (' + response.status + ')';
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch (e) { /* ignore */ }
    throw new Error(message);
  }

  return response.status === 204 ? null : response.json();
}

function crewCount(shipId) {
  return state.astronauts.filter((a) => a.spaceship && a.spaceship.id === shipId).length;
}

function checkPermission(requiredRole = 'ENGINEER') {
  if (state.role === 'GUEST') {
    toast('Khách tham quan chỉ có quyền xem dữ liệu (Read-only)', true);
    return false;
  }
  return true;
}

// ================= LIVE CLOCKS & TELEMETRY =================
function startEarthClock() {
  setInterval(() => {
    const now = new Date();
    $('utc-clock').textContent = now.toUTCString().slice(17, 25) + ' UTC';
  }, 1000);
}

async function fetchTelemetryLoop() {
  try {
    const data = await request('/simulation/telemetry');
    $('tele-iss-alt').textContent = data.issAltitudeKm + ' km';
    $('tele-iss-vel').textContent = data.issVelocityKmS + ' km/s';
    $('tele-pressure').textContent = data.cabinPressureKpa + ' kPa';
  } catch (err) {
    // silent fallback
  }
  setTimeout(fetchTelemetryLoop, 5000);
}

function startCountdown() {
  // Target: 48 days from now
  const targetDate = new Date(Date.now() + 48 * 24 * 3600 * 1000 + 14 * 3600 * 1000);
  setInterval(() => {
    const diff = targetDate - Date.now();
    if (diff <= 0) return;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
    const mins = Math.floor((diff / 1000 / 60) % 60);
    const secs = Math.floor((diff / 1000) % 60);

    $('cd-days').textContent = String(days).padStart(2, '0');
    $('cd-hours').textContent = String(hours).padStart(2, '0');
    $('cd-mins').textContent = String(mins).padStart(2, '0');
    $('cd-secs').textContent = String(secs).padStart(2, '0');
  }, 1000);
}

// ================= RENDER FUNCTIONS =================

function renderStats(stats) {
  $('stat-missions').textContent = stats.missions || 0;
  $('stat-active-missions').textContent = (stats.activeMissions || 0) + ' đang hoạt động';
  $('stat-ships').textContent = stats.ships || 0;
  $('stat-active-ships').textContent = (stats.activeShips || 0) + ' sẵn sàng bay';
  $('stat-astronauts').textContent = stats.astronauts || 0;
  $('stat-mission-astro').textContent = (stats.onMission || 0) + ' trên quỹ đạo';
  $('stat-fuel').textContent = stats.avgFuel || 0;
  $('stat-oxygen').textContent = stats.avgOxygen || 0;
  $('stat-budget').textContent = stats.totalBudgetMillions || 0;
}

// Render Overview Highlights
function renderOverview() {
  // Highlight active missions
  const activeMissions = state.missions.filter(m => m.status === 'IN_TRANSIT' || m.status === 'ORBITING' || m.status === 'COUNTDOWN');
  const cards = (activeMissions.length ? activeMissions : state.missions.slice(0, 3)).map(m =>
    h('div', { class: 'mini-mission-card' },
      h('div', {},
        h('div', { class: 'mm-code' }, m.code),
        h('div', { class: 'mm-name' }, m.name),
        h('div', { class: 'mm-dest' }, `📍 ${m.destination} • Tàu: ${m.spaceship ? m.spaceship.name : 'Chưa gán'}`)
      ),
      h('span', { class: 'badge-status ' + m.status.toLowerCase() }, MISSION_STATUS[m.status] || m.status)
    )
  );
  $('overview-missions-list').replaceChildren(...(cards.length ? cards : [h('div', { class: 'text-dim' }, 'Chưa có chiến dịch nào')]));
}

async function loadRecentLogsStream() {
  try {
    const logs = await request('/missions/logs/recent');
    const items = logs.map(l =>
      h('div', { class: 'stream-log-item ' + l.level.toLowerCase() },
        h('div', { class: 'log-meta' }, `[${l.level}] ${new Date(l.timestamp).toLocaleTimeString()} • ${l.mission ? l.mission.code : 'SYS'}`),
        h('div', {}, l.message)
      )
    );
    $('overview-logs-stream').replaceChildren(...(items.length ? items : [h('div', { class: 'text-dim' }, 'Chưa có thông điệp viễn trắc')]));
  } catch (e) { /* ignore */ }
}

// Render Missions Tab
function renderMissions() {
  let list = state.missions;
  if (state.searchMission) {
    const q = state.searchMission.toLowerCase();
    list = list.filter(m => m.code.toLowerCase().includes(q) || m.name.toLowerCase().includes(q) || m.destination.toLowerCase().includes(q));
  }
  if (state.filterMissionStatus) {
    list = list.filter(m => m.status === state.filterMissionStatus);
  }

  const cards = list.map(m => {
    return h('div', { class: 'mission-card' },
      h('div', { class: 'mc-header' },
        h('div', {},
          h('div', { class: 'mc-code' }, m.code),
          h('h4', { class: 'mc-title' }, m.name),
          h('div', { class: 'mc-dest' }, `📍 ${m.destination}`)
        ),
        h('span', { class: 'badge-status ' + m.status.toLowerCase() }, MISSION_STATUS[m.status] || m.status)
      ),
      h('div', { class: 'mc-body' }, m.primaryObjective || 'Chưa thiết lập mục tiêu chi tiết.'),
      h('div', { class: 'mc-meta-grid' },
        h('div', { class: 'mc-meta-item' },
          h('span', { class: 'mc-meta-label' }, 'TÀU CHỈ ĐỊNH'),
          h('span', { class: 'mc-meta-val text-cyan' }, m.spaceship ? m.spaceship.name : 'Chưa phân công')
        ),
        h('div', { class: 'mc-meta-item' },
          h('span', { class: 'mc-meta-label' }, 'ƯU TIÊN'),
          h('span', { class: 'mc-meta-val' }, MISSION_PRIORITY[m.priority] || m.priority)
        ),
        h('div', { class: 'mc-meta-item' },
          h('span', { class: 'mc-meta-label' }, 'NGÀY PHÓNG'),
          h('span', { class: 'mc-meta-val' }, m.launchDate || 'Chưa định')
        ),
        h('div', { class: 'mc-meta-item' },
          h('span', { class: 'mc-meta-label' }, 'NGÂN SÁCH'),
          h('span', { class: 'mc-meta-val text-gold' }, `$${m.budgetMillions}M USD`)
        )
      ),
      h('div', { class: 'mc-footer' },
        h('button', { class: 'btn btn-secondary btn-sm', onclick: () => openLogsModal(m) }, '📜 Nhật ký'),
        h('div', { class: 'btn-group' },
          h('button', { class: 'btn btn-sm', onclick: () => openMissionModal(m) }, 'Sửa'),
          h('button', { class: 'btn btn-sm btn-danger', onclick: () => deleteMission(m) }, 'Hủy')
        )
      )
    );
  });

  $('missions-container').replaceChildren(...(cards.length ? cards : [h('div', { class: 'empty-notice' }, 'Không tìm thấy chiến dịch phù hợp')]));
}

// Render Spaceships Table
function renderShips() {
  let list = state.ships;
  if (state.searchShip) {
    const q = state.searchShip.toLowerCase();
    list = list.filter(s => s.name.toLowerCase().includes(q) || s.model.toLowerCase().includes(q));
  }
  if (state.filterShipStatus) {
    list = list.filter(s => s.status === state.filterShipStatus);
  }

  const rows = list.map(ship => {
    const crew = crewCount(ship.id);
    return h('tr', {},
      h('td', { class: 'text-cyan font-bold' }, `🛸 ${ship.name}`),
      h('td', {},
        h('div', {}, ship.model),
        h('small', { class: 'text-dim' }, `${ship.manufacturer} (${ship.launchYear || 2024})`)
      ),
      h('td', {}, `${crew}/${ship.capacity} người`),
      h('td', {},
        h('div', { class: 'gauge-wrap' },
          h('div', { class: 'gauge-bar-bg' },
            h('div', { class: 'gauge-bar-fill fuel', style: `width: ${ship.fuelLevel}%` })
          ),
          h('span', { class: 'gauge-num text-amber' }, `${ship.fuelLevel}%`)
        )
      ),
      h('td', {},
        h('div', { class: 'gauge-wrap' },
          h('div', { class: 'gauge-bar-bg' },
            h('div', { class: 'gauge-bar-fill oxygen', style: `width: ${ship.oxygenLevel}%` })
          ),
          h('span', { class: 'gauge-num text-cyan' }, `${ship.oxygenLevel}%`)
        )
      ),
      h('td', { class: 'text-cyan' }, `${ship.speedKmS} km/s`),
      h('td', {}, `${ship.cargoCapacityTons} T`),
      h('td', {}, h('span', { class: 'badge-status ' + ship.status.toLowerCase() }, SHIP_STATUS[ship.status] || ship.status)),
      h('td', { class: 'text-right' },
        h('button', { class: 'btn btn-sm', onclick: () => openShipModal(ship) }, 'Sửa'),
        h('button', { class: 'btn btn-sm btn-danger', onclick: () => deleteShip(ship) }, 'Xóa')
      )
    );
  });

  $('ships-tbody').replaceChildren(...(rows.length ? rows : [
    h('tr', {}, h('td', { colspan: '9', class: 'text-center text-dim' }, 'Không tìm thấy tàu vũ trụ phù hợp'))
  ]));
}

// Render Astronauts Grid
function renderAstronauts() {
  let list = state.astronauts;
  if (state.searchAstro) {
    const q = state.searchAstro.toLowerCase();
    list = list.filter(a => a.fullName.toLowerCase().includes(q) || (a.nationality && a.nationality.toLowerCase().includes(q)));
  }
  if (state.filterAstroStatus) {
    list = list.filter(a => a.status === state.filterAstroStatus);
  }

  const cards = list.map(a => {
    const badges = (a.badges || '').split(',').map(b => b.trim()).filter(Boolean);
    return h('div', { class: 'astro-card' },
      h('div', { class: 'ac-top' },
        h('div', { class: 'ac-avatar' }, '👨‍🚀'),
        h('div', { class: 'ac-info' },
          h('div', { class: 'ac-name' }, a.fullName),
          h('div', { class: 'ac-rank' }, `${a.rankName} • ${a.nationality || 'Quốc tế'}`),
          h('span', { class: 'badge-status ' + a.status.toLowerCase() }, ASTRO_STATUS[a.status] || a.status)
        )
      ),
      h('div', { class: 'ac-vitals-grid' },
        h('div', { class: 'vital-item' },
          h('span', { class: 'vital-label' }, 'NHỊP TIM'),
          h('span', { class: 'vital-val text-green' }, `❤️ ${a.heartRate} bpm`)
        ),
        h('div', { class: 'vital-item' },
          h('span', { class: 'vital-label' }, 'SpO₂ OXY'),
          h('span', { class: 'vital-val text-cyan' }, `${a.oxygenSaturation}%`)
        ),
        h('div', { class: 'vital-item' },
          h('span', { class: 'vital-label' }, 'BỨC XẠ'),
          h('span', { class: 'vital-val text-amber' }, `${a.radiationLevel} mSv`)
        )
      ),
      h('div', { class: 'ac-details' },
        h('div', { class: 'mc-meta-grid' },
          h('div', { class: 'mc-meta-item' },
            h('span', { class: 'mc-meta-label' }, 'TÀU PHÂN CÔNG'),
            h('span', { class: 'mc-meta-val' }, a.spaceship ? a.spaceship.name : 'Chưa phân công')
          ),
          h('div', { class: 'mc-meta-item' },
            h('span', { class: 'mc-meta-label' }, 'CHIẾN DỊCH'),
            h('span', { class: 'mc-meta-val text-purple' }, a.mission ? a.mission.code : 'Chưa tham gia')
          )
        ),
        h('div', { class: 'badge-tag-wrap', style: 'margin-top: 8px;' },
          ...badges.map(b => h('span', { class: 'badge-tag' }, `🏅 ${b}`))
        )
      ),
      h('div', { class: 'mc-footer' },
        h('span', { class: 'text-dim', style: 'font-size: 0.75rem;' }, `Bay: ${a.spaceHours} giờ • Máu: ${a.bloodType || 'O+'}`),
        h('div', { class: 'btn-group' },
          h('button', { class: 'btn btn-sm', onclick: () => openAstroModal(a) }, 'Sửa'),
          h('button', { class: 'btn btn-sm btn-danger', onclick: () => deleteAstronaut(a) }, 'Xóa')
        )
      )
    );
  });

  $('astronauts-container').replaceChildren(...(cards.length ? cards : [
    h('div', { class: 'empty-notice' }, 'Không tìm thấy phi hành gia phù hợp')
  ]));
}

// Render Crisis Drills Tab
async function loadCrisisDrills() {
  try {
    const crises = await request('/simulation/crisis');
    state.crises = crises;
    const cards = crises.map(c => {
      const options = c.options.map(opt =>
        h('button', {
          class: 'crisis-opt-btn',
          onclick: () => resolveCrisis(c.id, opt.id, opt.text)
        },
        h('span', {}, `► ${opt.text}`),
        h('span', { class: 'opt-score' }, opt.costScore)
      ));

      return h('div', { class: 'crisis-card' },
        h('div', { class: 'crisis-header' },
          h('div', { class: 'crisis-title' }, `⚠️ ${c.title}`),
          h('span', { class: 'badge-mini text-danger' }, c.severity)
        ),
        h('div', { class: 'crisis-desc' }, c.description),
        h('div', { class: 'crisis-options' }, ...options)
      );
    });

    $('crisis-container').replaceChildren(...cards);
  } catch (e) {
    $('crisis-container').textContent = 'Không tải được kịch bản khủng hoảng';
  }
}

async function resolveCrisis(crisisId, choiceId, choiceText) {
  if (!checkPermission()) return;
  try {
    const result = await request('/simulation/crisis/resolve', {
      method: 'POST',
      body: JSON.stringify({ crisisId, choiceId, choiceText })
    });
    toast(`${result.message} (${result.commanderScore})`, !result.success);
    await loadAuditLogs();
    await loadRecentLogsStream();
  } catch (err) {
    toast(err.message, true);
  }
}

// Render Discovery (NASA APOD)
async function loadDiscovery() {
  try {
    const apod = await request('/simulation/nasa/apod');
    $('apod-img').src = apod.url;
    $('apod-title').textContent = apod.title;
    $('apod-author').textContent = `Nguồn: ${apod.copyright} • Ngày: ${apod.date}`;
    $('apod-explanation').textContent = apod.explanation;
    $('apod-date').textContent = apod.date;
  } catch (e) {
    $('apod-title').textContent = 'Thiên văn học sâu: Cực quang & Dải Ngân Hà';
  }
}

// Render Audit Logs Table
async function loadAuditLogs() {
  try {
    const logs = await request('/audit-logs');
    state.auditLogs = logs;
    const rows = logs.map(l =>
      h('tr', {},
        h('td', { class: 'text-dim' }, new Date(l.timestamp).toLocaleString()),
        h('td', { class: 'text-cyan font-bold' }, l.action),
        h('td', { class: 'text-purple' }, l.actor),
        h('td', {}, l.details)
      )
    );
    $('audit-tbody').replaceChildren(...(rows.length ? rows : [
      h('tr', {}, h('td', { colspan: '4', class: 'text-dim' }, 'Chưa có dữ liệu kiểm toán'))
    ]));
  } catch (e) { /* ignore */ }
}

// ================= MODAL HANDLERS & CRUD =================

// Ship Modal
function openShipModal(ship) {
  if (!checkPermission()) return;
  $('ship-modal-title').textContent = ship ? 'Cập Nhật Tàu Vũ Trụ' : 'Đăng Ký Tàu Vũ Trụ Mới';
  $('ship-id').value = ship ? ship.id : '';
  $('ship-name').value = ship ? ship.name : '';
  $('ship-model').value = ship ? ship.model : '';
  $('ship-capacity').value = ship ? ship.capacity : 4;
  $('ship-status').value = ship ? ship.status : 'ACTIVE';
  $('ship-fuel').value = ship ? ship.fuelLevel : 100;
  $('ship-oxygen').value = ship ? ship.oxygenLevel : 100;
  $('ship-speed').value = ship ? ship.speedKmS : 7.8;
  $('ship-cargo').value = ship ? ship.cargoCapacityTons : 15.0;
  $('ship-manufacturer').value = ship ? (ship.manufacturer || 'NUSU Aerospace') : 'NUSU Aerospace';
  $('ship-year').value = ship ? (ship.launchYear || 2024) : 2024;
  $('ship-modal').showModal();
}

async function deleteShip(ship) {
  if (!checkPermission()) return;
  if (!confirm(`Xác nhận xóa tàu vũ trụ "${ship.name}"? Phi hành đoàn trên tàu sẽ chuyển về chưa gán.`)) return;
  try {
    await request(`/spaceships/${ship.id}`, { method: 'DELETE' });
    toast(`Đã xóa tàu ${ship.name}`);
    await refreshAll();
  } catch (err) {
    toast(err.message, true);
  }
}

// Astronaut Modal
function openAstroModal(a) {
  if (!checkPermission()) return;
  $('astro-modal-title').textContent = a ? 'Cập Nhật Hồ Sơ Phi Hành Gia' : 'Tuyển Chọn Phi Hành Gia Mới';
  $('astro-id').value = a ? a.id : '';
  $('astro-name').value = a ? a.fullName : '';
  $('astro-rank').value = a ? a.rankName : '';
  $('astro-nationality').value = a ? (a.nationality || '') : 'Việt Nam';
  $('astro-blood').value = a ? (a.bloodType || 'O+') : 'O+';
  $('astro-status').value = a ? a.status : 'READY';
  $('astro-hours').value = a ? a.spaceHours : 100;
  $('astro-bpm').value = a ? a.heartRate : 72;
  $('astro-spo2').value = a ? a.oxygenSaturation : 98;
  $('astro-badges').value = a ? (a.badges || 'CREW_CERTIFIED') : 'CREW_CERTIFIED,EVA_SPECIALIST';
  $('astro-bio').value = a ? (a.bio || '') : '';

  // Options for Ships
  const shipOpts = [h('option', { value: '' }, 'Chưa phân công')];
  for (const s of state.ships) {
    shipOpts.push(h('option', { value: String(s.id) }, `${s.name} (${crewCount(s.id)}/${s.capacity})`));
  }
  $('astro-ship').replaceChildren(...shipOpts);
  $('astro-ship').value = a && a.spaceship ? String(a.spaceship.id) : '';

  // Options for Missions
  const missionOpts = [h('option', { value: '' }, 'Chưa tham gia chiến dịch')];
  for (const m of state.missions) {
    missionOpts.push(h('option', { value: String(m.id) }, `${m.code} - ${m.name}`));
  }
  $('astro-mission').replaceChildren(...missionOpts);
  $('astro-mission').value = a && a.mission ? String(a.mission.id) : '';

  $('astro-modal').showModal();
}

async function deleteAstronaut(a) {
  if (!checkPermission()) return;
  if (!confirm(`Xác nhận xóa hồ sơ phi hành gia "${a.fullName}"?`)) return;
  try {
    await request(`/astronauts/${a.id}`, { method: 'DELETE' });
    toast(`Đã xóa hồ sơ ${a.fullName}`);
    await refreshAll();
  } catch (err) {
    toast(err.message, true);
  }
}

// Mission Modal
function openMissionModal(m) {
  if (!checkPermission()) return;
  $('mission-modal-title').textContent = m ? 'Cập Nhật Chiến Dịch Vũ Trụ' : 'Khởi Tạo Chiến Dịch Vũ Trụ Mới';
  $('mission-id').value = m ? m.id : '';
  $('mission-code').value = m ? m.code : '';
  $('mission-name').value = m ? m.name : '';
  $('mission-dest').value = m ? m.destination : '';
  $('mission-priority').value = m ? m.priority : 'MEDIUM';
  $('mission-status').value = m ? m.status : 'PLANNING';
  $('mission-launch').value = m ? (m.launchDate || '') : '';
  $('mission-return').value = m ? (m.returnDate || '') : '';
  $('mission-budget').value = m ? m.budgetMillions : 250.0;
  $('mission-objective').value = m ? (m.primaryObjective || '') : '';

  const shipOpts = [h('option', { value: '' }, 'Chưa gán tàu vũ trụ')];
  for (const s of state.ships) {
    shipOpts.push(h('option', { value: String(s.id) }, `${s.name} (${s.model})`));
  }
  $('mission-ship').replaceChildren(...shipOpts);
  $('mission-ship').value = m && m.spaceship ? String(m.spaceship.id) : '';

  $('mission-modal').showModal();
}

async function deleteMission(m) {
  if (!checkPermission()) return;
  if (!confirm(`Hủy / Xóa chiến dịch "${m.code} - ${m.name}"?`)) return;
  try {
    await request(`/missions/${m.id}`, { method: 'DELETE' });
    toast(`Đã hủy chiến dịch ${m.code}`);
    await refreshAll();
  } catch (err) {
    toast(err.message, true);
  }
}

// Mission Logs Modal
async function openLogsModal(mission) {
  $('logs-modal-title').textContent = `Nhật Ký: ${mission.code} - ${mission.name}`;
  $('log-mission-id').value = mission.id;
  await loadMissionLogs(mission.id);
  $('logs-modal').showModal();
}

async function loadMissionLogs(missionId) {
  try {
    const logs = await request(`/missions/${missionId}/logs`);
    const items = logs.map(l =>
      h('div', { class: 'timeline-item ' + l.level.toLowerCase() },
        h('div', { class: 'log-meta' }, `[${l.level}] ${new Date(l.timestamp).toLocaleString()}`),
        h('div', {}, l.message)
      )
    );
    $('mission-timeline').replaceChildren(...(items.length ? items : [h('div', { class: 'text-dim' }, 'Chưa có bản ghi nhật ký nào.')]));
  } catch (e) {
    toast('Lỗi tải nhật ký chiến dịch', true);
  }
}

// ================= LOAD ALL DATA =================
async function refreshAll() {
  try {
    const [ships, astronauts, missions, stats] = await Promise.all([
      request('/spaceships'),
      request('/astronauts'),
      request('/missions'),
      request('/stats')
    ]);
    state.ships = ships;
    state.astronauts = astronauts;
    state.missions = missions;
    state.stats = stats;

    renderStats(stats);
    renderOverview();
    renderMissions();
    renderShips();
    renderAstronauts();
    await loadRecentLogsStream();
  } catch (err) {
    toast(err.message, true);
  }
}

// ================= EVENT LISTENERS SETUP =================
function setupEventListeners() {
  // Navigation Tabs
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      playSound('click');
      document.querySelectorAll('.nav-tab').forEach(t => t.classList.toggle('active', t === tab));
      const targetPanel = 'panel-' + tab.dataset.tab;
      document.querySelectorAll('.view-panel').forEach(p => p.classList.toggle('active', p.id === targetPanel));

      // Trigger lazy loads
      if (tab.dataset.tab === 'simulation') loadCrisisDrills();
      else if (tab.dataset.tab === 'discovery') loadDiscovery();
      else if (tab.dataset.tab === 'audit') loadAuditLogs();
    });
  });

  // Overview Quick Link to Missions Tab
  $('go-missions-tab').addEventListener('click', () => {
    $('tab-btn-missions').click();
  });

  // Audio Toggle
  $('audio-toggle').addEventListener('click', () => {
    state.soundEnabled = !state.soundEnabled;
    $('audio-icon').textContent = state.soundEnabled ? '🔊' : '🔇';
    if (state.soundEnabled) playSound('click');
  });

  // Role Selector
  $('role-select').addEventListener('change', (e) => {
    state.role = e.target.value;
    toast(`Đã chuyển vai trò sang: ${e.target.options[e.target.selectedIndex].text}`);
    playSound('click');
  });

  // Modal Closers
  document.querySelectorAll('[data-close]').forEach(btn => {
    btn.addEventListener('click', () => {
      $(btn.dataset.close).close();
      playSound('click');
    });
  });

  // Filter & Search Inputs
  $('search-mission').addEventListener('input', (e) => { state.searchMission = e.target.value; renderMissions(); });
  $('filter-mission-status').addEventListener('change', (e) => { state.filterMissionStatus = e.target.value; renderMissions(); });

  $('search-ship').addEventListener('input', (e) => { state.searchShip = e.target.value; renderShips(); });
  $('filter-ship-status').addEventListener('change', (e) => { state.filterShipStatus = e.target.value; renderShips(); });

  $('search-astro').addEventListener('input', (e) => { state.searchAstro = e.target.value; renderAstronauts(); });
  $('filter-astro-status').addEventListener('change', (e) => { state.filterAstroStatus = e.target.value; renderAstronauts(); });

  // Add Buttons
  $('btn-add-ship').addEventListener('click', () => openShipModal(null));
  $('btn-add-astronaut').addEventListener('click', () => openAstroModal(null));
  $('btn-add-mission').addEventListener('click', () => openMissionModal(null));
  $('btn-refresh-audit').addEventListener('click', () => { playSound('click'); loadAuditLogs(); });

  // Forms Submissions
  $('ship-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!checkPermission()) return;
    const id = $('ship-id').value;
    const body = {
      name: $('ship-name').value,
      model: $('ship-model').value,
      capacity: Number($('ship-capacity').value),
      status: $('ship-status').value,
      fuelLevel: Number($('ship-fuel').value),
      oxygenLevel: Number($('ship-oxygen').value),
      speedKmS: Number($('ship-speed').value),
      cargoCapacityTons: Number($('ship-cargo').value),
      manufacturer: $('ship-manufacturer').value,
      launchYear: Number($('ship-year').value)
    };
    try {
      await request(id ? `/spaceships/${id}` : '/spaceships', {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(body)
      });
      $('ship-modal').close();
      toast('Đã lưu thông tin tàu vũ trụ thành công');
      await refreshAll();
    } catch (err) {
      toast(err.message, true);
    }
  });

  $('astro-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!checkPermission()) return;
    const id = $('astro-id').value;
    const shipId = $('astro-ship').value;
    const missionId = $('astro-mission').value;
    const body = {
      fullName: $('astro-name').value,
      rankName: $('astro-rank').value,
      nationality: $('astro-nationality').value,
      status: $('astro-status').value,
      spaceshipId: shipId ? Number(shipId) : null,
      missionId: missionId ? Number(missionId) : null,
      bloodType: $('astro-blood').value,
      spaceHours: Number($('astro-hours').value),
      heartRate: Number($('astro-bpm').value),
      oxygenSaturation: Number($('astro-spo2').value),
      badges: $('astro-badges').value,
      bio: $('astro-bio').value
    };
    try {
      await request(id ? `/astronauts/${id}` : '/astronauts', {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(body)
      });
      $('astro-modal').close();
      toast('Đã lưu hồ sơ phi hành gia thành công');
      await refreshAll();
    } catch (err) {
      toast(err.message, true);
    }
  });

  $('mission-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!checkPermission()) return;
    const id = $('mission-id').value;
    const shipId = $('mission-ship').value;
    const body = {
      code: $('mission-code').value,
      name: $('mission-name').value,
      destination: $('mission-dest').value,
      priority: $('mission-priority').value,
      status: $('mission-status').value,
      spaceshipId: shipId ? Number(shipId) : null,
      launchDate: $('mission-launch').value || null,
      returnDate: $('mission-return').value || null,
      budgetMillions: Number($('mission-budget').value),
      primaryObjective: $('mission-objective').value
    };
    try {
      await request(id ? `/missions/${id}` : '/missions', {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(body)
      });
      $('mission-modal').close();
      toast('Đã lưu thông tin chiến dịch vũ trụ thành công');
      await refreshAll();
    } catch (err) {
      toast(err.message, true);
    }
  });

  // Add Log form
  $('add-log-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!checkPermission()) return;
    const missionId = $('log-mission-id').value;
    const body = {
      missionId: Number(missionId),
      level: $('log-level').value,
      message: $('log-text').value
    };
    try {
      await request(`/missions/${missionId}/logs`, {
        method: 'POST',
        body: JSON.stringify(body)
      });
      $('log-text').value = '';
      toast('Đã ghi nhật ký biến cố');
      await loadMissionLogs(missionId);
      await loadRecentLogsStream();
    } catch (err) {
      toast(err.message, true);
    }
  });

  // Comms Broadcast Form
  $('comms-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const channel = $('comms-channel').value;
    const text = $('comms-message').value;
    try {
      await request('/audit-logs', {
        method: 'POST',
        body: JSON.stringify({
          action: 'RADIO_TRANSMISSION',
          actor: state.role + ' Operator',
          details: `[Kênh ${channel}] ${text}`
        })
      });
      $('comms-message').value = '';
      toast('Đã phát bức điện vô tuyến thành công');
      await loadAuditLogs();
      await loadRecentLogsStream();
    } catch (err) {
      toast(err.message, true);
    }
  });

  // Orbital Calculator
  $('btn-run-calc').addEventListener('click', async () => {
    playSound('click');
    const dest = $('calc-dest').value;
    const payload = $('calc-payload').value;
    const crew = $('calc-crew').value;
    try {
      const res = await request(`/simulation/calculator?destination=${dest}&payloadTons=${payload}&crewCount=${crew}`);
      $('res-deltav').textContent = res.deltaVKmS + ' km/s';
      $('res-fuel').textContent = res.fuelNeededTons + ' tấn';
      $('res-days').textContent = res.transitDays + ' ngày';
      $('res-cost').textContent = '$' + res.estimatedCostMillionsUSD + 'M USD';
      toast('Đã hoàn tất tính toán quỹ đạo');
    } catch (err) {
      toast(err.message, true);
    }
  });
}

// ================= BOOTSTRAP =================
window.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  startEarthClock();
  startCountdown();
  fetchTelemetryLoop();
  refreshAll();
});
