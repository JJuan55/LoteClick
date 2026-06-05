/**
 * LoteClick - Script de Autenticación y Control de Accesos (Módulo 0)
 * Controla el login asíncrono y la redirección según el rol de usuario corporativo.
 */
document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const errorAlert = document.getElementById("errorAlert");
    const errorMessage = document.getElementById("errorMessage");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    
    // Elementos del botón para feedback visual de carga
    const submitBtn = document.getElementById("submitBtn");
    const btnSpinner = document.getElementById("btnSpinner");
    const btnText = document.getElementById("btnText");

    // Si ya existe sesión activa, redireccionar automáticamente a la vista correspondiente
    const activeUser = sessionStorage.getItem("user");
    if (activeUser) {
        try {
            const user = JSON.parse(activeUser);
            if (!user.token) {
                sessionStorage.clear();
                return;
            }
            redirigirPorRol(user.rol);
        } catch (e) {
            sessionStorage.clear();
        }
    }

    window.getAuthHeaders = function(extraHeaders = {}) {
        const rawUser = sessionStorage.getItem("user");
        if (!rawUser) return extraHeaders;

        try {
            const user = JSON.parse(rawUser);
            if (!user.token) return extraHeaders;
            return {
                ...extraHeaders,
                "Authorization": `Bearer ${user.token}`
            };
        } catch (e) {
            return extraHeaders;
        }
    };

    // Manejar el submit del formulario
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        
        // Ocultar alertas anteriores
        errorAlert.style.display = "none";
        emailInput.style.borderColor = "";
        passwordInput.style.borderColor = "";

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        // Activar spinner en botón
        setLoadingState(true);

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    correo: email,
                    contrasena: password
                })
            });

            const data = await response.json();

            if (response.ok) {
                // Éxito: Guardar datos en sessionStorage
                sessionStorage.setItem("user", JSON.stringify(data));
                
                // Redirigir según el rol retornado
                redirigirPorRol(data.rol);
            } else {
                // Fallo (400, 401, 403, etc.): Mostrar error y resetear clave
                manejarErrorAutenticacion(response.status, data.error || "Error al iniciar sesión");
            }
        } catch (error) {
            // Error de conexión o red
            console.error("Error en la solicitud de login:", error);
            mostrarAlerta("No se pudo establecer conexión con el servidor. Intente más tarde.");
        } finally {
            // Desactivar spinner
            setLoadingState(false);
        }
    });

    /**
     * Activa o desactiva el estado de carga del botón de login
     */
    function setLoadingState(isLoading) {
        if (isLoading) {
            submitBtn.disabled = true;
            btnSpinner.classList.remove("d-none");
            btnText.textContent = "Validando...";
        } else {
            submitBtn.disabled = false;
            btnSpinner.classList.add("d-none");
            btnText.textContent = "Iniciar Sesión";
        }
    }

    /**
     * Muestra el banner de alerta en color rojo y anima la tarjeta
     */
    function mostrarAlerta(mensaje) {
        errorMessage.textContent = mensaje;
        errorAlert.style.display = "flex";
        
        // Agregar pequeña vibración con Animate.css para feedback de error
        const loginCard = document.getElementById("loginCard");
        loginCard.classList.remove("animate__fadeInUp");
        loginCard.classList.add("animate__shakeX");
        
        // Limpiar la clase de animación después de que se ejecute para permitir repetirla
        setTimeout(() => {
            loginCard.classList.remove("animate__shakeX");
        }, 1000);
    }

    /**
     * Controla visualmente el tipo de error retornado
     */
    function manejarErrorAutenticacion(status, mensaje) {
        mostrarAlerta(mensaje);
        
        // Limpiar el campo de contraseña por seguridad según el CU01
        passwordInput.value = "";
        passwordInput.focus();

        // Resaltar los inputs que causaron el error
        if (status === 403) {
            // Cuenta inactiva
            emailInput.style.borderColor = "var(--color-danger)";
        } else {
            // Credenciales incorrectas o vacías
            emailInput.style.borderColor = "var(--color-danger)";
            passwordInput.style.borderColor = "var(--color-danger)";
        }
    }

    /**
     * Redirige al usuario según su rol de acceso (CU01)
     */
    function redirigirPorRol(rol) {
        switch (rol) {
            case "ADMINISTRADOR":
                window.location.href = "dashboard.html";
                break;
            case "CONTADOR":
                window.location.href = "dashboard.html";
                break;
            case "VENDEDOR":
                window.location.href = "inventario.html";
                break;
            default:
                mostrarAlerta("Rol no identificado. Contacte al administrador.");
                sessionStorage.clear();
        }
    }
});
