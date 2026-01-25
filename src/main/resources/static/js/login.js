let isLoading = false;

function showAlert(type, message) {
    const alertError = document.getElementById('alertError');
    const alertSuccess = document.getElementById('alertSuccess');

    alertError.classList.remove('hidden');
    alertSuccess.classList.remove('hidden');

    setTimeout(() => {
        alertError.classList.add('hidden');
        alertSuccess.classList.add('hidden');
    }, 100);

    if (type === 'error') {
        document.getElementById('errorMessage').textContent = message;
        setTimeout(() => alertError.classList.remove('hidden'), 100);
    } else if (type === 'success') {
        setTimeout(() => alertSuccess.classList.remove('hidden'), 100);
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
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username,
                password: password,
                rememberMe: rememberMe
            })
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

document.querySelectorAll('.form-input').forEach(input => {
    input.addEventListener('input', hideAlerts);
});

document.getElementById('loginForm').addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !isLoading) {
        handleLogin(e);
    }
});