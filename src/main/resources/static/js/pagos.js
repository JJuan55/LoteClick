document.addEventListener("DOMContentLoaded", () => {
    // 1. Validar y cargar sesión
    const user = JSON.parse(sessionStorage.getItem("user"));
    if (user) {
        document.getElementById("userNameLabel").innerHTML = `<i class="bi bi-person-fill"></i> ${user.nombreCompleto} (${user.rol})`;
        
        // Configurar accesibilidad según el rol
        if (user.rol === "ADMINISTRADOR") {
            if (document.getElementById("menuDashboard")) {
                document.getElementById("menuDashboard").style.display = "block";
            }
            if (document.getElementById("menuLotes")) {
                document.getElementById("menuLotes").style.display = "block";
            }
        }
    } else {
        alert("Acceso denegado. Inicie sesión primero.");
        window.location.href = "index.html";
        return;
    }

    // Manejo de Logout
    document.getElementById("logoutBtn").addEventListener("click", () => {
        sessionStorage.clear();
        window.location.href = "index.html";
    });

    // 2. Utilidades de Formato
    function formatCOP(val) {
        return new Intl.NumberFormat('es-CO', {
            style: 'currency',
            currency: 'COP',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(val);
    }

    // Establecer fecha por defecto en el modal (hoy)
    const egresoFechaInput = document.getElementById("egresoFecha");
    if (egresoFechaInput) {
        egresoFechaInput.value = new Date().toISOString().split('T')[0];
    }

    // Modal de bootstrap
    const egresoModalEl = document.getElementById("egresoModal");
    const egresoModal = egresoModalEl ? new bootstrap.Modal(egresoModalEl) : null;

    // 3. Cargar listado de egresos
    async function cargarEgresos(fechaInicio = "", fechaFin = "") {
        const tableBody = document.getElementById("egresosTableBody");
        if (!tableBody) return;

        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-4 text-muted">
                    <span class="spinner-border spinner-border-sm me-2" role="status"></span> Consultando egresos en Supabase...
                </td>
            </tr>
        `;

        try {
            let url = "/api/egresos/liquidar";
            const params = new URLSearchParams();
            if (fechaInicio) params.append("fechaInicio", fechaInicio);
            if (fechaFin) params.append("fechaFin", fechaFin);
            
            if (params.toString()) {
                url += "?" + params.toString();
            }

            const response = await fetch(url);
            if (!response.ok) {
                throw new Error("No se pudo obtener la liquidación de egresos.");
            }

            const egresos = await response.json();
            tableBody.innerHTML = "";

            let totalAcumulado = 0;

            if (egresos.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center py-4 text-muted">
                            <i class="bi bi-info-circle fs-4 d-block mb-2"></i> No se encontraron egresos en el rango seleccionado.
                        </td>
                    </tr>
                `;
                document.getElementById("totalEgresosLabel").textContent = formatCOP(0);
                document.getElementById("cantidadEgresosLabel").textContent = "0 registros";
                return;
            }

            egresos.forEach(eg => {
                totalAcumulado += eg.monto;
                const tr = document.createElement("tr");
                tr.className = "animate__animated animate__fadeIn";
                tr.innerHTML = `
                    <td class="fw-bold text-navy">${eg.fechaEgreso}</td>
                    <td><span class="badge bg-navy text-white text-uppercase" style="font-size: 0.75rem;">${eg.rubro}</span></td>
                    <td class="text-muted small">${eg.descripcion || 'Sin descripción'}</td>
                    <td class="fw-bold text-danger">${formatCOP(eg.monto)}</td>
                    <td class="small text-navy"><i class="bi bi-person-circle"></i> ${eg.contador ? eg.contador.nombreCompleto : 'Sistema'}</td>
                `;
                tableBody.appendChild(tr);
            });

            // Actualizar paneles de resumen
            document.getElementById("totalEgresosLabel").textContent = formatCOP(totalAcumulado);
            document.getElementById("cantidadEgresosLabel").textContent = `${egresos.length} registro${egresos.length > 1 ? 's' : ''}`;

        } catch (error) {
            console.error(error);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-4 text-danger">
                        <i class="bi bi-wifi-off fs-4 d-block mb-2"></i> Error al conectar con el servidor.
                    </td>
                </tr>
            `;
        }
    }

    // Cargar inicialmente sin filtros
    cargarEgresos();

    // 4. Formulario de Filtros / Liquidar
    const filtroEgresosForm = document.getElementById("filtroEgresosForm");
    if (filtroEgresosForm) {
        filtroEgresosForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const start = document.getElementById("fechaInicio").value;
            const end = document.getElementById("fechaFin").value;
            
            if (start && end && start > end) {
                alert("La fecha de inicio no puede ser posterior a la fecha de fin.");
                return;
            }
            cargarEgresos(start, end);
        });
    }

    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");
    if (btnLimpiarFiltros) {
        btnLimpiarFiltros.addEventListener("click", () => {
            document.getElementById("fechaInicio").value = "";
            document.getElementById("fechaFin").value = "";
            cargarEgresos();
        });
    }

    // 5. Registro de nuevo Egreso
    const egresoForm = document.getElementById("egresoForm");
    if (egresoForm) {
        egresoForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const rubro = document.getElementById("egresoRubro").value;
            const monto = document.getElementById("egresoMonto").value;
            const fecha = document.getElementById("egresoFecha").value;
            const descripcion = document.getElementById("egresoDescripcion").value;

            const spinner = document.getElementById("egresoSpinner");
            const confirmBtn = document.getElementById("btnConfirmarEgreso");
            const alertDiv = document.getElementById("egresoAlert");
            const alertText = document.getElementById("egresoAlertText");

            if (!rubro || !monto || !fecha || !descripcion) {
                alertText.textContent = "Todos los campos con asterisco (*) son obligatorios.";
                alertDiv.classList.remove("d-none");
                return;
            }

            if (parseFloat(monto) <= 0) {
                alertText.textContent = "El monto debe ser superior a cero.";
                alertDiv.classList.remove("d-none");
                return;
            }

            spinner.classList.remove("d-none");
            confirmBtn.disabled = true;
            alertDiv.classList.add("d-none");

            try {
                const response = await fetch("/api/egresos", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        contadorCorreo: user.correo,
                        monto: parseFloat(monto),
                        fechaEgreso: fecha,
                        rubro: rubro,
                        descripcion: descripcion
                    })
                });

                const resData = await response.json();

                if (response.ok) {
                    // Limpieza y cierre
                    if (egresoModal) {
                        egresoModal.hide();
                    }
                    egresoForm.reset();
                    
                    // Restablecer fecha a hoy
                    if (egresoFechaInput) {
                        egresoFechaInput.value = new Date().toISOString().split('T')[0];
                    }

                    alert("¡Gasto operativo registrado con éxito!");
                    
                    // Recargar tabla de egresos
                    cargarEgresos(
                        document.getElementById("fechaInicio").value,
                        document.getElementById("fechaFin").value
                    );
                } else {
                    alertText.textContent = resData.error || "No se pudo registrar el egreso.";
                    alertDiv.classList.remove("d-none");
                }
            } catch (err) {
                console.error(err);
                alertText.textContent = "Error de red al conectar con el servidor.";
                alertDiv.classList.remove("d-none");
            } finally {
                confirmBtn.disabled = false;
                spinner.classList.add("d-none");
            }
        });
    }

    // 6. Restablecer Base de Datos
    const resetBtn = document.getElementById("resetDbBtn");
    if (resetBtn) {
        resetBtn.addEventListener("click", async () => {
            if (confirm("¿Está seguro de que desea restablecer la base de datos al estado inicial? Se borrarán todos los pagos, egresos y contratos registrados durante la prueba.")) {
                resetBtn.disabled = true;
                resetBtn.innerHTML = `<span class="spinner-border spinner-border-sm" role="status"></span> Restableciendo...`;
                try {
                    const response = await fetch("/api/test/reset", {
                        method: "POST"
                    });
                    if (response.ok) {
                        alert("Base de datos restablecida exitosamente.");
                        window.location.reload();
                    } else {
                        const data = await response.json();
                        alert("Error al restablecer base de datos: " + (data.error || "Error desconocido"));
                        resetBtn.disabled = false;
                        resetBtn.innerHTML = `<i class="bi bi-arrow-clockwise"></i> Restablecer BD`;
                    }
                } catch (err) {
                    console.error(err);
                    alert("Error de conexión al restablecer base de datos.");
                    resetBtn.disabled = false;
                    resetBtn.innerHTML = `<i class="bi bi-arrow-clockwise"></i> Restablecer BD`;
                }
            }
        });
    }
});
