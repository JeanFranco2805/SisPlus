const API_BASE = '/api/users';

let busy = false;

/* ── Clock ── */
function updateClock() {
    const el = document.getElementById('liveClock');
    if (!el) return;
    const now  = new Date();
    const time = now.toLocaleTimeString('es-CO', { hour:'2-digit', minute:'2-digit', second:'2-digit', hour12:true });
    const date = now.toLocaleDateString('es-CO', { weekday:'long', day:'numeric', month:'long' });
    el.textContent = time;
    const dateEl = document.getElementById('liveDate');
    if (dateEl) dateEl.textContent = date.charAt(0).toUpperCase() + date.slice(1);
}
setInterval(updateClock, 1000);
updateClock();

/* ── Feedback ── */
function showFeedback(msg, type = 'success') {
    const fb  = document.getElementById('feedback');
    const ico = document.getElementById('feedbackIcon');
    const txt = document.getElementById('feedbackText');

    const icons = {
        success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>',
        error:   '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
    };

    fb.className  = `feedback feedback-${type} show`;
    ico.innerHTML = icons[type];
    txt.textContent = msg;

    clearTimeout(fb._timer);
    fb._timer = setTimeout(() => { fb.classList.remove('show'); }, 4000);
}

/* ── Get & validate CC ── */
function getCC() {
    const cc = document.getElementById('ccInput').value.trim();
    if (!cc) {
        showFeedback('Por favor ingresa el número de cédula.', 'error');
        document.getElementById('ccInput').focus();
        return null;
    }
    return cc;
}

/* ── Set button loading state ── */
function setLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    btn.disabled = loading;
    btn.classList.toggle('loading', loading);
}

/* ── Register entry ── */
async function registerEntry() {
    if (busy) return;
    const cc = getCC();
    if (!cc) return;

    busy = true;
    setLoading('btnEntry', true);
    setLoading('btnExit', true);

    try {
        const res = await fetch(`${API_BASE}/${encodeURIComponent(cc)}/entryCC`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        if (res.ok) {
            showFeedback(`Entrada registrada correctamente para CC: ${cc}`, 'success');
            document.getElementById('ccInput').value = '';
        } else {
            const text = await res.text().catch(() => '');
            showFeedback(text || 'No se pudo registrar la entrada. Verifica la cédula.', 'error');
        }
    } catch {
        showFeedback('Error de conexión. Intenta nuevamente.', 'error');
    } finally {
        busy = false;
        setLoading('btnEntry', false);
        setLoading('btnExit', false);
        document.getElementById('ccInput').focus();
    }
}

/* ── Register exit ── */
async function registerExit() {
    if (busy) return;
    const cc = getCC();
    if (!cc) return;

    busy = true;
    setLoading('btnEntry', true);
    setLoading('btnExit', true);

    try {
        const res = await fetch(`${API_BASE}/${encodeURIComponent(cc)}/exitCC`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        if (res.ok) {
            showFeedback(`Salida registrada correctamente para CC: ${cc}`, 'success');
            document.getElementById('ccInput').value = '';
        } else {
            const text = await res.text().catch(() => '');
            showFeedback(text || 'No se pudo registrar la salida. Verifica la cédula.', 'error');
        }
    } catch {
        showFeedback('Error de conexión. Intenta nuevamente.', 'error');
    } finally {
        busy = false;
        setLoading('btnEntry', false);
        setLoading('btnExit', false);
        document.getElementById('ccInput').focus();
    }
}

/* ── Enter key ── */
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('ccInput');
    if (input) {
        input.focus();
        input.addEventListener('keydown', e => {
            if (e.key === 'Enter') registerEntry();
        });
    }
});