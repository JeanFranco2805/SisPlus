const API_BASE = '/api';

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

window.addEventListener('resize', function () {
    const sidebar = document.getElementById('sidebar');
    if (window.innerWidth > 900) {
        sidebar.classList.remove('open');
    }
});

function getGreeting() {
    const h = new Date().getHours();
    if (h >= 5  && h < 12) return 'Buenos días';
    if (h >= 12 && h < 19) return 'Buenas tardes';
    return 'Buenas noches';
}

function formatDate() {
    return new Date().toLocaleDateString('es-CO', {
        weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    }).replace(/^\w/, c => c.toUpperCase());
}

function formatTime() {
    return new Date().toLocaleTimeString('es-CO', {
        hour: '2-digit', minute: '2-digit', hour12: true
    });
}

function formatHour(dt) {
    if (!dt) return '—';
    return new Date(dt).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function initGreeting(username) {
    const greeting = getGreeting();

    const greetingEl = document.getElementById('greetingLine');
    if (greetingEl) {
        greetingEl.innerHTML = `${greeting}, <span>${username}</span>`;
    }

    const topbarGreet = document.getElementById('topbarGreeting');
    if (topbarGreet) {
        topbarGreet.innerHTML = `${greeting}, <strong>${username}</strong>`;
    }

    const timeEl = document.getElementById('topbarTime');
    if (timeEl) {
        timeEl.textContent = formatTime();
        setInterval(() => { timeEl.textContent = formatTime(); }, 30000);
    }

    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
        dateEl.textContent = formatDate();
    }

    const y = new Date().getFullYear();
    const footerYear  = document.getElementById('footerYear');
    const footerYearM = document.getElementById('footerYearMain');
    if (footerYear)  footerYear.textContent  = `v2.0 · ${y}`;
    if (footerYearM) footerYearM.textContent = y;
}

function buildSparkBars(data, colorClass, hiClass) {
    const max = Math.max(...data, 1);
    return data.map((v, i) => {
        const h = Math.round((v / max) * 100);
        const isHi = i === data.length - 1;
        return `<div class="spark-bar ${isHi ? hiClass : colorClass}" style="height:${h}%"></div>`;
    }).join('');
}

function updateStats(employees, attendances) {
    const total     = employees.length;
    const present   = attendances.length;
    const pending   = attendances.filter(a => !a.departureTime).length;
    const overtime  = attendances.reduce((s, a) => s + (a.extraHours || 0), 0);
    const rate      = total > 0 ? ((present / total) * 100).toFixed(0) : 0;

    document.getElementById('statEmployees').textContent  = total;
    document.getElementById('statAttendance').textContent = present;
    document.getElementById('statOvertime').textContent   = overtime.toFixed(1);
    document.getElementById('statPending').textContent    = pending;

    document.getElementById('pillEmployees').textContent  = `${total} registrado${total !== 1 ? 's' : ''}`;
    document.getElementById('pillAttendance').innerHTML   = `<span class="stat-pill pill-up">▲ ${rate}% asistencia</span>`;

    const overtimeCount = attendances.filter(a => (a.extraHours || 0) > 0).length;
    document.getElementById('pillOvertime').textContent   = overtimeCount > 0 ? `En ${overtimeCount} empleado${overtimeCount !== 1 ? 's' : ''}` : 'Sin horas extras';

    const pendPill = document.getElementById('pillPending');
    pendPill.className = `stat-pill ${pending > 0 ? 'pill-down' : 'pill-up'}`;
    pendPill.textContent = pending > 0 ? `${pending} sin registrar` : 'Sin alertas';

    const sparkData = [4,6,5,8,7,present].map(v => Math.max(v, 0));
    document.getElementById('sparkEmployees').innerHTML  = buildSparkBars([18,20,19,22,21,total],  'spark-bar', '');
    document.getElementById('sparkAttendance').innerHTML = buildSparkBars(sparkData, 'spark-bar', '');
    document.getElementById('sparkOvertime').innerHTML   = buildSparkBars([1,3,2,4,2.5,overtime||0], 'spark-bar', '');
    document.getElementById('sparkPending').innerHTML    = buildSparkBars([0,2,1,3,1,pending], 'spark-bar', '');

    document.querySelectorAll('#sparkEmployees  .spark-bar').forEach((b, i, a) => { b.style.background = i === a.length-1 ? 'rgba(30,58,138,.8)'  : 'rgba(30,58,138,.2)';  });
    document.querySelectorAll('#sparkAttendance .spark-bar').forEach((b, i, a) => { b.style.background = i === a.length-1 ? 'rgba(13,148,136,.8)' : 'rgba(13,148,136,.2)'; });
    document.querySelectorAll('#sparkOvertime   .spark-bar').forEach((b, i, a) => { b.style.background = i === a.length-1 ? 'rgba(217,119,6,.8)'  : 'rgba(217,119,6,.2)';  });
    document.querySelectorAll('#sparkPending    .spark-bar').forEach((b, i, a) => { b.style.background = i === a.length-1 ? 'rgba(225,29,72,.8)'   : 'rgba(225,29,72,.2)';  });
}

const AVATAR_COLORS = [
    { bg: 'rgba(30,58,138,.1)',   color: '#1E3A8A' },
    { bg: 'rgba(13,148,136,.1)',  color: '#0D9488' },
    { bg: 'rgba(217,119,6,.1)',   color: '#B45309' },
    { bg: 'rgba(225,29,72,.1)',   color: '#E11D48' },
    { bg: 'rgba(124,58,237,.1)',  color: '#7C3AED' },
    { bg: 'rgba(5,150,105,.1)',   color: '#059669' },
];

function getInitials(name, lastName) {
    return ((name || '').charAt(0) + (lastName || '').charAt(0)).toUpperCase();
}

function renderAttendances(attendances) {
    const tbody   = document.getElementById('recentBody');
    const loading = document.getElementById('tableLoading');
    const empty   = document.getElementById('tableEmpty');
    const table   = document.getElementById('recentTable');

    loading.classList.add('hidden');

    if (!attendances.length) {
        empty.classList.remove('hidden');
        table.classList.add('hidden');
        return;
    }

    empty.classList.add('hidden');
    table.classList.remove('hidden');

    const rows = attendances
        .sort((a, b) => new Date(b.entryTime || 0) - new Date(a.entryTime || 0))
        .slice(0, 10);

    tbody.innerHTML = rows.map((att, idx) => {
        const user    = att.user || {};
        const name    = user.name || 'Usuario';
        const last    = user.lastName || '';
        const initials = getInitials(name, last);
        const col     = AVATAR_COLORS[idx % AVATAR_COLORS.length];
        const complete = !!att.departureTime;
        const hours    = att.workedHours && complete ? att.workedHours.toFixed(2) + 'h' : '—';
        const extra    = (att.extraHours || 0) > 0 ? att.extraHours.toFixed(2) + 'h' : '—';

        return `
        <tr>
          <td>
            <div class="name-cell">
              <div class="emp-avatar" style="background:${col.bg};color:${col.color}">${initials}</div>
              ${name} ${last}
            </div>
          </td>
          <td>${formatHour(att.entryTime)}</td>
          <td>${formatHour(att.departureTime)}</td>
          <td>${hours}</td>
          <td>${extra}</td>
          <td><span class="badge ${complete ? 'badge-ok' : 'badge-warn'}">${complete ? 'Completo' : 'Pendiente'}</span></td>
        </tr>`;
    }).join('');
}

function renderActivity(attendances) {
    const body = document.getElementById('activityBody');
    if (!attendances.length) {
        body.innerHTML = '<div style="font-size:11px;color:var(--gray-400);padding:4px 0;">Sin actividad reciente</div>';
        return;
    }

    const events = [];
    attendances.forEach(att => {
        const user = att.user || {};
        const name = `${user.name || ''} ${user.lastName || ''}`.trim();
        if (att.departureTime) {
            events.push({ text: `${name} completó su jornada`, time: att.departureTime, color: '#0D9488' });
        }
        if (att.entryTime) {
            events.push({ text: `${name} registró entrada`, time: att.entryTime, color: '#1E3A8A' });
        }
    });

    events.sort((a, b) => new Date(b.time) - new Date(a.time));

    const now = Date.now();
    function relTime(t) {
        const diff = Math.round((now - new Date(t).getTime()) / 60000);
        if (diff < 1)  return 'Ahora mismo';
        if (diff < 60) return `Hace ${diff} min`;
        return `Hace ${Math.round(diff / 60)}h`;
    }

    body.innerHTML = events.slice(0, 4).map(e => `
      <div class="act-item">
        <div class="act-timeline">
          <div class="act-dot" style="background:${e.color}"></div>
          <div class="act-line"></div>
        </div>
        <div class="act-content">
          <div class="act-text">${e.text}</div>
          <div class="act-time">${relTime(e.time)}</div>
        </div>
      </div>`).join('');
}

async function loadDashboard() {
    try {
        const [empRes, attRes] = await Promise.all([
            fetch(`${API_BASE}/users`),
            fetch(`${API_BASE}/attendances?filter=today`)
        ]);

        if (!empRes.ok || !attRes.ok) throw new Error('Error cargando datos');

        const employees   = await empRes.json();
        const attendances = await attRes.json();

        updateStats(employees, attendances);
        renderAttendances(attendances);
        renderActivity(attendances);

    } catch (err) {
        console.error(err);
        document.getElementById('tableLoading').textContent = 'Error al cargar los datos.';
    }
}

window.onload = function () {
    initGreeting('Administrador');
    loadDashboard();
    setInterval(loadDashboard, 30000);
};