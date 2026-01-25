const API_BASE = '/api/admin';
let allAdmins = [];

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
        : type === 'warning'
            ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
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

function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    input.type = input.type === 'password' ? 'text' : 'password';
}

async function loadAdmins() {
    try {
        const response = await fetch(`${API_BASE}/`);

        if (!response.ok) {
            throw new Error('Error al cargar administradores');
        }

        allAdmins = await response.json();
        displayAdmins(allAdmins);

        document.getElementById('loadingIndicator').classList.add('hidden');
        document.getElementById('adminsTable').classList.remove('hidden');
    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al cargar los administradores. Por favor, recarga la página.', 'error');
        document.getElementById('loadingIndicator').innerHTML = '<p style="color: var(--error);">Error al cargar administradores</p>';
    }
}

function displayAdmins(admins) {
    const tbody = document.getElementById('adminsTableBody');

    if (admins.length === 0) {
        tbody.innerHTML = `
                <tr>
                    <td colspan="4" style="text-align: center; padding: 40px; color: var(--text-gray);">
                        No se encontraron administradores
                    </td>
                </tr>
            `;
        return;
    }

    tbody.innerHTML = admins.map(admin => {
        const username = admin.username || 'N/A';
        return `
                <tr>
                    <td><strong>${username}</strong></td>
                    <td><span class="badge badge-success">ADMIN</span></td>
                    <td><span class="badge badge-success">Activo</span></td>
                    <td>
                        <div class="action-buttons">
                            <button class="btn btn-danger btn-sm" onclick="deleteAdmin('${username}')" title="Eliminar">
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

function filterAdmins() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const filtered = allAdmins.filter(admin => {
        const username = (admin.username || '').toLowerCase();
        return username.includes(searchTerm);
    });
    displayAdmins(filtered);
}

function openCreateModal() {
    document.getElementById('createModal').classList.add('active');
    document.getElementById('createForm').reset();
    document.getElementById('createAlert').innerHTML = '';
}

function closeCreateModal() {
    document.getElementById('createModal').classList.remove('active');
}

async function createAdmin(event) {
    event.preventDefault();

    const username = document.getElementById('createUsername').value;
    const password = document.getElementById('createPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        showAlert('Las contraseñas no coinciden', 'error', 'createAlert');
        return;
    }

    if (password.length < 6) {
        showAlert('La contraseña debe tener al menos 6 caracteres', 'error', 'createAlert');
        return;
    }

    const formData = {
        username: username,
        password: password
    };

    try {
        const response = await fetch(`${API_BASE}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Error al guardar administrador');
        }

        closeCreateModal();
        showAlert('Administrador registrado exitosamente', 'success');
        loadAdmins();

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al guardar el administrador. El nombre de usuario puede estar en uso.', 'error', 'createAlert');
    }
}

async function deleteAdmin(username) {
    if (!confirm(`¿Está seguro de que desea eliminar al administrador "${username}"? Esta acción no se puede deshacer.`)) {
        return;
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/${encodeURIComponent(username)}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Error al eliminar administrador');
        }

        showAlert('Administrador eliminado exitosamente', 'success');
        loadAdmins();

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al eliminar el administrador. Intenta nuevamente.', 'error');
    }
}

window.onclick = function(event) {
    const createModal = document.getElementById('createModal');

    if (event.target === createModal) {
        closeCreateModal();
    }
}

window.onload = function() {
    loadAdmins();
}