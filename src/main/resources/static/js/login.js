let isLoading = false;

function getCsrfToken() {
    const match = document.cookie.split(';')
        .map(c => c.trim())
        .find(c => c.startsWith('XSRF-TOKEN='));
    return match ? decodeURIComponent(match.split('=')[1]) : null;
}

function showAlert(type, message) {
    const alertError = document.getElementById('alertError');
    const alertSuccess = document.getElementById('alertSuccess');

    alertError.classList.add('hidden');
    alertSuccess.classList.add('hidden');

    if (type === 'error') {
        document.getElementById('errorMessage').textContent = message;
        alertError.classList.remove('hidden');
    } else if (type === 'success') {
        alertSuccess.classList.remove('hidden');
    }
}

function hideAlerts() {
    document.getElementById('alertError').classList.add('hidden');
    document.getElementById('alertSuccess').classList.add('hidden');
}

async function handleLogin(event) {
    event.preventDefault();

    if (isLoading) return;

    hideAlerts();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const rememberMe = document.getElementById('remember').checked;

    if (!username || !password) {
        showAlert('error', 'Por favor completa todos los campos');
        return;
    }

    const loginButton = document.getElementById('loginButton');
    loginButton.classList.add('loading');
    loginButton.disabled = true;
    isLoading = true;

    try {
        const csrfToken = getCsrfToken();
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken) {
            headers['X-XSRF-TOKEN'] = csrfToken;
        }

        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify({ username, password, rememberMe })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showAlert('success', 'Inicio de sesión exitoso');
            setTimeout(() => {
                window.location.href = data.redirectUrl || '/dashboard';
            }, 800);
        } else {
            showAlert('error', data.message || 'Credenciales incorrectas');
            loginButton.classList.remove('loading');
            loginButton.disabled = false;
            isLoading = false;
        }
    } catch (error) {
        console.error('Error en login:', error);
        showAlert('error', 'Error de conexión. Por favor intenta nuevamente.');
        loginButton.classList.remove('loading');
        loginButton.disabled = false;
        isLoading = false;
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('loginForm');

    form.addEventListener('submit', handleLogin);

    form.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('input', hideAlerts);
    });
});