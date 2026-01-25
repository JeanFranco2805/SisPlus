
const API_BASE = '/api';
let allAttendances = [];
let filteredAttendances = [];
let employees = [];

const ITEMS_PER_PAGE = 15;
let currentPage = 1;
let totalPages = 1;

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

function showAlert(message, type = 'success', container = 'alertContainer') {
    const alertContainer = document.getElementById(container);
    const alertClass = type === 'success' ? 'alert-success' : (type === 'warning' ? 'alert-warning' : 'alert-error');
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

function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '-';
    const date = new Date(dateTimeString);
    return date.toLocaleString('es-ES', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatTime(dateTimeString) {
    if (!dateTimeString) return '-';
    const date = new Date(dateTimeString);
    return date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
}

function formatDateTimeLocal(dateTimeString) {
    if (!dateTimeString) return '';
    const date = new Date(dateTimeString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

async function loadEmployees() {
    try {
        const response = await fetch(`${API_BASE}/users`);
        if (!response.ok) throw new Error('Error al cargar empleados');

        employees = await response.json();
        populateEmployeeSelects();
    } catch (error) {
        console.error('Error al cargar empleados:', error);
        showAlert('Error al cargar la lista de empleados', 'error');
    }
}

function populateEmployeeSelects() {
    const entrySelect = document.getElementById('entryEmployeeId');
    const exitSelect = document.getElementById('exitEmployeeId');

    const options = employees.map(emp => {
        const id = emp.id || emp.id;
        return `<option value="${id}">${emp.name} ${emp.lastName} - CC: ${emp.cc}</option>`;
    }).join('');

    entrySelect.innerHTML = '<option value="">-- Seleccione un empleado --</option>' + options;
    exitSelect.innerHTML = '<option value="">-- Seleccione un empleado --</option>' + options;
}

async function changePeriodFilter() {
    const filter = document.getElementById('periodFilter').value;
    await loadAttendances(filter);
}

async function loadAttendances(filter = 'today') {
    try {
        let url = `${API_BASE}/attendances?filter=${filter}`;

        console.log('Cargando asistencias desde:', url);
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`Error al cargar asistencias: ${response.status}`);
        }

        allAttendances = await response.json();
        console.log('Asistencias cargadas:', allAttendances);

        filterAttendances();
        updatePendingAlert();

        document.getElementById('loadingIndicator').classList.add('hidden');
        document.getElementById('attendanceTable').classList.remove('hidden');
        document.getElementById('paginationContainer').classList.remove('hidden');

    } catch (error) {
        console.error('Error al cargar asistencias:', error);
        showAlert('Error al cargar las asistencias. ' + error.message, 'error');
        document.getElementById('loadingIndicator').innerHTML = '<p style="color: var(--error);">Error al cargar asistencias: ' + error.message + '</p>';
    }
}

function filterAttendances() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;

    filteredAttendances = allAttendances.filter(att => {
        const fullName = att.user ? `${att.user.name} ${att.user.lastName}`.toLowerCase() : '';
        const matchesSearch = fullName.includes(searchTerm);

        let matchesStatus = true;
        if (statusFilter === 'complete') {
            matchesStatus = att.departureTime !== null;
        } else if (statusFilter === 'pending') {
            matchesStatus = att.departureTime === null;
        }

        return matchesSearch && matchesStatus;
    });

    currentPage = 1;
    updatePagination();
    displayCurrentPage();
}

function updatePagination() {
    totalPages = Math.ceil(filteredAttendances.length / ITEMS_PER_PAGE);

    const totalRecords = filteredAttendances.length;
    const start = totalRecords > 0 ? (currentPage - 1) * ITEMS_PER_PAGE + 1 : 0;
    const end = Math.min(currentPage * ITEMS_PER_PAGE, totalRecords);

    document.getElementById('totalRecords').textContent = totalRecords;
    document.getElementById('showingStart').textContent = start;
    document.getElementById('showingEnd').textContent = end;

    document.getElementById('firstPageBtn').disabled = currentPage === 1;
    document.getElementById('prevPageBtn').disabled = currentPage === 1;
    document.getElementById('nextPageBtn').disabled = currentPage === totalPages || totalPages === 0;
    document.getElementById('lastPageBtn').disabled = currentPage === totalPages || totalPages === 0;

    renderPageNumbers();
}

function renderPageNumbers() {
    const pageNumbersContainer = document.getElementById('pageNumbers');
    let pages = [];

    for (let i = 1; i <= totalPages; i++) {
        if (i === 1 || i === totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
            pages.push(i);
        } else if (pages[pages.length - 1] !== '...') {
            pages.push('...');
        }
    }

    pageNumbersContainer.innerHTML = pages.map(page => {
        if (page === '...') {
            return '<span style="padding: 8px;">...</span>';
        }
        return `<button class="pagination-btn ${page === currentPage ? 'active' : ''}" onclick="goToPage(${page})">${page}</button>`;
    }).join('');
}

function displayCurrentPage() {
    const start = (currentPage - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    const pageAttendances = filteredAttendances.slice(start, end);

    displayAttendances(pageAttendances);
    updatePagination();
}

function goToPage(page) {
    currentPage = page;
    displayCurrentPage();
}

function previousPage() {
    if (currentPage > 1) {
        currentPage--;
        displayCurrentPage();
    }
}

function nextPage() {
    if (currentPage < totalPages) {
        currentPage++;
        displayCurrentPage();
    }
}

function goToLastPage() {
    currentPage = totalPages;
    displayCurrentPage();
}

function displayAttendances(attendances) {
    const tbody = document.getElementById('attendanceTableBody');

    if (attendances.length === 0) {
        tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px; color: var(--text-gray);">
                        No se encontraron asistencias
                    </td>
                </tr>
            `;
        return;
    }

    tbody.innerHTML = attendances.map(att => {
        const status = att.departureTime ? 'complete' : 'pending';
        const statusBadge = status === 'complete'
            ? '<span class="badge badge-success">Completo</span>'
            : '<span class="badge badge-warning">Pendiente</span>';

        const workedHours = att.workedHours ? att.workedHours.toFixed(2) + 'h' : '-';
        const extraHours = att.extraHours ? att.extraHours.toFixed(2) + 'h' : '0h';
        const nightHours = att.nightHours ? att.nightHours.toFixed(2) + 'h' : '0h';
        const userId = att.user?.id || 'N/A';
        const userName = att.user ? `${att.user.name} ${att.user.lastName}` : 'Usuario desconocido';
        const attendanceId = att.id;

        return `
                <tr>
                    <td><strong>${userName}</strong></td>
                    <td>${formatTime(att.entryTime)}</td>
                    <td>${formatTime(att.departureTime)}</td>
                    <td>${workedHours}</td>
                    <td>${extraHours}</td>
                    <td>${nightHours}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="btn btn-secondary btn-sm" onclick="openEditModal(${attendanceId})">
                                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                </svg>
                                Editar
                            </button>
                            ${status === 'pending'
            ? `<button class="btn btn-primary btn-sm" onclick="quickRegisterExit(${userId})">Salida</button>`
            : `<button class="btn btn-danger btn-sm" onclick="deleteAttendance(${attendanceId})">
                                    <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="3 6 5 6 21 6"/>
                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                    </svg>
                                   </button>`
        }
                        </div>
                    </td>
                </tr>
            `;
    }).join('');
}

function updatePendingAlert() {
    const pending = allAttendances.filter(att => !att.departureTime).length;
    const alertContainer = document.getElementById('alertContainer');

    if (pending > 0) {
        alertContainer.innerHTML = `
                <div class="alert alert-warning">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    <span><strong>${pending} empleado${pending > 1 ? 's' : ''}</strong> con salidas pendientes de registro</span>
                </div>
            `;
    } else {
        alertContainer.innerHTML = '';
    }
}

// Modales - Entrada
function openEntryModal() {
    document.getElementById('entryModal').classList.add('active');
    document.getElementById('entryForm').reset();
    document.getElementById('entryAlert').innerHTML = '';
}

function closeEntryModal() {
    document.getElementById('entryModal').classList.remove('active');
}

async function registerEntry(event) {
    event.preventDefault();

    const employeeId = document.getElementById('entryEmployeeId').value;

    if (!employeeId) {
        showAlert('Por favor selecciona un empleado', 'error', 'entryAlert');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/users/${employeeId}/entry`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || `Error al registrar entrada: ${response.status}`);
        }

        closeEntryModal();
        showAlert('Entrada registrada exitosamente', 'success');
        const currentFilter = document.getElementById('periodFilter').value;
        await loadAttendances(currentFilter);

    } catch (error) {
        console.error('Error completo:', error);
        showAlert('Error al registrar la entrada: ' + error.message, 'error', 'entryAlert');
    }
}

// Modales - Salida
function openExitModal() {
    document.getElementById('exitModal').classList.add('active');
    document.getElementById('exitForm').reset();
    document.getElementById('exitAlert').innerHTML = '';
}

function closeExitModal() {
    document.getElementById('exitModal').classList.remove('active');
}

async function registerExit(event) {
    event.preventDefault();

    const employeeId = document.getElementById('exitEmployeeId').value;

    if (!employeeId) {
        showAlert('Por favor selecciona un empleado', 'error', 'exitAlert');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/users/${employeeId}/entry`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || `Error al registrar salida: ${response.status}`);
        }

        closeExitModal();
        showAlert('Salida registrada exitosamente', 'success');
        const currentFilter = document.getElementById('periodFilter').value;
        await loadAttendances(currentFilter);

    } catch (error) {
        console.error('Error completo:', error);
        showAlert('Error al registrar la salida: ' + error.message, 'error', 'exitAlert');
    }
}

async function quickRegisterExit(employeeId) {
    if (!confirm('¿Desea registrar la salida de este empleado ahora?')) return;

    try {
        const response = await fetch(`${API_BASE}/users/${employeeId}/exit`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || `Error: ${response.status}`);
        }

        showAlert('Salida registrada exitosamente', 'success');
        const currentFilter = document.getElementById('periodFilter').value;
        await loadAttendances(currentFilter);

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al registrar la salida: ' + error.message, 'error');
    }
}

// Modales - Editar
async function openEditModal(attendanceId) {
    try {
        const response = await fetch(`${API_BASE}/attendances/${attendanceId}`);
        if (!response.ok) throw new Error('Error al cargar la asistencia');

        const attendance = await response.json();

        document.getElementById('editAttendanceId').value = attendanceId;
        document.getElementById('editEmployeeName').value = `${attendance.user.name} ${attendance.user.lastName}`;
        document.getElementById('editEntryTime').value = formatDateTimeLocal(attendance.entryTime);
        document.getElementById('editDepartureTime').value = formatDateTimeLocal(attendance.departureTime);

        document.getElementById('editModal').classList.add('active');
        document.getElementById('editAlert').innerHTML = '';
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar los datos de la asistencia', 'error');
    }
}

function closeEditModal() {
    document.getElementById('editModal').classList.remove('active');
}

async function updateAttendance(event) {
    event.preventDefault();

    const attendanceId = document.getElementById('editAttendanceId').value;
    const entryTime = document.getElementById('editEntryTime').value;
    const departureTime = document.getElementById('editDepartureTime').value;

    if (!entryTime) {
        showAlert('La hora de entrada es obligatoria', 'error', 'editAlert');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/attendances/${attendanceId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                entryTime: entryTime || null,
                departureTime: departureTime || null
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Error al actualizar la asistencia');
        }

        closeEditModal();
        showAlert('Asistencia actualizada exitosamente', 'success');
        const currentFilter = document.getElementById('periodFilter').value;
        await loadAttendances(currentFilter);

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al actualizar la asistencia: ' + error.message, 'error', 'editAlert');
    }
}

// Eliminar asistencia
async function deleteAttendance(attendanceId) {
    if (!confirm('¿Está seguro de que desea eliminar esta asistencia? Esta acción no se puede deshacer.')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/attendances/${attendanceId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Error al eliminar la asistencia');
        }

        showAlert('Asistencia eliminada exitosamente', 'success');
        const currentFilter = document.getElementById('periodFilter').value;
        await loadAttendances(currentFilter);

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar la asistencia: ' + error.message, 'error');
    }
}

// Cerrar modales al hacer clic fuera
window.onclick = function(event) {
    const entryModal = document.getElementById('entryModal');
    const exitModal = document.getElementById('exitModal');
    const editModal = document.getElementById('editModal');

    if (event.target === entryModal) {
        closeEntryModal();
    }
    if (event.target === exitModal) {
        closeExitModal();
    }
    if (event.target === editModal) {
        closeEditModal();
    }
}

// Inicialización
window.onload = function() {
    loadEmployees();
    loadAttendances('today');
}