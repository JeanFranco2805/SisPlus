const API_BASE = '/api';

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.getElementById('mainContent');

    if (window.innerWidth <= 768) {
        sidebar.classList.toggle('open');
    } else {
        sidebar.classList.toggle('closed');
        mainContent.classList.toggle('expanded');
    }
}

window.addEventListener('resize', function() {
    const sidebar = document.getElementById('sidebar');
    if (window.innerWidth > 768) {
        sidebar.classList.remove('open');
    }
});

function formatTime(dateTimeString) {
    if (!dateTimeString) return '-';
    const date = new Date(dateTimeString);
    return date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
}

function formatDate() {
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const date = new Date().toLocaleDateString('es-ES', options);
    return date.charAt(0).toUpperCase() + date.slice(1);
}

async function loadDashboardData() {
    try {
        const [employeesRes, attendancesRes] = await Promise.all([
            fetch(`${API_BASE}/users`),
            fetch(`${API_BASE}/attendances?filter=today`)
        ]);

        if (!employeesRes.ok || !attendancesRes.ok) {
            throw new Error('Error al cargar datos del dashboard');
        }

        const employees = await employeesRes.json();
        const attendances = await attendancesRes.json();

        updateStatistics(employees, attendances);
        displayRecentAttendances(attendances);

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loadingTable').innerHTML =
            '<p style="color: var(--error);">Error al cargar datos del dashboard</p>';
    }
}

function updateStatistics(employees, attendances) {
    const totalEmployees = employees.length;
    document.getElementById('totalEmployees').textContent = totalEmployees;
    document.getElementById('employeesChange').textContent =
        `${totalEmployees} empleado${totalEmployees !== 1 ? 's' : ''} registrado${totalEmployees !== 1 ? 's' : ''}`;

    const todayAttendances = attendances.length;
    document.getElementById('todayAttendances').textContent = todayAttendances;

    const attendanceRate = totalEmployees > 0
        ? ((todayAttendances / totalEmployees) * 100).toFixed(1)
        : 0;
    document.getElementById('attendanceRate').textContent =
        `${attendanceRate}% tasa de asistencia`;

    let totalOvertimeHours = 0;
    let employeesWithOvertime = 0;
    let pendingExits = 0;

    attendances.forEach(att => {
        if (att.extraHours && att.extraHours > 0) {
            totalOvertimeHours += att.extraHours;
            employeesWithOvertime++;
        }
        if (!att.departureTime) {
            pendingExits++;
        }
    });

    document.getElementById('totalOvertimeHours').textContent = totalOvertimeHours.toFixed(1);
    document.getElementById('overtimeEmployees').textContent =
        employeesWithOvertime > 0
            ? `En ${employeesWithOvertime} empleado${employeesWithOvertime !== 1 ? 's' : ''}`
            : 'Sin horas extras registradas';

    document.getElementById('pendingAlerts').textContent = pendingExits;
    document.getElementById('alertsDescription').textContent =
        pendingExits > 0
            ? `Salida${pendingExits !== 1 ? 's' : ''} no registrada${pendingExits !== 1 ? 's' : ''}`
            : 'Sin alertas pendientes';

    const alertChangeElem = document.querySelector('#pendingAlerts').parentElement.querySelector('.stat-change');
    if (pendingExits === 0) {
        alertChangeElem.classList.remove('negative');
        alertChangeElem.classList.add('positive');
    }
}

function displayRecentAttendances(attendances) {
    const tbody = document.getElementById('recentAttendancesBody');
    const loadingDiv = document.getElementById('loadingTable');
    const table = document.getElementById('recentAttendancesTable');
    const emptyState = document.getElementById('emptyState');

    loadingDiv.style.display = 'none';

    if (attendances.length === 0) {
        emptyState.style.display = 'block';
        table.style.display = 'none';
        return;
    }

    emptyState.style.display = 'none';
    table.style.display = 'table';

    const recentAttendances = attendances
        .sort((a, b) => {
            const dateA = new Date(a.entryTime || 0);
            const dateB = new Date(b.entryTime || 0);
            return dateB - dateA;
        })
        .slice(0, 10);

    tbody.innerHTML = recentAttendances.map(att => {
        const status = att.departureTime ? 'complete' : 'pending';
        const statusBadge = status === 'complete'
            ? '<span class="badge badge-success">Completo</span>'
            : '<span class="badge badge-warning">Pendiente</span>';

        const workedHours = att.workedHours && att.departureTime
            ? att.workedHours.toFixed(2) + 'h'
            : '-';

        const userName = att.user
            ? `${att.user.name} ${att.user.lastName}`
            : 'Usuario desconocido';

        return `
                <tr>
                    <td><strong>${userName}</strong></td>
                    <td>${formatTime(att.entryTime)}</td>
                    <td>${formatTime(att.departureTime)}</td>
                    <td>${workedHours}</td>
                    <td>${statusBadge}</td>
                </tr>
            `;
    }).join('');
}

window.onload = function() {
    document.getElementById('currentDate').textContent =
        'Resumen general del sistema - ' + formatDate();
    loadDashboardData();

    setInterval(loadDashboardData, 30000);
}