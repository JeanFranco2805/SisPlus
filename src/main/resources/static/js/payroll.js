const API_BASE = '/api';
let employees  = [];

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

/* ── Helpers ── */
function fmtCurrency(v) {
    return new Intl.NumberFormat('es-CO', { minimumFractionDigits:2, maximumFractionDigits:2 }).format(v || 0);
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

function getMonthName(m) {
    return ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
        'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'][m - 1];
}

function getInitials(name, lastName) {
    return ((name || '').charAt(0) + (lastName || '').charAt(0)).toUpperCase();
}

/* ── Period toggle ── */
function togglePeriod() {
    const period   = document.getElementById('periodSelect').value;
    const dateGrp  = document.getElementById('dateGroup');
    const monthGrp = document.getElementById('monthGroup');
    const yearGrp  = document.getElementById('yearGroup');

    if (period === 'monthly') {
        dateGrp.classList.add('hidden');
        monthGrp.classList.remove('hidden');
        yearGrp.classList.remove('hidden');
    } else {
        dateGrp.classList.remove('hidden');
        monthGrp.classList.add('hidden');
        yearGrp.classList.add('hidden');
    }
}

/* ── Load employees ── */
async function loadEmployees() {
    try {
        const res = await fetch(`${API_BASE}/users`);
        if (!res.ok) throw new Error();
        employees = await res.json();
        const sel = document.getElementById('employeeSelect');
        sel.innerHTML = '<option value="">— Selecciona un empleado —</option>' +
            employees.map(e => `<option value="${e.id}">${e.name} ${e.lastName} · CC ${e.cc}</option>`).join('');
    } catch {
        showAlert('Error al cargar la lista de empleados.', 'error');
    }
}

/* ── Calculate ── */
async function calculatePayroll() {
    const empId  = document.getElementById('employeeSelect').value;
    const period = document.getElementById('periodSelect').value;

    if (!empId) { showAlert('Selecciona un empleado para continuar.', 'warning'); return; }

    let url  = `${API_BASE}/users/${empId}/payroll?period=${period}`;
    let attUrl = `${API_BASE}/attendances?userId=${empId}`;

    if (period === 'monthly') {
        const month = document.getElementById('monthInput').value;
        const year  = document.getElementById('yearInput').value;
        url    += `&month=${month}&year=${year}`;
        const first = new Date(year, month - 1, 1).toISOString().split('T')[0];
        const last  = new Date(year, month, 0).toISOString().split('T')[0];
        attUrl += `&startDate=${first}&endDate=${last}`;
    } else if (period === 'weekly') {
        const date  = document.getElementById('dateInput').value || new Date().toISOString().split('T')[0];
        url    += `&date=${date}`;
        const end   = new Date(date);
        const start = new Date(end); start.setDate(start.getDate() - 6);
        attUrl += `&startDate=${start.toISOString().split('T')[0]}&endDate=${end.toISOString().split('T')[0]}`;
    } else {
        const date = document.getElementById('dateInput').value || new Date().toISOString().split('T')[0];
        url    += `&date=${date}`;
        attUrl += `&date=${date}`;
    }

    document.getElementById('resultsEmpty').classList.add('hidden');
    document.getElementById('resultsContent').classList.add('hidden');
    document.getElementById('resultsLoading').classList.remove('hidden');
    document.getElementById('resultsCard').classList.remove('hidden');

    try {
        const [payRes, attRes] = await Promise.all([fetch(url), fetch(attUrl)]);
        if (!payRes.ok) throw new Error('Error al calcular nómina');
        const data = await payRes.json();
        const atts = attRes.ok ? await attRes.json() : [];
        renderResults(data, atts, period);
    } catch (err) {
        showAlert(err.message, 'error');
        document.getElementById('resultsLoading').classList.add('hidden');
        document.getElementById('resultsEmpty').classList.remove('hidden');
    }
}

function renderResults(data, atts, period) {
    const emp      = employees.find(e => e.id === data.id);
    const initials = emp ? getInitials(emp.name, emp.lastName) : '??';
    const attCount = Array.isArray(atts) ? atts.length : 0;

    document.getElementById('empAvatarText').textContent = initials;
    document.getElementById('empStripName').textContent  = `${data.name} ${data.lastName}`;
    document.getElementById('empStripCc').textContent    = `CC: ${data.cc}`;

    let periodLabel = '';
    let infoMsg     = '';
    let alertType   = 'success';

    if (period === 'daily') {
        periodLabel = 'Período diario';
        infoMsg     = attCount === 0 ? 'Sin asistencias para este día'
            : attCount === 1 ? '1 asistencia encontrada'
                : `${attCount} asistencias encontradas`;
        if (attCount === 0) alertType = 'warning';
    } else if (period === 'weekly') {
        periodLabel = 'Período semanal · últimos 7 días';
        infoMsg     = attCount === 0 ? 'Sin asistencias en la semana'
            : `${attCount} día${attCount > 1 ? 's' : ''} trabajado${attCount > 1 ? 's' : ''} en la semana`;
        if (attCount === 0) alertType = 'warning';
    } else {
        const month = document.getElementById('monthInput').value;
        const year  = document.getElementById('yearInput').value;
        periodLabel = `${getMonthName(month)} ${year}`;
        infoMsg     = attCount === 0 ? 'Sin asistencias en el mes'
            : `${attCount} día${attCount > 1 ? 's' : ''} trabajado${attCount > 1 ? 's' : ''} en el mes`;
        if (attCount === 0) alertType = 'warning';
    }

    document.getElementById('periodLabel').textContent = periodLabel;
    document.getElementById('periodInfo').textContent  = infoMsg;

    /* ── Horas en formato HH:MM ── */
    document.getElementById('rRegularHours').textContent   = decimalToHHMM(data.regularHours);
    document.getElementById('rDayExtra').textContent       = decimalToHHMM(data.dayOvertimeHours);
    document.getElementById('rNightExtra').textContent     = decimalToHHMM(data.nightOvertimeHours);
    document.getElementById('rNightHours').textContent     = decimalToHHMM(data.nightHours);

    /* ── Pagos en moneda ── */
    document.getElementById('rRegularPay').textContent     = fmtCurrency(data.regularPay);
    document.getElementById('rDayExtraPay').textContent    = fmtCurrency(data.dayOvertimePay);
    document.getElementById('rNightExtraPay').textContent  = fmtCurrency(data.nightOvertimePay);
    document.getElementById('rNightSurcharge').textContent = fmtCurrency(data.nightSurchargePay);
    document.getElementById('rTotalExtra').textContent     = fmtCurrency(data.totalOvertimePay);
    document.getElementById('rTotalPay').textContent       = fmtCurrency(data.totalPay);
    document.getElementById('rPeriodBadge').textContent    = periodLabel;

    document.getElementById('resultsLoading').classList.add('hidden');
    document.getElementById('resultsContent').classList.remove('hidden');

    showAlert(
        data.totalPay === 0 ? `${infoMsg} — total a pagar: $0` : `Nómina calculada · ${infoMsg}`,
        alertType
    );
}

/* ── Excel download ── */
async function downloadExcel() {
    const month = document.getElementById('monthInput').value;
    const year  = document.getElementById('yearInput').value;
    showAlert('Generando archivo Excel…', 'warning');
    try {
        const res = await fetch(`${API_BASE}/payroll/export/excel?month=${month}&year=${year}`);
        if (!res.ok) throw new Error();
        const blob = await res.blob();
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href = url; a.download = `Nomina_${getMonthName(month)}_${year}.xlsx`;
        document.body.appendChild(a); a.click();
        URL.revokeObjectURL(url); document.body.removeChild(a);
        showAlert('Archivo Excel descargado exitosamente');
    } catch {
        showAlert('Error al generar el archivo Excel.', 'error');
    }
}

window.onload = () => {
    initClock();
    loadEmployees();
    const today = new Date();
    document.getElementById('dateInput').value   = today.toISOString().split('T')[0];
    document.getElementById('monthInput').value  = today.getMonth() + 1;
    document.getElementById('yearInput').value   = today.getFullYear();
};