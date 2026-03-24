const API_BASE = '/api';

let allAttendances      = [];
let filteredAttendances = [];
let employees           = [];
const PER_PAGE          = 10;
let currentPage         = 1;

const AVATAR_COLORS = [
    { bg:'rgba(30,58,138,.1)',  color:'#1E3A8A' },
    { bg:'rgba(13,148,136,.1)', color:'#0D9488' },
    { bg:'rgba(217,119,6,.1)',  color:'#B45309' },
    { bg:'rgba(225,29,72,.1)',  color:'#E11D48' },
    { bg:'rgba(124,58,237,.1)',color:'#7C3AED' },
    { bg:'rgba(5,150,105,.1)',  color:'#059669' },
];

/* ── Sidebar / clock ── */
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

/* ── Alerts ── */
function showAlert(msg, type = 'success', container = 'alertContainer') {
    const icon = type === 'success'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
    const el = document.getElementById(container);
    el.innerHTML = `<div class="alert alert-${type}">${icon}<span>${msg}</span></div>`;
    setTimeout(() => { el.innerHTML = ''; }, 5000);
}

/* ── Helpers ── */
function getInitials(name, lastName) {
    return ((name || '').charAt(0) + (lastName || '').charAt(0)).toUpperCase();
}
function fmtTime(dt) {
    if (!dt) return '—';
    return new Date(dt).toLocaleTimeString('es-CO', { hour:'2-digit', minute:'2-digit', hour12:false });
}
function fmtDateTimeLocal(dt) {
    if (!dt) return '';
    const d = new Date(dt);
    const pad = n => String(n).padStart(2,'0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/* ── Load employees for selects ── */
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
    } catch { /* silently fail */ }
}

/* ── Load attendances ── */
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

/* ── Render ── */
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
        const hours    = att.workedHours && complete ? att.workedHours.toFixed(2) + 'h' : '—';
        const extra    = (att.extraHours  || 0) > 0 ? `<span class="hours-val hours-extra">${att.extraHours.toFixed(2)}h</span>` : '<span class="hours-val">—</span>';
        const night    = (att.nightHours  || 0) > 0 ? `<span class="hours-val hours-night">${att.nightHours.toFixed(2)}h</span>`  : '<span class="hours-val">—</span>';
        const userId   = user.id || '';

        return `
        <tr>
          <td>
            <div class="emp-name-cell">
              <div class="emp-avatar" style="background:${col.bg};color:${col.color}">${initials}</div>
              <span class="emp-fullname">${user.name || ''} ${user.lastName || ''}</span>
            </div>
          </td>
          <td><span class="time-val">${fmtTime(att.entryTime)}</span></td>
          <td><span class="time-val">${fmtTime(att.departureTime)}</span></td>
          <td><span class="hours-val">${hours}</span></td>
          <td>${extra}</td>
          <td>${night}</td>
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

/* ── Entry modal ── */
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

/* ── Exit modal ── */
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

/* ── Edit modal ── */
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

/* ── Delete ── */
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

/* ── Backdrop click ── */
window.addEventListener('click', e => {
    ['modalEntry','modalExit','modalEdit','modalDelete'].forEach(id => {
        if (e.target === document.getElementById(id))
            document.getElementById(id).classList.remove('active');
    });
});

window.onload = () => { initClock(); loadEmployees(); loadAttendances('today'); };