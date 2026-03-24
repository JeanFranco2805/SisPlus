let isLoading = false;

function togglePassword() {
    const input = document.getElementById('password');
    const icon = document.getElementById('eyeIcon');

    if (input.type === 'password') {
        input.type = 'text';
        icon.innerHTML = `
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
            <line x1="1" y1="1" x2="23" y2="23"/>`;
    } else {
        input.type = 'password';
        icon.innerHTML = `
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>`;
    }
}

function showAlert(type, message) {
    document.getElementById('alertError').classList.remove('show');
    document.getElementById('alertSuccess').classList.remove('show');

    if (type === 'error') {
        document.getElementById('errorMessage').textContent = message;
        document.getElementById('alertError').classList.add('show');
    } else if (type === 'success') {
        document.getElementById('alertSuccess').classList.add('show');
    }
}

function hideAlerts() {
    document.getElementById('alertError').classList.remove('show');
    document.getElementById('alertSuccess').classList.remove('show');
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
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, rememberMe })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            showAlert('success');
            setTimeout(() => {
                window.location.href = data.redirectUrl || '/dashboard';
            }, 900);
        } else {
            showAlert('error', data.message || 'Credenciales incorrectas');
            loginButton.classList.remove('loading');
            loginButton.disabled = false;
            isLoading = false;
        }
    } catch (error) {
        showAlert('error', 'Error de conexión. Por favor intenta nuevamente.');
        loginButton.classList.remove('loading');
        loginButton.disabled = false;
        isLoading = false;
    }
}

document.querySelectorAll('.field-input').forEach(input => {
    input.addEventListener('input', hideAlerts);
});

document.getElementById('loginForm').addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !isLoading) {
        handleLogin(e);
    }
});