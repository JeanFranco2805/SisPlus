const API_BASE = '/api';

let allAttendances      = [];
let filteredAttendances = [];
let employees           = [];
const PER_PAGE          = 10;
let currentPage         = 1;
let mapInstance         = null;
let mapVisible          = false;

const AVATAR_COLORS = [
    { bg:'rgba(30,58,138,.1)',  color:'#1E3A8A' },
    { bg:'rgba(13,148,136,.1)', color:'#0D9488' },
    { bg:'rgba(217,119,6,.1)',  color:'#B45309' },
    { bg:'rgba(225,29,72,.1)',  color:'#E11D48' },
    { bg:'rgba(124,58,237,.1)',color:'#7C3AED' },
    { bg:'rgba(5,150,105,.1)',  color:'#059669' },
];

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const main    = document.getElementById('mainContent');
    if (window.innerWidth <= 900) {
        sidebar.classList.toggle('open');
    } else {
        sidebar.classList.toggle('closed');
        main.classList.toggle('expanded');
    }
}
window.addEventListener('resize', () => {
    if (window.innerWidth > 900) document.getElementById('sidebar').classList.remove('open');
});

function initClock() {
    const el = document.getElementById('topbarTime');
    if (!el) return;
    const tick = () => { el.textContent = new Date().toLocaleTimeString('es-CO', { hour:'2-digit', minute:'2-digit', hour12:true }); };
    tick(); setInterval(tick, 30000);
    const y = new Date().getFullYear();
    const fv = document.getElementById('footerVersion');
    const fm = document.getElementById('footerYearMain');
    if (fv) fv.textContent = `v2.0 · ${y}`;
    if (fm) fm.textContent = y;
}

function showAlert(msg, type = 'success', container = 'alertContainer') {
    const icon = type === 'success'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
    const el = document.getElementById(container);
    el.innerHTML = `<div class="alert alert-${type}">${icon}<span>${msg}</span></div>`;
    setTimeout(() => { el.innerHTML = ''; }, 5000);
}

function getInitials(name, lastName) {
    return ((name || '').charAt(0) + (lastName || '').charAt(0)).toUpperCase();
}

function fmtTime(dt) {
    if (!dt) return '—';
    return new Date(dt).toLocaleTimeString('es-CO', { hour:'2-digit', minute:'2-digit', hour12:false });
}

function fmtDate(dt) {
    if (!dt) return '—';
    return new Date(dt).toLocaleDateString('es-CO', { day:'2-digit', month:'2-digit', year:'numeric' });
}

function fmtDateTimeLocal(dt) {
    if (!dt) return '';
    const d = new Date(dt);
    const pad = n => String(n).padStart(2,'0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * Convierte horas decimales a formato HH:MM
 * Ejemplo: 4.63 → "4:38"  |  8.0 → "8:00"  |  0.5 → "0:30"
 */
function decimalToHHMM(decimal) {
    if (!decimal && decimal !== 0) return '0:00';
    const totalMinutes = Math.round(Math.abs(decimal) * 60);
    const hours   = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return `${hours}:${String(minutes).padStart(2, '0')}`;
}

async function loadEmployees() {
    try {
        const res = await fetch(`${API_BASE}/users`);
        if (!res.ok) throw new Error();
        employees = await res.json();
        const options = ['<option value="">— Selecciona un empleado —</option>',
            ...employees.map(e => `<option value="${e.id}">${e.name} ${e.lastName} · CC ${e.cc}</option>`)
        ].join('');
        ['entryEmployeeId','exitEmployeeId'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerHTML = options;
        });
    } catch { }
}

async function loadAttendances(filter = 'today') {
    try {
        const res = await fetch(`${API_BASE}/attendances?filter=${filter}`);
        if (!res.ok) throw new Error();
        allAttendances = await res.json();
        applyFilter();
        updatePendingBanner();
        document.getElementById('stateLoading').classList.add('hidden');
        document.getElementById('tableSection').classList.remove('hidden');
        document.getElementById('paginationWrap').classList.remove('hidden');
    } catch {
        document.getElementById('stateLoading').textContent = 'Error al cargar asistencias.';
        showAlert('Error al cargar las asistencias.', 'error');
    }
}

function updatePendingBanner() {
    const pending = allAttendances.filter(a => !a.departureTime).length;
    const banner  = document.getElementById('pendingBanner');
    if (pending > 0) {
        document.getElementById('pendingCount').textContent =
            `${pending} empleado${pending > 1 ? 's' : ''} con salida pendiente de registro`;
        banner.classList.remove('hidden');
    } else {
        banner.classList.add('hidden');
    }
}

function applyFilter() {
    const q      = document.getElementById('searchInput').value.toLowerCase();
    const status = document.getElementById('statusFilter').value;

    filteredAttendances = allAttendances.filter(a => {
        const name = a.user ? `${a.user.name} ${a.user.lastName}`.toLowerCase() : '';
        const matchQ = name.includes(q);
        const matchS = status === ''         ? true
            : status === 'complete' ? !!a.departureTime
                :                         !a.departureTime;
        return matchQ && matchS;
    });
    currentPage = 1;
    renderTable();
    renderPagination();
}

async function changePeriodFilter() {
    const filter = document.getElementById('periodFilter').value;
    document.getElementById('stateLoading').classList.remove('hidden');
    document.getElementById('tableSection').classList.add('hidden');
    await loadAttendances(filter);
}

function renderTable() {
    const tbody = document.getElementById('attTableBody');
    const empty = document.getElementById('stateEmpty');
    const start = (currentPage - 1) * PER_PAGE;
    const slice = filteredAttendances.slice(start, start + PER_PAGE);

    if (!filteredAttendances.length) {
        empty.classList.remove('hidden');
        tbody.innerHTML = '';
        return;
    }
    empty.classList.add('hidden');

    tbody.innerHTML = slice.map((att, i) => {
        const user     = att.user || {};
        const initials = getInitials(user.name, user.lastName);
        const col      = AVATAR_COLORS[(start + i) % AVATAR_COLORS.length];
        const complete = !!att.departureTime;

        /* ── Formato HH:MM para todas las horas ── */
        const hours = att.workedHours && complete
            ? decimalToHHMM(att.workedHours)
            : '—';

        const extra = (att.extraHours || 0) > 0
            ? `<span class="hours-val hours-extra">${decimalToHHMM(att.extraHours)}</span>`
            : '<span class="hours-val">—</span>';

        const night = (att.nightHours || 0) > 0
            ? `<span class="hours-val hours-night">${decimalToHHMM(att.nightHours)}</span>`
            : '<span class="hours-val">—</span>';

        const userId   = user.id || '';
        const dateStr  = fmtDate(att.entryTime);
        const entryStr = fmtTime(att.entryTime);
        const exitStr  = fmtTime(att.departureTime);

        const hasLocation = (att.entryLatitude != null && att.entryLongitude != null) ||
                            (att.exitLatitude != null && att.exitLongitude != null);
        const locationBadge = hasLocation
            ? `<button type="button" class="loc-badge loc-clickable" onclick="openLocationModal(${att.id})" title="Ver direcciones de entrada y salida"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg></button>`
            : '<span class="loc-badge loc-none">—</span>';

        return `
        <tr>
          <td>
            <div class="emp-name-cell">
              <div class="emp-avatar" style="background:${col.bg};color:${col.color}">${initials}</div>
              <span class="emp-fullname">${user.name || ''} ${user.lastName || ''}</span>
            </div>
          </td>
          <td>
            <div class="date-cell">
              <span class="date-val">${dateStr}</span>
            </div>
          </td>
          <td>
            <div class="datetime-cell">
              <span class="time-val">${entryStr}</span>
            </div>
          </td>
          <td>
            <div class="datetime-cell">
              <span class="time-val">${exitStr}</span>
            </div>
          </td>
          <td><span class="hours-val">${hours}</span></td>
          <td>${extra}</td>
          <td>${night}</td>
          <td>${locationBadge}</td>
          <td><span class="badge ${complete ? 'badge-complete' : 'badge-pending'}">${complete ? 'Completo' : 'Pendiente'}</span></td>
          <td>
            <div class="action-btns">
              <button class="btn btn-secondary btn-sm" onclick="openEditModal(${att.id})">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4z"/></svg>
                Editar
              </button>
              ${!complete
            ? `<button class="btn btn-primary btn-sm" onclick="quickExit(${userId})">
                     <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                     Salida
                   </button>`
            : `<button class="btn btn-danger btn-sm" onclick="confirmDelete(${att.id})">
                     <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                   </button>`}
            </div>
          </td>
        </tr>`;
    }).join('');
}

function renderPagination() {
    const total    = filteredAttendances.length;
    const totalPgs = Math.max(1, Math.ceil(total / PER_PAGE));
    const start    = total ? (currentPage - 1) * PER_PAGE + 1 : 0;
    const end      = Math.min(currentPage * PER_PAGE, total);

    document.getElementById('paginInfo').textContent = `Mostrando ${start}–${end} de ${total} registros`;

    let pages = [];
    for (let i = 1; i <= totalPgs; i++) {
        if (i === 1 || i === totalPgs || (i >= currentPage - 1 && i <= currentPage + 1)) pages.push(i);
        else if (pages[pages.length - 1] !== '…') pages.push('…');
    }

    document.getElementById('paginControls').innerHTML = `
      <button class="page-btn" onclick="goPage(1)" ${currentPage===1?'disabled':''}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg>
      </button>
      <button class="page-btn" onclick="goPage(${currentPage-1})" ${currentPage===1?'disabled':''}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      ${pages.map(p => p === '…'
        ? `<span style="padding:0 4px;font-size:12px;color:var(--gray-400)">…</span>`
        : `<button class="page-btn ${p===currentPage?'active':''}" onclick="goPage(${p})">${p}</button>`
    ).join('')}
      <button class="page-btn" onclick="goPage(${currentPage+1})" ${currentPage===totalPgs?'disabled':''}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
      <button class="page-btn" onclick="goPage(${totalPgs})" ${currentPage===totalPgs?'disabled':''}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="13 17 18 12 13 7"/><polyline points="6 17 11 12 6 7"/></svg>
      </button>`;
}

function goPage(p) {
    const total = Math.max(1, Math.ceil(filteredAttendances.length / PER_PAGE));
    currentPage = Math.min(Math.max(1, p), total);
    renderTable();
    renderPagination();
}

/* ── Reverse geocoding with Nominatim (OpenStreetMap) ── */
async function getAddress(lat, lon) {
    try {
        const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=18&addressdetails=1`, {
            headers: { 'Accept-Language': 'es' }
        });
        if (!res.ok) return null;
        const data = await res.json();
        return data.display_name || null;
    } catch {
        return null;
    }
}

/* ── Map ── */
function toggleMap() {
    const container = document.getElementById('mapContainer');
    const btn = document.getElementById('mapToggleBtn');
    mapVisible = !mapVisible;
    container.classList.toggle('hidden', !mapVisible);
    btn.innerHTML = mapVisible
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="1 6 1 22 8 18 16 22 21 18 21 2 16 6 8 2 1 6"/><line x1="8" y1="2" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="22"/></svg> Ocultar mapa'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="1 6 1 22 8 18 16 22 21 18 21 2 16 6 8 2 1 6"/><line x1="8" y1="2" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="22"/></svg> Ver mapa';
    if (mapVisible) {
        setTimeout(() => {
            if (mapInstance) mapInstance.invalidateSize();
            renderMap();
        }, 150);
    }
}

async function renderMap() {
    const mapEl = document.getElementById('attendanceMap');
    if (!mapEl) return;

    if (!mapInstance) {
        mapInstance = L.map(mapEl).setView([10.9838, -74.8890], 13); // Barranquilla default
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(mapInstance);
    }

    // Limpiar marcadores y popups anteriores
    mapInstance.eachLayer(layer => {
        if (layer instanceof L.Marker || layer instanceof L.CircleMarker || layer instanceof L.Popup) {
            mapInstance.removeLayer(layer);
        }
    });
    // Limpiar leyenda anterior
    if (mapInstance._legendControl) {
        mapInstance.removeControl(mapInstance._legendControl);
        mapInstance._legendControl = null;
    }

    mapInstance.invalidateSize();

    const markers = [];
    let hasLocation = false;

    for (const att of filteredAttendances) {
        const user = att.user || {};
        const name = `${user.name || ''} ${user.lastName || ''}`.trim();

        if (att.entryLatitude != null && att.entryLongitude != null) {
            const addr = await getAddress(att.entryLatitude, att.entryLongitude);
            const popupText = `<b>${name}</b><br><span style="color:#0D9488">● Entrada</span>: ${fmtTime(att.entryTime)}<br><small>${addr || `Lat: ${att.entryLatitude.toFixed(5)}, Lng: ${att.entryLongitude.toFixed(5)}`}</small>`;
            const m = L.marker([att.entryLatitude, att.entryLongitude], {
                icon: L.divIcon({
                    className: 'custom-pin',
                    html: '<div style="background:#0D9488;width:14px;height:14px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                })
            }).bindPopup(popupText).addTo(mapInstance);
            markers.push([att.entryLatitude, att.entryLongitude]);
            hasLocation = true;
        }

        if (att.exitLatitude != null && att.exitLongitude != null) {
            const addr = await getAddress(att.exitLatitude, att.exitLongitude);
            const popupText = `<b>${name}</b><br><span style="color:#E11D48">● Salida</span>: ${fmtTime(att.departureTime)}<br><small>${addr || `Lat: ${att.exitLatitude.toFixed(5)}, Lng: ${att.exitLongitude.toFixed(5)}`}</small>`;
            const m = L.marker([att.exitLatitude, att.exitLongitude], {
                icon: L.divIcon({
                    className: 'custom-pin',
                    html: '<div style="background:#E11D48;width:16px;height:16px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>',
                    iconSize: [16, 16],
                    iconAnchor: [8, 8]
                })
            }).bindPopup(popupText).addTo(mapInstance);
            markers.push([att.exitLatitude, att.exitLongitude]);
            hasLocation = true;
        }
    }

    if (hasLocation) {
        const bounds = L.latLngBounds(markers);
        mapInstance.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
    } else {
        mapInstance.setView([10.9838, -74.8890], 13);
        // Mostrar mensaje informativo sobre el mapa
        const info = L.control({ position: 'topright' });
        info.onAdd = function () {
            const div = L.DomUtil.create('div', 'map-info');
            div.innerHTML = `<b>No hay ubicaciones registradas</b><br><small>Los marcadores aparecen solo cuando el empleado marca asistencia desde <b>/register</b> con geolocalización activada.</small>`;
            return div;
        };
        info.addTo(mapInstance);
        mapInstance._legendControl = info;
    }

    // Legend
    const legend = L.control({ position: 'bottomright' });
    legend.onAdd = function () {
        const div = L.DomUtil.create('div', 'map-legend');
        div.innerHTML = `
            <div class="legend-item"><span class="legend-dot" style="background:#0D9488"></span> Entrada</div>
            <div class="legend-item"><span class="legend-dot" style="background:#E11D48"></span> Salida</div>
        `;
        return div;
    };
    legend.addTo(mapInstance);
    if (!mapInstance._legendControl) mapInstance._legendControl = legend;
}

/* ── Location modal ── */
function openLocationModal(attId) {
    const att = allAttendances.find(a => a.id === attId);
    if (!att) return;

    // Ocultar mapa si está visible para evitar que se sobreponga
    if (mapVisible) {
        toggleMap();
    }

    document.getElementById('locationAlert').innerHTML = '';
    document.getElementById('locEntryBlock').style.display = 'none';
    document.getElementById('locExitBlock').style.display = 'none';
    document.getElementById('locEntryAddress').textContent = 'Obteniendo dirección…';
    document.getElementById('locExitAddress').textContent = 'Obteniendo dirección…';
    document.getElementById('modalLocation').classList.add('active');

    const promises = [];

    if (att.entryLatitude != null && att.entryLongitude != null) {
        document.getElementById('locEntryBlock').style.display = '';
        document.getElementById('locEntryTime').textContent = fmtTime(att.entryTime);
        promises.push(
            getAddress(att.entryLatitude, att.entryLongitude).then(addr => {
                document.getElementById('locEntryAddress').textContent = addr || 'Dirección no disponible';
            })
        );
    }

    if (att.exitLatitude != null && att.exitLongitude != null) {
        document.getElementById('locExitBlock').style.display = '';
        document.getElementById('locExitTime').textContent = fmtTime(att.departureTime);
        promises.push(
            getAddress(att.exitLatitude, att.exitLongitude).then(addr => {
                document.getElementById('locExitAddress').textContent = addr || 'Dirección no disponible';
            })
        );
    }

    Promise.all(promises).catch(() => {
        document.getElementById('locationAlert').innerHTML = '<div class="alert alert-error">Error al obtener direcciones.</div>';
    });
}

function closeLocationModal() {
    document.getElementById('modalLocation').classList.remove('active');
}

function openEntryModal() {
    document.getElementById('entryForm').reset();
    document.getElementById('entryAlert').innerHTML = '';
    document.getElementById('modalEntry').classList.add('active');
}
function closeEntryModal() { document.getElementById('modalEntry').classList.remove('active'); }

async function registerEntry(e) {
    e.preventDefault();
    const id = document.getElementById('entryEmployeeId').value;
    if (!id) { showAlert('Selecciona un empleado', 'error', 'entryAlert'); return; }
    try {
        const res = await fetch(`${API_BASE}/users/${id}/entry`, { method:'POST', headers:{'Content-Type':'application/json'} });
        if (!res.ok) { const d = await res.json(); throw new Error(d.error || 'Error'); }
        closeEntryModal();
        showAlert('Entrada registrada exitosamente');
        await loadAttendances(document.getElementById('periodFilter').value);
    } catch (err) {
        showAlert(err.message, 'error', 'entryAlert');
    }
}

function openExitModal() {
    document.getElementById('exitForm').reset();
    document.getElementById('exitAlert').innerHTML = '';
    document.getElementById('modalExit').classList.add('active');
}
function closeExitModal() { document.getElementById('modalExit').classList.remove('active'); }

async function registerExit(e) {
    e.preventDefault();
    const id = document.getElementById('exitEmployeeId').value;
    if (!id) { showAlert('Selecciona un empleado', 'error', 'exitAlert'); return; }
    try {
        const res = await fetch(`${API_BASE}/users/${id}/exit`, { method:'POST', headers:{'Content-Type':'application/json'} });
        if (!res.ok) { const d = await res.json(); throw new Error(d.error || 'Error'); }
        closeExitModal();
        showAlert('Salida registrada exitosamente');
        await loadAttendances(document.getElementById('periodFilter').value);
    } catch (err) {
        showAlert(err.message, 'error', 'exitAlert');
    }
}

async function quickExit(userId) {
    if (!confirm('¿Registrar salida de este empleado ahora?')) return;
    try {
        const res = await fetch(`${API_BASE}/users/${userId}/exit`, { method:'POST', headers:{'Content-Type':'application/json'} });
        if (!res.ok) { const d = await res.json(); throw new Error(d.error || 'Error'); }
        showAlert('Salida registrada exitosamente');
        await loadAttendances(document.getElementById('periodFilter').value);
    } catch (err) {
        showAlert(err.message, 'error');
    }
}

async function openEditModal(attId) {
    try {
        const res = await fetch(`${API_BASE}/attendances/${attId}`);
        if (!res.ok) throw new Error();
        const att = await res.json();
        document.getElementById('editAttId').value        = attId;
        document.getElementById('editEmpName').value      = `${att.user.name} ${att.user.lastName}`;
        document.getElementById('editEntryTime').value    = fmtDateTimeLocal(att.entryTime);
        document.getElementById('editDepartureTime').value= fmtDateTimeLocal(att.departureTime);
        document.getElementById('editAlert').innerHTML    = '';
        document.getElementById('modalEdit').classList.add('active');
    } catch {
        showAlert('Error al cargar la asistencia.', 'error');
    }
}
function closeEditModal() { document.getElementById('modalEdit').classList.remove('active'); }

async function updateAttendance(e) {
    e.preventDefault();
    const id = document.getElementById('editAttId').value;
    const body = {
        entryTime:     document.getElementById('editEntryTime').value    || null,
        departureTime: document.getElementById('editDepartureTime').value || null
    };
    try {
        const res = await fetch(`${API_BASE}/attendances/${id}`, {
            method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)
        });
        if (!res.ok) { const d = await res.json(); throw new Error(d.error || 'Error'); }
        closeEditModal();
        showAlert('Asistencia actualizada exitosamente');
        await loadAttendances(document.getElementById('periodFilter').value);
    } catch (err) {
        showAlert(err.message, 'error', 'editAlert');
    }
}

let pendingDeleteId = null;
function confirmDelete(id) {
    pendingDeleteId = id;
    document.getElementById('modalDelete').classList.add('active');
}
function closeDeleteModal() { document.getElementById('modalDelete').classList.remove('active'); pendingDeleteId = null; }

async function deleteAttendance() {
    if (!pendingDeleteId) return;
    try {
        const res = await fetch(`${API_BASE}/attendances/${pendingDeleteId}`, { method:'DELETE' });
        if (!res.ok) throw new Error();
        closeDeleteModal();
        showAlert('Asistencia eliminada exitosamente');
        await loadAttendances(document.getElementById('periodFilter').value);
    } catch {
        showAlert('Error al eliminar la asistencia.', 'error');
        closeDeleteModal();
    }
}

window.addEventListener('click', e => {
    ['modalEntry','modalExit','modalEdit','modalDelete'].forEach(id => {
        if (e.target === document.getElementById(id))
            document.getElementById(id).classList.remove('active');
    });
});

window.onload = () => { initClock(); loadEmployees(); loadAttendances('today'); };