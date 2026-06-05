document.addEventListener("DOMContentLoaded", () => {
    const user = getActiveUser();
    if (!user) {
        window.location.href = "index.html";
        return;
    }

    // Cargar Nombre y Rol en la Cabecera
    const userNameLabel = document.getElementById("userNameLabel");
    if (userNameLabel) {
        userNameLabel.innerHTML = `<i class="bi bi-person-fill"></i> ${user.nombreCompleto} (${user.rol})`;
    }

    // Configurar Menú según el Rol
    if (user.rol === "ADMINISTRADOR") {
        document.getElementById("menuDashboard").style.display = "block";
        document.getElementById("menuEgresos").style.display = "block";
        document.getElementById("menuPersonal").style.display = "block";
        document.getElementById("menuSocios").style.display = "block";
    }

    // Manejo de Logout
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", () => {
            sessionStorage.clear();
            window.location.href = "index.html";
        });
    }

    // Formatear precio en Pesos Colombianos (COP) sin decimales
    function formatearPrecio(val) {
        if (val === null || val === undefined) return "$0";
        return new Intl.NumberFormat('es-CO', {
            style: 'currency',
            currency: 'COP',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(val);
    }

    /**
     * Obtiene y renderiza la lista de ventas del backend
     */
    async function cargarHistorialVentas() {
        const cuerpoMisVentas = document.getElementById("cuerpoMisVentas");
        const misVentasCount = document.getElementById("misVentasCount");
        if (!cuerpoMisVentas) return;

        try {
            const response = await fetch("/api/ventas/mis-ventas", {
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error("No se pudo obtener el historial de ventas.");
            }

            const ventas = await response.json();
            if (misVentasCount) {
                misVentasCount.textContent = `${ventas.length} ${ventas.length === 1 ? 'Venta' : 'Ventas'}`;
            }

            cuerpoMisVentas.innerHTML = "";
            if (ventas.length === 0) {
                cuerpoMisVentas.innerHTML = `
                    <tr>
                        <td colspan="8" class="text-center py-4 text-muted">
                            No se encontraron registros de ventas concretadas.
                        </td>
                    </tr>
                `;
                return;
            }

            ventas.forEach(v => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td class="fw-bold text-navy">Lote ${v.numeroLote}</td>
                    <td><span class="badge bg-light text-navy border">${v.etapaNombre}</span></td>
                    <td class="fw-medium">${v.vendedorNombre}</td>
                    <td class="fw-medium text-navy">${v.compradorNombre}</td>
                    <td class="fw-bold text-success">${formatearPrecio(v.cuotaSeparacion)}</td>
                    <td class="fw-bold text-navy">${v.plazoMeses} meses</td>
                    <td class="fw-bold text-success">${formatearPrecio(v.precioVentaPactado)}</td>
                    <td class="small text-muted">${v.fechaVenta}</td>
                `;
                cuerpoMisVentas.appendChild(tr);
            });

        } catch (error) {
            console.error("Error al cargar ventas:", error);
            cuerpoMisVentas.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-4 text-danger small">
                        <i class="bi bi-exclamation-triangle-fill"></i> Error al cargar el registro de ventas.
                    </td>
                </tr>
            `;
        }
    }

    // Inicializar carga
    cargarHistorialVentas();
});
