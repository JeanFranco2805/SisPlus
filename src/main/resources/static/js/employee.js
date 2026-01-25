const API_BASE = '/api/';
let allEmployees = [];

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
    const alertClass = type === 'success' ? 'alert-success' : 'alert-error';
    const icon = type === 'success'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';

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

async function loadEmployees() {
    try {
        const response = await fetch(`${API_BASE}users`);

        if (!response.ok) {
            throw new Error('Error al cargar empleados');
        }

        allEmployees = await response.json();
        displayEmployees(allEmployees);

        document.getElementById('loadingIndicator').classList.add('hidden');
        document.getElementById('employeesTable').classList.remove('hidden');
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar los empleados. Por favor, recarga la página.', 'error');
        document.getElementById('loadingIndicator').innerHTML = '<p style="color: var(--error);">Error al cargar empleados</p>';
    }
}

function displayEmployees(employees) {
    const tbody = document.getElementById('employeesTableBody');

    if (employees.length === 0) {
        tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; padding: 40px; color: var(--text-gray);">
                        No se encontraron empleados
                    </td>
                </tr>
            `;
        return;
    }

    tbody.innerHTML = employees.map(emp => {
        const employeeId = emp.id || 'N/A';
        return `
                <tr>
                    <td><strong>#${employeeId}</strong></td>
                    <td>${emp.name}</td>
                    <td>${emp.lastName}</td>
                    <td>${emp.cc}</td>
                    <td><span class="badge badge-success">Activo</span></td>
                    <td>
                        <div class="action-buttons">
                            <button class="btn btn-secondary btn-sm" onclick="viewEmployee(${employeeId})" title="Ver Detalles">
                                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                                    <circle cx="12" cy="12" r="3"/>
                                </svg>
                                Ver
                            </button>
                            <button class="btn btn-primary btn-sm" onclick="openEditModal(${employeeId})" title="Editar">
                                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                                </svg>
                                Editar
                            </button>
                            <button class="btn btn-danger btn-sm" onclick="deleteEmployee(${employeeId})" title="Eliminar">
                                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <polyline points="3 6 5 6 21 6"/>
                                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                                </svg>
                                Eliminar
                            </button>
                        </div>
                    </td>
                </tr>
            `;
    }).join('');
}

function filterEmployees() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const filtered = allEmployees.filter(emp => {
        const employeeId = (emp.id || '').toString().toLowerCase();
        const fullName = `${emp.name} ${emp.lastName}`.toLowerCase();
        const cc = emp.cc.toLowerCase();
        return employeeId.includes(searchTerm) || fullName.includes(searchTerm) || cc.includes(searchTerm);
    });
    displayEmployees(filtered);
}

function openCreateModal() {
    document.getElementById('createModal').classList.add('active');
    document.getElementById('createForm').reset();
    document.getElementById('createAlert').innerHTML = '';
}

function closeCreateModal() {
    document.getElementById('createModal').classList.remove('active');
}

async function createEmployee(event) {
    event.preventDefault();

    const formData = {
        name: document.getElementById('createName').value,
        lastName: document.getElementById('createLastName').value,
        cc: document.getElementById('createCc').value
    };

    try {
        const response = await fetch(`${API_BASE}users`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            throw new Error('Error al guardar empleado');
        }

        closeCreateModal();
        showAlert('Empleado registrado exitosamente', 'success');
        loadEmployees();

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al guardar el empleado. Intenta nuevamente.', 'error', 'createAlert');
    }
}

function viewEmployee(id) {
    const employee = allEmployees.find(emp => emp.id === id);

    if (!employee) {
        showAlert('Empleado no encontrado', 'error');
        return;
    }

    document.getElementById('viewId').textContent = employee.id;
    document.getElementById('viewFullName').textContent = `${employee.name} ${employee.lastName}`;
    document.getElementById('viewName').textContent = employee.name;
    document.getElementById('viewLastName').textContent = employee.lastName;
    document.getElementById('viewCc').textContent = employee.cc;

    document.getElementById('viewModal').classList.add('active');
}

function closeViewModal() {
    document.getElementById('viewModal').classList.remove('active');
}

function openEditModal(id) {
    const employee = allEmployees.find(emp => emp.id === id);

    if (!employee) {
        showAlert('Empleado no encontrado', 'error');
        return;
    }

    document.getElementById('editId').value = employee.id;
    document.getElementById('editName').value = employee.name;
    document.getElementById('editLastName').value = employee.lastName;
    document.getElementById('editCc').value = employee.cc;

    document.getElementById('editModal').classList.add('active');
    document.getElementById('editAlert').innerHTML = '';
}

function closeEditModal() {
    document.getElementById('editModal').classList.remove('active');
}

async function updateEmployee(event) {
    event.preventDefault();

    const employeeId = document.getElementById('editId').value;
    const formData = {
        name: document.getElementById('editName').value,
        lastName: document.getElementById('editLastName').value,
        cc: document.getElementById('editCc').value
    };

    try {
        const response = await fetch(`${API_BASE}users/${employeeId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            throw new Error('Error al actualizar empleado');
        }

        closeEditModal();
        showAlert('Empleado actualizado exitosamente', 'success');
        loadEmployees();

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al actualizar el empleado. Intenta nuevamente.', 'error', 'editAlert');
    }
}

async function deleteEmployee(id) {
    if (!confirm('¿Está seguro de que desea eliminar este empleado? Esta acción no se puede deshacer.')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}users/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Error al eliminar empleado');
        }

        showAlert('Empleado eliminado exitosamente', 'success');
        loadEmployees();

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar el empleado. Intenta nuevamente.', 'error');
    }
}

window.onclick = function(event) {
    const createModal = document.getElementById('createModal');
    const viewModal = document.getElementById('viewModal');
    const editModal = document.getElementById('editModal');

    if (event.target === createModal) {
        closeCreateModal();
    }
    if (event.target === viewModal) {
        closeViewModal();
    }
    if (event.target === editModal) {
        closeEditModal();
    }
}

window.onload = function() {
    loadEmployees();
}