const API_BASE = '/api/config';
let originalConfig = {};

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
    window.scrollTo({ top: 0, behavior: 'smooth' });
    setTimeout(() => { el.innerHTML = ''; }, 6000);
}

/* ── Load config ── */
async function loadConfiguration() {
    try {
        const res = await fetch(API_BASE);
        if (!res.ok) throw new Error();
        const configs = await res.json();

        configs.forEach(c => { originalConfig[c.key] = c.value; });

        setField('regularHourRate',  'REGULAR_HOUR_RATE',  '7959');
        setField('dayOvertimeRate',  'DAY_OVERTIME_RATE',  '9948');
        setField('nightSurchargeRate','NIGHT_SURCHARGE_RATE','2786');
        setField('nightOvertimeRate','NIGHT_OVERTIME_RATE','13928.25');
        setField('nightStartHour',   'NIGHT_START_HOUR',   '19');
        setField('nightEndHour',     'NIGHT_END_HOUR',     '6');

        const tz = document.getElementById('timeZone');
        if (tz) tz.value = originalConfig['TIME_ZONE'] || 'America/Bogota';

        document.getElementById('stateLoading').classList.add('hidden');
        document.getElementById('configContent').classList.remove('hidden');
    } catch {
        document.getElementById('stateLoading').innerHTML =
            '<div class="spinner"></div><span>Error al cargar. Recarga la página.</span>';
    }
}

function setField(id, key, fallback) {
    const el = document.getElementById(id);
    if (el) el.value = originalConfig[key] || fallback;
}

/* ── Reset ── */
function resetForm() {
    if (!confirm('¿Restablecer todos los campos a los valores guardados actualmente?')) return;
    setField('regularHourRate',  'REGULAR_HOUR_RATE',  '7959');
    setField('dayOvertimeRate',  'DAY_OVERTIME_RATE',  '9948');
    setField('nightSurchargeRate','NIGHT_SURCHARGE_RATE','2786');
    setField('nightOvertimeRate','NIGHT_OVERTIME_RATE','13928.25');
    setField('nightStartHour',   'NIGHT_START_HOUR',   '19');
    setField('nightEndHour',     'NIGHT_END_HOUR',     '6');
    const tz = document.getElementById('timeZone');
    if (tz) tz.value = originalConfig['TIME_ZONE'] || 'America/Bogota';
    showAlert('Campos restablecidos a los valores guardados.');
}

/* ── Save ── */
async function saveConfiguration(e) {
    e.preventDefault();

    const saveBtn = document.getElementById('saveBtn');
    saveBtn.disabled = true;
    saveBtn.innerHTML = `<div class="spinner" style="width:14px;height:14px;border-width:2px"></div> Guardando…`;

    const updates = [
        { key: 'REGULAR_HOUR_RATE',   value: document.getElementById('regularHourRate').value },
        { key: 'DAY_OVERTIME_RATE',   value: document.getElementById('dayOvertimeRate').value },
        { key: 'NIGHT_SURCHARGE_RATE',value: document.getElementById('nightSurchargeRate').value },
        { key: 'NIGHT_OVERTIME_RATE', value: document.getElementById('nightOvertimeRate').value },
        { key: 'NIGHT_START_HOUR',    value: document.getElementById('nightStartHour').value },
        { key: 'NIGHT_END_HOUR',      value: document.getElementById('nightEndHour').value },
        { key: 'TIME_ZONE',           value: document.getElementById('timeZone').value },
    ];

    try {
        const results = await Promise.all(
            updates.map(u =>
                fetch(`${API_BASE}/${u.key}?value=${encodeURIComponent(u.value)}`, {
                    method: 'PUT', headers: { 'Content-Type': 'application/json' }
                })
            )
        );

        if (!results.every(r => r.ok)) throw new Error();

        updates.forEach(u => { originalConfig[u.key] = u.value; });
        showAlert('Configuración guardada. Reinicia la aplicación para aplicar todos los cambios.', 'warning');
    } catch {
        showAlert('Error al guardar la configuración. Intenta nuevamente.', 'error');
    } finally {
        saveBtn.disabled = false;
        saveBtn.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                <polyline points="17 21 17 13 7 13 7 21"/>
                <polyline points="7 3 7 8 15 8"/>
            </svg>
            Guardar cambios`;
    }
}

/* ── Hour validation ── */
['nightStartHour','nightEndHour'].forEach(id => {
    document.addEventListener('DOMContentLoaded', () => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener('input', function () {
            const v = parseInt(this.value);
            this.setCustomValidity((v < 0 || v > 23) ? 'La hora debe estar entre 0 y 23' : '');
        });
    });
});

window.onload = () => { initClock(); loadConfiguration(); };