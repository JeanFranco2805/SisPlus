const API_BASE = '/api/config';
let originalConfig = {};

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
    const alertClass = type === 'success' ? 'alert-success' : (type === 'warning' ? 'alert-warning' : 'alert-error');
    const icon = type === 'success'
        ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        : type === 'warning'
            ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
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

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function loadConfiguration() {
    try {
        const response = await fetch(API_BASE);
        if (!response.ok) throw new Error('Error al cargar configuración');

        const configs = await response.json();

        configs.forEach(config => {
            originalConfig[config.key] = config.value;
        });

        document.getElementById('regularHourRate').value = originalConfig['REGULAR_HOUR_RATE'] || '7959';
        document.getElementById('dayOvertimeRate').value = originalConfig['DAY_OVERTIME_RATE'] || '9948';
        document.getElementById('nightSurchargeRate').value = originalConfig['NIGHT_SURCHARGE_RATE'] || '2786';
        document.getElementById('nightOvertimeRate').value = originalConfig['NIGHT_OVERTIME_RATE'] || '13928.25';
        document.getElementById('nightStartHour').value = originalConfig['NIGHT_START_HOUR'] || '19';
        document.getElementById('nightEndHour').value = originalConfig['NIGHT_END_HOUR'] || '6';
        document.getElementById('timeZone').value = originalConfig['TIME_ZONE'] || 'America/Bogota';

        document.getElementById('loadingIndicator').style.display = 'none';
        document.getElementById('configContent').style.display = 'block';

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('loadingIndicator').innerHTML =
            '<p style="color: var(--error);">Error al cargar la configuración. Por favor, recarga la página.</p>';
    }
}

function resetForm() {
    if (!confirm('¿Deseas restablecer todos los cambios a los valores actuales?')) {
        return;
    }

    document.getElementById('regularHourRate').value = originalConfig['REGULAR_HOUR_RATE'] || '7959';
    document.getElementById('dayOvertimeRate').value = originalConfig['DAY_OVERTIME_RATE'] || '9948';
    document.getElementById('nightSurchargeRate').value = originalConfig['NIGHT_SURCHARGE_RATE'] || '2786';
    document.getElementById('nightOvertimeRate').value = originalConfig['NIGHT_OVERTIME_RATE'] || '13928.25';
    document.getElementById('nightStartHour').value = originalConfig['NIGHT_START_HOUR'] || '19';
    document.getElementById('nightEndHour').value = originalConfig['NIGHT_END_HOUR'] || '6';
    document.getElementById('timeZone').value = originalConfig['TIME_ZONE'] || 'America/Bogota';

    showAlert('Formulario restablecido a los valores actuales', 'success');
}

async function saveConfiguration(event) {
    event.preventDefault();

    const saveButton = document.getElementById('saveButton');
    saveButton.disabled = true;
    saveButton.innerHTML = '<div class="loading-spinner" style="width: 16px; height: 16px; border-width: 2px;"></div> Guardando...';

    const configUpdates = [
        { key: 'REGULAR_HOUR_RATE', value: document.getElementById('regularHourRate').value },
        { key: 'DAY_OVERTIME_RATE', value: document.getElementById('dayOvertimeRate').value },
        { key: 'NIGHT_SURCHARGE_RATE', value: document.getElementById('nightSurchargeRate').value },
        { key: 'NIGHT_OVERTIME_RATE', value: document.getElementById('nightOvertimeRate').value },
        { key: 'NIGHT_START_HOUR', value: document.getElementById('nightStartHour').value },
        { key: 'NIGHT_END_HOUR', value: document.getElementById('nightEndHour').value },
        { key: 'TIME_ZONE', value: document.getElementById('timeZone').value }
    ];

    try {
        const promises = configUpdates.map(config =>
            fetch(`${API_BASE}/${config.key}?value=${encodeURIComponent(config.value)}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
        );

        const responses = await Promise.all(promises);

        const allSuccessful = responses.every(response => response.ok);

        if (!allSuccessful) {
            throw new Error('Error al guardar algunas configuraciones');
        }

        configUpdates.forEach(config => {
            originalConfig[config.key] = config.value;
        });

        showAlert('Configuración guardada exitosamente. Reinicia la aplicación para aplicar los cambios.', 'warning');

    } catch (error) {
        console.error('Error:', error);
        showAlert('Error al guardar la configuración. Intenta nuevamente.', 'error');
    } finally {
        saveButton.disabled = false;
        saveButton.innerHTML = `
                <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                    <polyline points="17 21 17 13 7 13 7 21"/>
                    <polyline points="7 3 7 8 15 8"/>
                </svg>
                Guardar Cambios
            `;
    }
}

document.getElementById('nightStartHour')?.addEventListener('input', function() {
    const value = parseInt(this.value);
    if (value < 0 || value > 23) {
        this.setCustomValidity('La hora debe estar entre 0 y 23');
    } else {
        this.setCustomValidity('');
    }
});

document.getElementById('nightEndHour')?.addEventListener('input', function() {
    const value = parseInt(this.value);
    if (value < 0 || value > 23) {
        this.setCustomValidity('La hora debe estar entre 0 y 23');
    } else {
        this.setCustomValidity('');
    }
});

window.onload = function() {
    loadConfiguration();
};