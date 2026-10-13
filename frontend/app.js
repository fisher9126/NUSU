const API = '/api';

const SHIP_STATUS = { ACTIVE: 'Hoạt động', MAINTENANCE: 'Bảo trì', RETIRED: 'Ngừng hoạt động' };
const ASTRO_STATUS = { READY: 'Sẵn sàng', ON_MISSION: 'Đang làm nhiệm vụ', RETIRED: 'Nghỉ hưu' };

const state = { ships: [], astronauts: [] };
const $ = (id) => document.getElementById(id);

function h(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === 'class') node.className = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else node.setAttribute(key, value);
  }
  node.append(...children.filter((child) => child !== null && child !== undefined));
  return node;
}

let toastTimer;
function toast(message, isError = false) {
  const el = $('toast');
  el.textContent = message;
  el.className = 'show' + (isError ? ' error' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.className = ''; }, 3200);
}

async function request(path, options = {}) {
  const response = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!response.ok) {
    let message = 'Lỗi ' + response.status;
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch (ignored) { /* response without JSON body */ }
    throw new Error(message);
  }
  return response.status === 204 ? null : response.json();
}

function badge(label, status) {
  return h('span', { class: 'badge ' + status.toLowerCase() }, label);
}

function crewCount(shipId) {
  return state.astronauts.filter((a) => a.spaceship && a.spaceship.id === shipId).length;
}

function renderStats(stats) {
  $('stat-ships').textContent = stats.ships;
  $('stat-active').textContent = stats.activeShips;
  $('stat-astronauts').textContent = stats.astronauts;
  $('stat-mission').textContent = stats.onMission;
}

function emptyRow(columns, text) {
  return h('tr', {}, h('td', { class: 'empty', colspan: String(columns) }, text));
}

function renderShips() {
  const rows = state.ships.map((ship) =>
    h('tr', {},
      h('td', {}, ship.name),
      h('td', {}, ship.model),
      h('td', {}, `${crewCount(ship.id)}/${ship.capacity}`),
      h('td', {}, badge(SHIP_STATUS[ship.status], ship.status)),
      h('td', { class: 'actions' },
        h('button', { class: 'btn small', onclick: () => openShipDialog(ship) }, 'Sửa'),
        h('button', { class: 'btn small danger', onclick: () => removeShip(ship) }, 'Xóa'))));
  $('ships-body').replaceChildren(...(rows.length ? rows : [emptyRow(5, 'Chưa có tàu vũ trụ nào')]));
}

function renderAstronauts() {
  const rows = state.astronauts.map((a) =>
    h('tr', {},
      h('td', {}, a.fullName),
      h('td', {}, a.rankName),
      h('td', {}, a.nationality || '-'),
      h('td', {}, badge(ASTRO_STATUS[a.status], a.status)),
      h('td', {}, a.spaceship ? a.spaceship.name : 'Chưa phân công'),
      h('td', { class: 'actions' },
        h('button', { class: 'btn small', onclick: () => openAstroDialog(a) }, 'Sửa'),
        h('button', { class: 'btn small danger', onclick: () => removeAstronaut(a) }, 'Xóa'))));
  $('astronauts-body').replaceChildren(...(rows.length ? rows : [emptyRow(6, 'Chưa có phi hành gia nào')]));
}

async function loadAll() {
  try {
    const [ships, astronauts, stats] = await Promise.all([
      request('/spaceships'),
      request('/astronauts'),
      request('/stats'),
    ]);
    state.ships = ships;
    state.astronauts = astronauts;
    renderStats(stats);
    renderShips();
    renderAstronauts();
  } catch (error) {
    toast(error.message, true);
  }
}

function openShipDialog(ship) {
  $('shipTitle').textContent = ship ? 'Sửa tàu vũ trụ' : 'Thêm tàu vũ trụ';
  $('ship-id').value = ship ? ship.id : '';
  $('ship-name').value = ship ? ship.name : '';
  $('ship-model').value = ship ? ship.model : '';
  $('ship-capacity').value = ship ? ship.capacity : 4;
  $('ship-status').value = ship ? ship.status : 'ACTIVE';
  $('shipDialog').showModal();
}

function openAstroDialog(astronaut) {
  $('astroTitle').textContent = astronaut ? 'Sửa phi hành gia' : 'Thêm phi hành gia';
  $('astro-id').value = astronaut ? astronaut.id : '';
  $('astro-name').value = astronaut ? astronaut.fullName : '';
  $('astro-rank').value = astronaut ? astronaut.rankName : '';
  $('astro-nationality').value = astronaut ? astronaut.nationality || '' : '';
  $('astro-status').value = astronaut ? astronaut.status : 'READY';

  const options = [h('option', { value: '' }, 'Chưa phân công')];
  for (const ship of state.ships) {
    options.push(h('option', { value: String(ship.id) }, `${ship.name} (${crewCount(ship.id)}/${ship.capacity})`));
  }
  $('astro-ship').replaceChildren(...options);
  $('astro-ship').value = astronaut && astronaut.spaceship ? String(astronaut.spaceship.id) : '';
  $('astroDialog').showModal();
}

async function save(path, id, body, dialogId) {
  try {
    await request(id ? `${path}/${id}` : path, {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(body),
    });
    $(dialogId).close();
    toast('Đã lưu');
    await loadAll();
  } catch (error) {
    toast(error.message, true);
  }
}

async function remove(path, id, name) {
  if (!confirm(`Xóa "${name}"?`)) return;
  try {
    await request(`${path}/${id}`, { method: 'DELETE' });
    toast('Đã xóa');
    await loadAll();
  } catch (error) {
    toast(error.message, true);
  }
}

const removeShip = (ship) => remove('/spaceships', ship.id, ship.name);
const removeAstronaut = (a) => remove('/astronauts', a.id, a.fullName);

$('shipForm').addEventListener('submit', (event) => {
  event.preventDefault();
  save('/spaceships', $('ship-id').value, {
    name: $('ship-name').value,
    model: $('ship-model').value,
    capacity: Number($('ship-capacity').value),
    status: $('ship-status').value,
  }, 'shipDialog');
});

$('astroForm').addEventListener('submit', (event) => {
  event.preventDefault();
  const shipId = $('astro-ship').value;
  save('/astronauts', $('astro-id').value, {
    fullName: $('astro-name').value,
    rankName: $('astro-rank').value,
    nationality: $('astro-nationality').value,
    status: $('astro-status').value,
    spaceshipId: shipId ? Number(shipId) : null,
  }, 'astroDialog');
});

$('add-ship').addEventListener('click', () => openShipDialog(null));
$('add-astronaut').addEventListener('click', () => openAstroDialog(null));

document.querySelectorAll('[data-close]').forEach((button) => {
  button.addEventListener('click', () => $(button.dataset.close).close());
});

document.querySelectorAll('.tab').forEach((tab) => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t === tab));
    $('tab-ships').hidden = tab.dataset.tab !== 'ships';
    $('tab-astronauts').hidden = tab.dataset.tab !== 'astronauts';
  });
});

loadAll();
