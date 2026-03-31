const API_BASE = '/api/reports';

let allReports      = [];
let filteredReports = [];
const PER_PAGE      = 12;
let currentPage     = 1;

const MONTH_NAMES = [
    'Enero','Febrero','Marzo','Abril','Mayo','Junio',
    'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'
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

function showAlert(msg, type = 'success') {
    const icons = {
        success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>',
        error:   '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
        warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    };
    const el = document.getElementById('alertContainer');
    el.innerHTML = `<div class="alert alert-${type}">${icons[type]}<span>${msg}</span></div>`;
    setTimeout(() => { el.innerHTML = ''; }, 6000);
}

function fmtCurrency(v) {
    return new Intl.NumberFormat('es-CO', { style:'currency', currency:'COP', minimumFractionDigits:0, maximumFractionDigits:0 }).format(v || 0);
}

function fmtDateTime(dt) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('es-CO', { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit', hour12:false });
}

async function loadReports() {
    try {
        const res = await fetch(API_BASE);
        if (!res.ok) throw new Error();
        allReports = await res.json();
        updateStats();
        applyFilter();
        document.getElementById('stateLoading').classList.add('hidden');
        document.getElementById('tableSection').classList.remove('hidden');
    } catch {
        document.getElementById('stateLoading').innerHTML = '<svg style="width:44px;height:44px;color:var(--gray-300);margin:0 auto 14px;display:block" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg><span>Error al cargar los reportes</span>';
        showAlert('Error al cargar los reportes. Recarga la página.', 'error');
    }
}

function updateStats() {
    const total = allReports.length;
    const totalPayroll = allReports.reduce((s, r) => s + (r.totalPayroll || 0), 0);
    const auto = allReports.filter(r => r.generationType === 'AUTOMATIC').length;

    document.getElementById('statTotal').textContent = total;
    document.getElementById('statAuto').textContent = auto;
    document.getElementById('statPayroll').textContent = fmtCurrency(totalPayroll);
}

function applyFilter() {
    const q    = document.getElementById('searchInput').value.toLowerCase();
    const year = document.getElementById('yearFilter').value;
    const type = document.getElementById('typeFilter').value;

    filteredReports = allReports.filter(r => {
        const text = `${r.monthName || ''} ${r.month} ${r.year}`.toLowerCase();
        const matchQ    = !q    || text.includes(q);
        const matchYear = !year || String(r.year) === year;
        const matchType = !type || r.generationType === type;
        return matchQ && matchYear && matchType;
    });

    currentPage = 1;
    renderTable();
    renderPagination();
}

function renderTable() {
    const tbody = document.getElementById('repTableBody');
    const empty = document.getElementById('stateEmpty');
    const start = (currentPage - 1) * PER_PAGE;
    const slice = filteredReports.slice(start, start + PER_PAGE);

    if (!filteredReports.length) {
        empty.classList.remove('hidden');
        tbody.innerHTML = '';
        return;
    }
    empty.classList.add('hidden');

    tbody.innerHTML = slice.map(r => {
        const isAuto  = r.generationType === 'AUTOMATIC';
        const typeLabel = isAuto ? 'Automático' : 'Manual';
        const typeCls   = isAuto ? 'type-auto' : 'type-manual';
        const monthName = r.monthName || MONTH_NAMES[(r.month - 1)] || '—';

        return `
        <tr>
          <td>
            <div class="month-badge">
              <div class="month-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
              </div>
              <div class="month-text">
                <span class="month-name">${monthName}</span>
                <span class="month-year">${r.year}</span>
              </div>
            </div>
          </td>
          <td class="pay-cell">${fmtCurrency(r.totalPayroll)}</td>
          <td style="font-size:12px;color:var(--gray-600)">${r.totalEmployees || 0} empleados</td>
          <td><span class="type-badge ${typeCls}">${typeLabel}</span></td>
          <td class="date-cell">${fmtDateTime(r.generatedAt)}</td>
          <td>
            <div class="action-btns">
              <button class="btn btn-teal btn-sm" onclick="downloadReport(${r.id}, '${r.fileName || ''}')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                Excel
              </button>
              <button class="btn-icon-only danger" onclick="confirmDelete(${r.id}, '${monthName} ${r.year}')" title="Eliminar reporte">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
          </td>
        </tr>`;
    }).join('');
}

function renderPagination() {
    const total    = filteredReports.length;
    const totalPgs = Math.max(1, Math.ceil(total / PER_PAGE));
    const start    = total ? (currentPage - 1) * PER_PAGE + 1 : 0;
    const end      = Math.min(currentPage * PER_PAGE, total);

    document.getElementById('paginInfo').textContent = `Mostrando ${start}–${end} de ${total} reportes`;

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
    const total = Math.max(1, Math.ceil(filteredReports.length / PER_PAGE));
    currentPage = Math.min(Math.max(1, p), total);
    renderTable();
    renderPagination();
}

function buildYearOptions() {
    const years = [...new Set(allReports.map(r => r.year))].sort((a, b) => b - a);
    const sel = document.getElementById('yearFilter');
    const current = sel.value;
    sel.innerHTML = '<option value="">Todos los años</option>' +
        years.map(y => `<option value="${y}" ${String(y) === current ? 'selected' : ''}>${y}</option>`).join('');
}

async function downloadReport(id, fileName) {
    try {
        const res = await fetch(`${API_BASE}/${id}/download`);
        if (!res.ok) throw new Error();
        const blob = await res.blob();
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href = url;
        a.download = fileName || `reporte_${id}.xlsx`;
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showAlert('Archivo descargado exitosamente');
    } catch {
        showAlert('Error al descargar el reporte.', 'error');
    }
}

function openCreateModal() {
    const now = new Date();
    document.getElementById('createMonth').value = now.getMonth() + 1;
    document.getElementById('createYear').value  = now.getFullYear();
    document.getElementById('createAlert').innerHTML = '';
    document.getElementById('modalCreate').classList.add('active');
}
function closeCreateModal() { document.getElementById('modalCreate').classList.remove('active'); }

async function createReport(e) {
    e.preventDefault();
    const month = parseInt(document.getElementById('createMonth').value);
    const year  = parseInt(document.getElementById('createYear').value);

    closeCreateModal();

    const overlay = document.getElementById('generatingOverlay');
    overlay.classList.add('active');

    try {
        const res = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ month, year })
        });

        if (!res.ok) {
            const d = await res.json().catch(() => ({}));
            throw new Error(d.error || 'Error al generar reporte');
        }

        const report = await res.json();
        overlay.classList.remove('active');
        showAlert(`Reporte de ${MONTH_NAMES[month - 1]} ${year} generado exitosamente`);
        await loadReports();
        buildYearOptions();
    } catch (err) {
        overlay.classList.remove('active');
        showAlert(err.message || 'Error al generar el reporte.', 'error');
    }
}

let pendingDeleteId   = null;
let pendingDeleteName = '';

function confirmDelete(id, name) {
    pendingDeleteId   = id;
    pendingDeleteName = name;
    document.getElementById('deleteReportName').textContent = name;
    document.getElementById('modalDelete').classList.add('active');
}
function closeDeleteModal() {
    document.getElementById('modalDelete').classList.remove('active');
    pendingDeleteId = null;
}

async function deleteReport() {
    if (!pendingDeleteId) return;
    try {
        const res = await fetch(`${API_BASE}/${pendingDeleteId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error();
        closeDeleteModal();
        showAlert(`Reporte "${pendingDeleteName}" eliminado`);
        await loadReports();
        buildYearOptions();
    } catch {
        showAlert('Error al eliminar el reporte.', 'error');
        closeDeleteModal();
    }
}

window.addEventListener('click', e => {
    ['modalCreate', 'modalDelete'].forEach(id => {
        if (e.target === document.getElementById(id))
            document.getElementById(id).classList.remove('active');
    });
});

window.onload = () => {
    initClock();
    loadReports();

    const now = new Date();
    document.getElementById('createMonth').value = now.getMonth() + 1;
    document.getElementById('createYear').value  = now.getFullYear();
};
