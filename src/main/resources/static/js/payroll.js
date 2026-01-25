const API_BASE = '/api';
let employees = [];

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

function showAlert(message, type = 'success') {
    const alertContainer = document.getElementById('alertContainer');
    const alertClass = type === 'success' ? 'alert-success' : 'alert-error';
    const icon = type === 'success'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>';

    alertContainer.innerHTML = `
            <div class="alert ${alertClass}">
                ${icon}
                <span>${message}</span>
            </div>
        `;

    setTimeout(() => {
        alertContainer.innerHTML = '';
    }, 5000);
}

function toggleDateInputs() {
    const period = document.getElementById('periodSelect').value;
    const dateGroup = document.getElementById('dateGroup');
    const monthGroup = document.getElementById('monthGroup');
    const yearGroup = document.getElementById('yearGroup');

    if (period === 'monthly') {
        dateGroup.classList.add('hidden');
        monthGroup.classList.remove('hidden');
        yearGroup.classList.remove('hidden');
    } else {
        dateGroup.classList.remove('hidden');
        monthGroup.classList.add('hidden');
        yearGroup.classList.add('hidden');
    }
}

async function loadEmployees() {
    try {
        const response = await fetch(`${API_BASE}/users`);
        if (!response.ok) throw new Error('Error al cargar empleados');

        employees = await response.json();
        const select = document.getElementById('employeeSelect');

        select.innerHTML = '<option value="">-- Seleccione un empleado --</option>' +
            employees.map(emp => {
                return `<option value="${emp.id}">${emp.name} ${emp.lastName} - CC: ${emp.cc}</option>`;
            }).join('');
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar la lista de empleados', 'error');
    }
}

function formatCurrency(value) {
    return new Intl.NumberFormat('es-CO', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(value);
}

function formatHours(value) {
    return value.toFixed(2);
}

async function downloadExcel() {
    const month = document.getElementById('monthInput').value;
    const year = document.getElementById('yearInput').value;

    showAlert('Generando archivo Excel...', 'success');

    try {
        const response = await fetch(`${API_BASE}/payroll/export/excel?month=${month}&year=${year}`);

        if (!response.ok) {
            throw new Error('Error al generar el archivo Excel');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Nomina_${getMonthName(month)}_${year}.xlsx`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

        showAlert('Archivo Excel descargado exitosamente', 'success');
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al descargar el archivo Excel', 'error');
    }
}

function getMonthName(month) {
    const monthNames = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    return monthNames[month - 1];
}

async function calculatePayroll() {
    const employeeId = document.getElementById('employeeSelect').value;
    const period = document.getElementById('periodSelect').value;

    if (!employeeId) {
        showAlert('Por favor seleccione un empleado', 'error');
        return;
    }

    let url = `${API_BASE}/users/${employeeId}/payroll?period=${period}`;

    if (period === 'monthly') {
        const month = document.getElementById('monthInput').value;
        const year = document.getElementById('yearInput').value;
        url += `&month=${month}&year=${year}`;
    } else {
        const date = document.getElementById('dateInput').value;
        if (date) {
            url += `&date=${date}`;
        }
    }

    let attendancesUrl = `${API_BASE}/attendances?userId=${employeeId}`;

    if (period === 'monthly') {
        const month = document.getElementById('monthInput').value;
        const year = document.getElementById('yearInput').value;
        const firstDay = new Date(year, month - 1, 1);
        const lastDay = new Date(year, month, 0);
        attendancesUrl += `&startDate=${firstDay.toISOString().split('T')[0]}&endDate=${lastDay.toISOString().split('T')[0]}`;
    } else if (period === 'weekly') {
        const date = document.getElementById('dateInput').value || new Date().toISOString().split('T')[0];
        const endDate = new Date(date);
        const startDate = new Date(endDate);
        startDate.setDate(startDate.getDate() - 6);
        attendancesUrl += `&startDate=${startDate.toISOString().split('T')[0]}&endDate=${endDate.toISOString().split('T')[0]}`;
    } else {
        const date = document.getElementById('dateInput').value || new Date().toISOString().split('T')[0];
        attendancesUrl += `&date=${date}`;
    }

    document.getElementById('loadingIndicator').classList.remove('hidden');
    document.getElementById('resultsContainer').classList.add('hidden');

    try {
        const [payrollResponse, attendancesResponse] = await Promise.all([
            fetch(url),
            fetch(attendancesUrl)
        ]);

        if (!payrollResponse.ok) throw new Error('Error al calcular nómina');
        if (!attendancesResponse.ok) throw new Error('Error al obtener asistencias');

        const payrollData = await payrollResponse.json();
        const attendancesData = await attendancesResponse.json();

        displayResults(payrollData, attendancesData);

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al calcular la nómina: ' + error.message, 'error');
    } finally {
        document.getElementById('loadingIndicator').classList.add('hidden');
    }
}

function displayResults(data, attendancesData) {
    const employee = employees.find(e => e.id === data.id);
    const initials = employee ? (employee.name.charAt(0) + employee.lastName.charAt(0)).toUpperCase() : 'EM';
    const period = document.getElementById('periodSelect').value;

    document.getElementById('employeeInfoContainer').innerHTML = `
            <div class="employee-avatar">${initials}</div>
            <div class="employee-details">
                <h3>${data.name} ${data.lastName}</h3>
                <p>CC: ${data.cc}</p>
            </div>
        `;

    const attendanceCount = Array.isArray(attendancesData) ? attendancesData.length : 0;

    let periodText = '';
    let daysInfo = '';
    let alertMessage = '';

    switch(period) {
        case 'daily':
            periodText = 'Período: Diario';
            if (attendanceCount === 0) {
                daysInfo = 'Sin registros de asistencia para este día';
                alertMessage = 'El empleado no tiene asistencia registrada para este día';
            } else if (attendanceCount === 1) {
                daysInfo = '1 asistencia encontrada';
                alertMessage = 'Nómina calculada para 1 día trabajado';
            } else {
                daysInfo = `${attendanceCount} asistencias encontradas (registro duplicado - revisar)`;
                alertMessage = 'Advertencia: Se encontraron múltiples asistencias para el mismo día';
            }
            break;

        case 'weekly':
            periodText = 'Período: Semanal (últimos 7 días)';
            if (attendanceCount === 0) {
                daysInfo = 'Sin registros de asistencia en la semana';
                alertMessage = 'El empleado no tiene asistencias registradas en esta semana';
            } else if (attendanceCount === 1) {
                daysInfo = '⚠️ Solo 1 asistencia en la semana - Mostrando acumulado';
                alertMessage = 'Este empleado solo tiene 1 asistencia registrada en la semana. Los valores mostrados corresponden al acumulado de ese único día trabajado.';
            } else {
                daysInfo = `${attendanceCount} asistencias en la semana - Mostrando acumulado total`;
                alertMessage = `Nómina calculada correctamente. Acumulado de ${attendanceCount} días trabajados en la semana.`;
            }
            break;

        case 'monthly':
            const month = document.getElementById('monthInput').value;
            const monthNames = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
            const year = document.getElementById('yearInput').value;
            periodText = `Período: ${monthNames[month - 1]} ${year}`;

            if (attendanceCount === 0) {
                daysInfo = 'Sin registros de asistencia en el mes';
                alertMessage = 'El empleado no tiene asistencias registradas en este mes';
            } else if (attendanceCount === 1) {
                daysInfo = '⚠️ Solo 1 asistencia en el mes - Mostrando acumulado';
                alertMessage = 'Este empleado solo tiene 1 asistencia registrada en el mes. Los valores mostrados corresponden al acumulado de ese único día trabajado.';
            } else {
                daysInfo = `${attendanceCount} asistencias en el mes - Mostrando acumulado total`;
                alertMessage = `Nómina calculada correctamente. Acumulado de ${attendanceCount} días trabajados en el mes.`;
            }
            break;
    }

    document.getElementById('periodInfo').textContent = periodText;
    document.getElementById('daysWorkedInfo').textContent = daysInfo;

    document.getElementById('regularHours').textContent = formatHours(data.regularHours || 0);
    document.getElementById('dayOvertimeHours').textContent = formatHours(data.dayOvertimeHours || 0);
    document.getElementById('nightOvertimeHours').textContent = formatHours(data.nightOvertimeHours || 0);
    document.getElementById('nightHours').textContent = formatHours(data.nightHours || 0);

    document.getElementById('regularPay').textContent = formatCurrency(data.regularPay || 0);
    document.getElementById('dayOvertimePay').textContent = formatCurrency(data.dayOvertimePay || 0);
    document.getElementById('nightOvertimePay').textContent = formatCurrency(data.nightOvertimePay || 0);
    document.getElementById('nightSurchargePay').textContent = formatCurrency(data.nightSurchargePay || 0);
    document.getElementById('totalOvertimePay').textContent = formatCurrency(data.totalOvertimePay || 0);
    document.getElementById('totalPay').textContent = formatCurrency(data.totalPay || 0);

    document.getElementById('resultsContainer').classList.remove('hidden');

    if (data.totalPay === 0) {
        showAlert(alertMessage, 'error');
    } else if (attendanceCount === 1 && period !== 'daily') {
        showAlert(alertMessage, 'error');
    } else {
        showAlert(alertMessage, 'success');
    }
}

window.onload = function() {
    loadEmployees();
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('dateInput').value = today;
    const currentMonth = new Date().getMonth() + 1;
    document.getElementById('monthInput').value = currentMonth;
}