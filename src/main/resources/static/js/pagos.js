document.addEventListener("DOMContentLoaded", () => {
    // 1. Validar y cargar sesión
    const user = typeof getActiveUser === "function"
        ? getActiveUser()
        : JSON.parse(sessionStorage.getItem("user"));

    if (user && user.token) {
        document.getElementById("userNameLabel").innerHTML = `<i class="bi bi-person-fill"></i> ${user.nombreCompleto} (${user.rol})`;
        
        // Configurar accesibilidad según el rol
        if (user.rol === "ADMINISTRADOR") {
            if (document.getElementById("menuDashboard")) {
                document.getElementById("menuDashboard").style.display = "block";
            }
            if (document.getElementById("menuLotes")) {
                document.getElementById("menuLotes").style.display = "block";
            }
            if (document.getElementById("menuVentas")) {
                document.getElementById("menuVentas").style.display = "block";
            }
            if (document.getElementById("menuPersonal")) {
                document.getElementById("menuPersonal").style.display = "block";
            }
            if (document.getElementById("menuSocios")) {
                document.getElementById("menuSocios").style.display = "block";
            }
        } else if (user.rol === "CONTADOR") {
            if (document.getElementById("menuDashboard")) {
                document.getElementById("menuDashboard").style.display = "block";
            }
            if (document.getElementById("menuSocios")) {
                document.getElementById("menuSocios").style.display = "block";
            }
        }
        if (user.rol !== "ADMINISTRADOR" && document.getElementById("resetDbBtn")) {
            document.getElementById("resetDbBtn").style.display = "none";
        }
    } else {
        alert("La sesión no es válida o expiró. Inicie sesión nuevamente.");
        sessionStorage.clear();
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

    async function parseApiResponse(response) {
        const rawText = await response.text();
        if (!rawText) {
            return {};
        }

        try {
            return JSON.parse(rawText);
        } catch (error) {
            throw new Error("El servidor devolvió una respuesta inválida.");
        }
    }

    function manejarSesionInvalida(mensaje) {
        alert(mensaje || "La sesión no es válida. Inicia sesión nuevamente.");
        sessionStorage.clear();
        window.location.href = "index.html";
    }

    // Establecer fecha por defecto en el modal (hoy)
    const egresoFechaInput = document.getElementById("egresoFecha");
    if (egresoFechaInput) {
        egresoFechaInput.value = new Date().toISOString().split('T')[0];
    }
    const aporteFechaInput = document.getElementById("aporteFecha");
    if (aporteFechaInput) {
        aporteFechaInput.value = new Date().toISOString().split('T')[0];
    }
    const repartoFechaInput = document.getElementById("repartoFecha");
    if (repartoFechaInput) {
        repartoFechaInput.value = new Date().toISOString().split('T')[0];
    }

    // Modal de bootstrap
    const egresoModalEl = document.getElementById("egresoModal");
    const egresoModal = egresoModalEl ? new bootstrap.Modal(egresoModalEl) : null;
    const aporteModalEl = document.getElementById("aporteModal");
    const aporteModal = aporteModalEl ? new bootstrap.Modal(aporteModalEl) : null;
    const socioModalEl = document.getElementById("socioModal");
    const socioModal = socioModalEl ? new bootstrap.Modal(socioModalEl) : null;
    const repartoModalEl = document.getElementById("repartoModal");
    const repartoModal = repartoModalEl ? new bootstrap.Modal(repartoModalEl) : null;

    let sociosCache = [];

    function obtenerFiltrosActuales() {
        return {
            fechaInicio: document.getElementById("fechaInicio")?.value || "",
            fechaFin: document.getElementById("fechaFin")?.value || "",
            tipoSalida: document.getElementById("tipoSalida")?.value || "TODOS",
            rubro: document.getElementById("rubroFiltro")?.value || "TODOS"
        };
    }

    async function cargarSocios() {
        const sociosBody = document.getElementById("sociosTableBody");

        if (sociosBody) {
            sociosBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-4 text-muted">
                        <span class="spinner-border spinner-border-sm me-2" role="status"></span> Cargando socios...
                    </td>
                </tr>
            `;
        }

        try {
            const response = await fetch("/api/socios", {
                headers: getAuthHeaders()
            });
            const socios = await parseApiResponse(response);

            if (!response.ok) {
                throw new Error(socios.error || "No se pudo obtener el listado de socios.");
            }

            sociosCache = Array.isArray(socios) ? socios : [];

            if (sociosBody) {
                sociosBody.innerHTML = "";

                if (sociosCache.length === 0) {
                    sociosBody.innerHTML = `
                        <tr>
                            <td colspan="5" class="text-center py-4 text-muted">
                                <i class="bi bi-info-circle fs-4 d-block mb-2"></i> No hay socios registrados.
                            </td>
                        </tr>
                    `;
                } else {
                    sociosCache.forEach((socio) => {
                        const tr = document.createElement("tr");
                        tr.innerHTML = `
                            <td>
                                <div class="fw-semibold text-navy">${socio.nombre}</div>
                                <div class="small text-muted">${socio.telefono || "Sin telefono"} · ${socio.correo || "Sin correo"}</div>
                                <div class="small text-muted">${socio.porcentajeParticipacion != null ? `${socio.porcentajeParticipacion}% participacion` : "Participacion sin definir"}</div>
                                <span class="badge ${socio.activo ? "bg-success" : "bg-secondary"} mt-2">${socio.activo ? "ACTIVO" : "INACTIVO"}</span>
                            </td>
                            <td class="fw-bold text-success money-cell">${formatCOP(socio.totalInvertido || 0)}</td>
                            <td class="fw-bold text-success money-cell">${formatCOP(socio.totalRecibido || 0)}</td>
                            <td class="fw-bold ${(socio.saldoPorRecuperar || 0) > 0 ? "text-danger" : "text-success"} money-cell">${formatCOP(socio.saldoPorRecuperar || 0)}</td>
                            <td class="text-end acciones-cell">
                                <div class="btn-group btn-group-sm" role="group" aria-label="Acciones del socio">
                                    <button type="button" class="btn btn-outline-primary btn-editar-socio" data-id="${socio.id}" title="Editar socio">
                                        <i class="bi bi-pencil-square"></i> Editar
                                    </button>
                                    ${socio.activo
                                        ? `<button type="button" class="btn btn-outline-danger btn-eliminar-socio" data-id="${socio.id}" data-nombre="${socio.nombre}" title="Eliminar socio"><i class="bi bi-trash"></i> Eliminar</button>`
                                        : `<button type="button" class="btn btn-outline-success btn-reactivar-socio" data-id="${socio.id}" title="Reactivar socio"><i class="bi bi-arrow-clockwise"></i> Reactivar</button>`
                                    }
                                </div>
                            </td>
                        `;
                        sociosBody.appendChild(tr);
                    });
                    registrarEventosSocios();
                }
            }

            renderizarFormularioReparto();
            renderizarSelectorAportes();
        } catch (error) {
            console.error(error);
            if (sociosBody) {
                sociosBody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center py-4 text-danger">
                            <i class="bi bi-wifi-off fs-4 d-block mb-2"></i> ${error.message || "Error al cargar socios."}
                        </td>
                    </tr>
                `;
            }
            if (error.message && /sesión|Authorization|Bearer|autenticado|permisos/i.test(error.message)) {
                manejarSesionInvalida(error.message);
            }
        }
    }

    function renderizarFormularioReparto() {
        const container = document.getElementById("repartoSociosContainer");
        if (!container) {
            return;
        }

        const sociosActivos = sociosCache.filter((socio) => socio.activo);
        if (sociosActivos.length === 0) {
            container.innerHTML = `
                <div class="alert alert-light border text-muted mb-0">
                    Registre al menos un socio activo para hacer repartos.
                </div>
            `;
            return;
        }

        container.innerHTML = sociosActivos.map((socio) => `
            <div class="border rounded-3 p-3 bg-light">
                <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                    <div>
                        <div class="fw-bold text-navy">${socio.nombre}</div>
                        <div class="small text-muted">${socio.porcentajeParticipacion != null ? `${socio.porcentajeParticipacion}% participacion` : "Participacion sin definir"}</div>
                    </div>
                    <div class="small text-success fw-semibold">Recibido: ${formatCOP(socio.totalRecibido || 0)}</div>
                </div>
                <input type="number" class="form-control reparto-monto" data-socio-id="${socio.id}" data-socio-nombre="${socio.nombre}" placeholder="Monto a entregar a ${socio.nombre}">
            </div>
        `).join("");
    }

    function renderizarSelectorAportes() {
        const select = document.getElementById("aporteSocio");
        if (!select) {
            return;
        }

        const sociosActivos = sociosCache.filter((socio) => socio.activo);
        select.innerHTML = `<option value="">Seleccione un socio...</option>`;
        sociosActivos.forEach((socio) => {
            const option = document.createElement("option");
            option.value = socio.id;
            option.textContent = socio.nombre;
            select.appendChild(option);
        });
    }

    function limpiarFormularioSocio() {
        const socioForm = document.getElementById("socioForm");
        if (socioForm) {
            socioForm.reset();
        }
        document.getElementById("socioId").value = "";
        document.getElementById("socioActivo").checked = true;
        document.getElementById("socioActivoGroup").classList.add("d-none");
        document.getElementById("socioModalLabel").innerHTML = `<i class="bi bi-person-plus-fill"></i> Registrar Socio del Proyecto`;
        document.getElementById("btnConfirmarSocio").innerHTML = `<span class="spinner-border spinner-border-sm me-2 d-none" id="socioSpinner" role="status" aria-hidden="true"></span> Guardar Socio`;
        document.getElementById("socioAlert").classList.add("d-none");
    }

    function abrirEdicionSocio(socio) {
        document.getElementById("socioId").value = socio.id;
        document.getElementById("socioNombre").value = socio.nombre || "";
        document.getElementById("socioTelefono").value = socio.telefono || "";
        document.getElementById("socioCorreo").value = socio.correo || "";
        document.getElementById("socioParticipacion").value = socio.porcentajeParticipacion ?? "";
        document.getElementById("socioObservaciones").value = socio.observaciones || "";
        document.getElementById("socioActivo").checked = !!socio.activo;
        document.getElementById("socioActivoGroup").classList.remove("d-none");
        document.getElementById("socioModalLabel").innerHTML = `<i class="bi bi-pencil-square"></i> Editar Socio`;
        document.getElementById("btnConfirmarSocio").innerHTML = `<span class="spinner-border spinner-border-sm me-2 d-none" id="socioSpinner" role="status" aria-hidden="true"></span> Guardar Cambios`;
        document.getElementById("socioAlert").classList.add("d-none");
        if (socioModal) socioModal.show();
    }

    function registrarEventosSocios() {
        document.querySelectorAll(".btn-editar-socio").forEach((btn) => {
            btn.addEventListener("click", () => {
                const socio = sociosCache.find((item) => item.id === btn.dataset.id);
                if (socio) {
                    abrirEdicionSocio(socio);
                }
            });
        });

        document.querySelectorAll(".btn-eliminar-socio").forEach((btn) => {
            btn.addEventListener("click", async () => {
                if (!confirm(`¿Eliminar a ${btn.dataset.nombre}? Sus aportes y repartos históricos se conservan.`)) {
                    return;
                }
                await cambiarEstadoSocio(btn.dataset.id, false);
            });
        });

        document.querySelectorAll(".btn-reactivar-socio").forEach((btn) => {
            btn.addEventListener("click", async () => {
                await cambiarEstadoSocio(btn.dataset.id, true);
            });
        });
    }

    async function cambiarEstadoSocio(socioId, activo) {
        const socio = sociosCache.find((item) => item.id === socioId);
        if (!socio) {
            return;
        }

        try {
            const response = await fetch(`/api/socios/${socioId}`, {
                method: activo ? "PUT" : "DELETE",
                headers: getAuthHeaders({
                    "Content-Type": "application/json"
                }),
                body: activo ? JSON.stringify({
                    nombre: socio.nombre,
                    telefono: socio.telefono,
                    correo: socio.correo,
                    porcentajeParticipacion: socio.porcentajeParticipacion,
                    observaciones: socio.observaciones,
                    activo: true
                }) : undefined
            });
            const data = await parseApiResponse(response);
            if (!response.ok) {
                throw new Error(data.error || "No se pudo cambiar el estado del socio.");
            }
            await cargarSocios();
        } catch (error) {
            console.error(error);
            alert(error.message || "Error al cambiar el estado del socio.");
        }
    }

    // 3. Cargar listado de egresos
    async function cargarEgresos(fechaInicio = "", fechaFin = "", tipoSalida = "TODOS", rubro = "TODOS") {
        const tableBody = document.getElementById("egresosTableBody");
        const resumenBody = document.getElementById("resumenRubrosTableBody");
        const ingresosBody = document.getElementById("ingresosTableBody");
        if (!tableBody) return;

        tableBody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-4 text-muted">
                    <span class="spinner-border spinner-border-sm me-2" role="status"></span> Consultando egresos en Supabase...
                </td>
            </tr>
        `;
        if (resumenBody) {
            resumenBody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center py-4 text-muted">
                        <span class="spinner-border spinner-border-sm me-2" role="status"></span> Calculando consolidado por rubro...
                    </td>
                </tr>
            `;
        }
        if (ingresosBody) {
            ingresosBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-4 text-muted">
                        <span class="spinner-border spinner-border-sm me-2" role="status"></span> Cargando ingresos...
                    </td>
                </tr>
            `;
        }

        try {
            const params = new URLSearchParams();
            if (fechaInicio) params.append("fechaInicio", fechaInicio);
            if (fechaFin) params.append("fechaFin", fechaFin);
            if (tipoSalida && tipoSalida !== "TODOS") params.append("tipoSalida", tipoSalida);
            if (rubro && rubro !== "TODOS") params.append("rubro", rubro);

            const queryString = params.toString();
            const [responseHistorial, responseResumen, responseIngresos, responseIngresosResumen] = await Promise.all([
                fetch(`/api/egresos/liquidar?${queryString}`, {
                    headers: getAuthHeaders()
                }),
                fetch(`/api/egresos/resumen?${queryString}`, {
                    headers: getAuthHeaders()
                }),
                fetch(`/api/ingresos?${queryString}`, {
                    headers: getAuthHeaders()
                }),
                fetch(`/api/ingresos/resumen?${queryString}`, {
                    headers: getAuthHeaders()
                })
            ]);

            const egresos = await parseApiResponse(responseHistorial);
            const resumen = await parseApiResponse(responseResumen);
            const ingresos = await parseApiResponse(responseIngresos);
            const ingresosResumen = await parseApiResponse(responseIngresosResumen);

            if (!responseHistorial.ok) {
                throw new Error(egresos.error || "No se pudo obtener la liquidación de egresos.");
            }
            if (!responseResumen.ok) {
                throw new Error(resumen.error || "No se pudo obtener el resumen por rubro.");
            }
            if (!responseIngresos.ok) {
                throw new Error(ingresos.error || "No se pudo obtener el registro general de ingresos.");
            }
            if (!responseIngresosResumen.ok) {
                throw new Error(ingresosResumen.error || "No se pudo obtener el resumen de ingresos.");
            }

            const isAdmin = user && user.rol === "ADMINISTRADOR";
            const accionesHeader = document.getElementById("accionesHeader");
            if (accionesHeader) {
                accionesHeader.style.display = isAdmin ? "table-cell" : "none";
            }

            tableBody.innerHTML = "";
            const colspanVal = isAdmin ? 9 : 8;

            if (egresos.length === 0) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="${colspanVal}" class="text-center py-4 text-muted">
                            <i class="bi bi-info-circle fs-4 d-block mb-2"></i> No se encontraron salidas de caja en el filtro seleccionado.
                        </td>
                    </tr>
                `;
            } else {
                egresos.forEach(eg => {
                    const tr = document.createElement("tr");
                    tr.className = "animate__animated animate__fadeIn";
                    
                    let deleteBtnCell = "";
                    if (isAdmin) {
                        const isOperativo = eg.tipoSalida === "OPERATIVO";
                        deleteBtnCell = isOperativo 
                            ? `<td><button class="btn btn-sm btn-danger btn-eliminar-egreso" data-id="${eg.id}" title="Eliminar egreso operativo"><i class="bi bi-trash"></i></button></td>`
                            : `<td>-</td>`;
                    }

                    tr.innerHTML = `
                        <td class="fw-bold text-navy">${eg.fecha}</td>
                        <td><span class="badge ${eg.tipoSalida === 'DISTRIBUCION_SOCIOS' ? 'bg-success' : 'bg-primary'} text-white text-uppercase" style="font-size: 0.75rem;">${eg.tipoSalida}</span></td>
                        <td><span class="badge bg-navy text-white text-uppercase" style="font-size: 0.75rem;">${eg.rubro}</span></td>
                        <td class="small text-navy">${eg.beneficiario || "-"}</td>
                        <td class="small text-muted">${eg.referencia || "-"}</td>
                        <td class="text-muted small">${eg.descripcion || 'Sin descripción'}</td>
                        <td class="fw-bold text-danger">${formatCOP(eg.monto)}</td>
                        <td class="small text-navy"><i class="bi bi-person-circle"></i> ${eg.registradoPor || 'Sistema'}</td>
                        ${deleteBtnCell}
                    `;
                    tableBody.appendChild(tr);
                });

                // Registrar eventos de eliminación
                document.querySelectorAll(".btn-eliminar-egreso").forEach(btn => {
                    btn.addEventListener("click", async (e) => {
                        const egresoId = btn.getAttribute("data-id");
                        if (confirm("¿Está seguro de que desea eliminar este egreso operativo? Esta acción es irreversible, se registrará en la bitácora de auditoría y afectará la utilidad neta de inmediato.")) {
                            try {
                                const response = await fetch(`/api/egresos/${egresoId}`, {
                                    method: "DELETE",
                                    headers: getAuthHeaders()
                                });
                                const resData = await response.json();
                                if (response.ok) {
                                    alert("Egreso operativo eliminado con éxito.");
                                    // Recargar lista
                                    const finicio = document.getElementById("filtroFechaInicio") ? document.getElementById("filtroFechaInicio").value : "";
                                    const ffin = document.getElementById("filtroFechaFin") ? document.getElementById("filtroFechaFin").value : "";
                                    const tsalida = document.getElementById("filtroTipoSalida") ? document.getElementById("filtroTipoSalida").value : "TODOS";
                                    const frubro = document.getElementById("filtroRubro") ? document.getElementById("filtroRubro").value : "TODOS";
                                    cargarEgresos(finicio, ffin, tsalida, frubro);
                                } else {
                                    alert(resData.error || "No se pudo eliminar el egreso.");
                                }
                            } catch (err) {
                                console.error(err);
                                alert("Error al conectar con el servidor para eliminar el egreso.");
                            }
                        }
                    });
                });
            }

            if (resumenBody) {
                resumenBody.innerHTML = "";
                if (!resumen.rubros || resumen.rubros.length === 0) {
                    resumenBody.innerHTML = `
                        <tr>
                            <td colspan="3" class="text-center py-4 text-muted">
                                <i class="bi bi-info-circle fs-4 d-block mb-2"></i> No hay egresos para consolidar en el rango seleccionado.
                            </td>
                        </tr>
                    `;
                } else {
                    resumen.rubros.forEach(item => {
                        const tr = document.createElement("tr");
                        tr.className = "animate__animated animate__fadeIn";
                        tr.innerHTML = `
                            <td><span class="badge bg-navy text-white text-uppercase" style="font-size: 0.8rem;">${item.rubro}</span></td>
                            <td class="fw-semibold text-navy">${item.cantidad}</td>
                            <td class="fw-bold text-danger">${formatCOP(item.total)}</td>
                        `;
                        resumenBody.appendChild(tr);
                    });
                }
            }

            if (ingresosBody) {
                ingresosBody.innerHTML = "";
                if (!ingresos || ingresos.length === 0) {
                    ingresosBody.innerHTML = `
                        <tr>
                            <td colspan="6" class="text-center py-4 text-muted">
                                <i class="bi bi-info-circle fs-4 d-block mb-2"></i> No hay ingresos en el rango seleccionado.
                            </td>
                        </tr>
                    `;
                } else {
                    ingresos.forEach(item => {
                        const tr = document.createElement("tr");
                        tr.className = "animate__animated animate__fadeIn";
                        tr.innerHTML = `
                            <td class="fw-bold text-navy">${item.fecha}</td>
                            <td><span class="badge ${item.origen === 'INVERSIONISTA' ? 'bg-success' : 'bg-primary'} text-white text-uppercase" style="font-size: 0.75rem;">${item.origen}</span></td>
                            <td class="small fw-semibold text-navy">${item.tipo}</td>
                            <td class="small text-navy">${item.referencia}</td>
                            <td class="text-muted small">${item.detalle}</td>
                            <td class="fw-bold text-success">${formatCOP(item.monto)}</td>
                        `;
                        ingresosBody.appendChild(tr);
                    });
                }
            }

            document.getElementById("totalEgresosLabel").textContent = formatCOP(resumen.totalEgresos || 0);
            document.getElementById("cantidadEgresosLabel").textContent = `${resumen.cantidadEgresos || 0} registro${(resumen.cantidadEgresos || 0) !== 1 ? 's' : ''}`;
            document.getElementById("cantidadRubrosLabel").textContent = `${resumen.cantidadRubros || 0} rubro${(resumen.cantidadRubros || 0) !== 1 ? 's' : ''}`;
            document.getElementById("rubroMayorLabel").textContent = resumen.rubroMayor ? resumen.rubroMayor.rubro : "Sin datos";
            document.getElementById("rubroMayorMontoLabel").textContent = formatCOP(resumen.rubroMayor ? resumen.rubroMayor.total : 0);
            document.getElementById("totalIngresosLabel").textContent = formatCOP(ingresosResumen.totalIngresos || 0);
            document.getElementById("totalVentasIngresosLabel").textContent = formatCOP(ingresosResumen.totalVentas || 0);
            document.getElementById("totalInversionistasLabel").textContent = formatCOP(ingresosResumen.totalInversionistas || 0);
            document.getElementById("saldoCajaLabel").textContent = formatCOP(ingresosResumen.saldoCaja || 0);
            document.getElementById("totalDistribucionesSociosLabel").textContent = formatCOP(resumen.totalDistribucionesSocios || 0);

        } catch (error) {
            console.error(error);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-4 text-danger">
                        <i class="bi bi-wifi-off fs-4 d-block mb-2"></i> ${error.message || "Error al cargar egresos."}
                    </td>
                </tr>
            `;
            if (resumenBody) {
                resumenBody.innerHTML = `
                    <tr>
                        <td colspan="3" class="text-center py-4 text-danger">
                            <i class="bi bi-wifi-off fs-4 d-block mb-2"></i> Error al calcular el consolidado por rubro.
                        </td>
                    </tr>
                `;
            }
            if (ingresosBody) {
                ingresosBody.innerHTML = `
                    <tr>
                        <td colspan="6" class="text-center py-4 text-danger">
                            <i class="bi bi-wifi-off fs-4 d-block mb-2"></i> ${error.message || 'Error al cargar ingresos.'}
                        </td>
                    </tr>
                `;
            }
            document.getElementById("totalEgresosLabel").textContent = formatCOP(0);
            document.getElementById("cantidadEgresosLabel").textContent = "0 registros";
            document.getElementById("cantidadRubrosLabel").textContent = "0 rubros";
            document.getElementById("rubroMayorLabel").textContent = "Sin datos";
            document.getElementById("rubroMayorMontoLabel").textContent = formatCOP(0);
            document.getElementById("totalIngresosLabel").textContent = formatCOP(0);
            document.getElementById("totalVentasIngresosLabel").textContent = formatCOP(0);
            document.getElementById("totalInversionistasLabel").textContent = formatCOP(0);
            document.getElementById("saldoCajaLabel").textContent = formatCOP(0);
            document.getElementById("totalDistribucionesSociosLabel").textContent = formatCOP(0);
            if (error.message && /sesión|Authorization|Bearer|autenticado|permisos/i.test(error.message)) {
                manejarSesionInvalida(error.message);
            }
        }
    }

    // Cargar inicialmente sin filtros
    cargarEgresos();
    cargarSocios();

    // 4. Formulario de Filtros / Liquidar
    const filtroEgresosForm = document.getElementById("filtroEgresosForm");
    if (filtroEgresosForm) {
        filtroEgresosForm.addEventListener("submit", (e) => {
            e.preventDefault();
            const start = document.getElementById("fechaInicio").value;
            const end = document.getElementById("fechaFin").value;
            const tipoSalida = document.getElementById("tipoSalida").value;
            const rubro = document.getElementById("rubroFiltro").value;
            
            if (start && end && start > end) {
                alert("La fecha de inicio no puede ser posterior a la fecha de fin.");
                return;
            }
            cargarEgresos(start, end, tipoSalida, rubro);
        });
    }

    const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");
    if (btnLimpiarFiltros) {
        btnLimpiarFiltros.addEventListener("click", () => {
            document.getElementById("fechaInicio").value = "";
            document.getElementById("fechaFin").value = "";
            document.getElementById("tipoSalida").value = "TODOS";
            document.getElementById("rubroFiltro").value = "TODOS";
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
                    headers: getAuthHeaders({
                        "Content-Type": "application/json"
                    }),
                    body: JSON.stringify({
                        monto: parseFloat(monto),
                        fechaEgreso: fecha,
                        rubro: rubro,
                        descripcion: descripcion
                    })
                });

                const resData = await parseApiResponse(response);

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
                    const filtros = obtenerFiltrosActuales();
                    cargarEgresos(filtros.fechaInicio, filtros.fechaFin, filtros.tipoSalida, filtros.rubro);
                } else {
                    if (response.status === 400 && /sesión|Authorization|Bearer|autenticado|permisos/i.test(resData.error || "")) {
                        manejarSesionInvalida(resData.error);
                        return;
                    }
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

    const aporteForm = document.getElementById("aporteForm");
    if (aporteForm) {
        const aporteSocioSelect = document.getElementById("aporteSocio");
        if (aporteSocioSelect) {
            aporteSocioSelect.addEventListener("change", () => {
                const socio = sociosCache.find((item) => item.id === aporteSocioSelect.value);
                const nombreInput = document.getElementById("aporteNombre");
                if (!nombreInput) {
                    return;
                }
                if (socio) {
                    nombreInput.value = socio.nombre;
                    nombreInput.readOnly = true;
                } else {
                    nombreInput.value = "";
                    nombreInput.readOnly = true;
                }
            });
        }

        aporteForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const socioId = document.getElementById("aporteSocio")?.value || "";
            const nombre = document.getElementById("aporteNombre").value.trim();
            const monto = document.getElementById("aporteMonto").value;
            const fecha = document.getElementById("aporteFecha").value;
            const descripcion = document.getElementById("aporteDescripcion").value;
            const spinner = document.getElementById("aporteSpinner");
            const confirmBtn = document.getElementById("btnConfirmarAporte");
            const alertDiv = document.getElementById("aporteAlert");
            const alertText = document.getElementById("aporteAlertText");

            if (!socioId || !nombre || !monto || !fecha) {
                alertText.textContent = "Seleccione un socio e ingrese monto y fecha del aporte.";
                alertDiv.classList.remove("d-none");
                return;
            }

            spinner.classList.remove("d-none");
            confirmBtn.disabled = true;
            alertDiv.classList.add("d-none");

            try {
                const response = await fetch("/api/ingresos/inversionistas", {
                    method: "POST",
                    headers: getAuthHeaders({
                        "Content-Type": "application/json"
                    }),
                    body: JSON.stringify({
                        socioId,
                        nombreInversionista: nombre,
                        monto: parseFloat(monto),
                        fechaAporte: fecha,
                        descripcion: descripcion
                    })
                });

                const data = await parseApiResponse(response);
                if (response.ok) {
                    if (aporteModal) aporteModal.hide();
                    aporteForm.reset();
                    const nombreInput = document.getElementById("aporteNombre");
                    if (nombreInput) {
                        nombreInput.readOnly = false;
                    }
                    if (aporteFechaInput) {
                        aporteFechaInput.value = new Date().toISOString().split('T')[0];
                    }
                    alert("Aporte de inversionista registrado correctamente.");
                    await cargarSocios();
                    const filtros = obtenerFiltrosActuales();
                    cargarEgresos(filtros.fechaInicio, filtros.fechaFin, filtros.tipoSalida, filtros.rubro);
                } else {
                    if (response.status === 400 && /sesión|Authorization|Bearer|autenticado|permisos/i.test(data.error || "")) {
                        manejarSesionInvalida(data.error);
                        return;
                    }
                    alertText.textContent = data.error || "No se pudo registrar el aporte.";
                    alertDiv.classList.remove("d-none");
                }
            } catch (err) {
                console.error(err);
                alertText.textContent = "Error de red al registrar el aporte.";
                alertDiv.classList.remove("d-none");
            } finally {
                confirmBtn.disabled = false;
                spinner.classList.add("d-none");
            }
        });
    }

    const socioForm = document.getElementById("socioForm");
    const btnNuevoSocio = document.getElementById("btnNuevoSocio");
    if (btnNuevoSocio) {
        btnNuevoSocio.addEventListener("click", limpiarFormularioSocio);
    }

    if (socioForm) {
        socioForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const socioId = document.getElementById("socioId").value;
            const nombre = document.getElementById("socioNombre").value.trim();
            const telefono = document.getElementById("socioTelefono").value.trim();
            const correo = document.getElementById("socioCorreo").value.trim();
            const porcentajeParticipacion = document.getElementById("socioParticipacion").value;
            const observaciones = document.getElementById("socioObservaciones").value.trim();
            const activo = document.getElementById("socioActivo").checked;
            const spinner = document.getElementById("socioSpinner");
            const confirmBtn = document.getElementById("btnConfirmarSocio");
            const alertDiv = document.getElementById("socioAlert");
            const alertText = document.getElementById("socioAlertText");

            if (!nombre) {
                alertText.textContent = "El nombre del socio es obligatorio.";
                alertDiv.classList.remove("d-none");
                return;
            }

            spinner.classList.remove("d-none");
            confirmBtn.disabled = true;
            alertDiv.classList.add("d-none");

            try {
                const response = await fetch(socioId ? `/api/socios/${socioId}` : "/api/socios", {
                    method: socioId ? "PUT" : "POST",
                    headers: getAuthHeaders({
                        "Content-Type": "application/json"
                    }),
                    body: JSON.stringify({
                        nombre,
                        telefono,
                        correo,
                        porcentajeParticipacion: porcentajeParticipacion ? parseFloat(porcentajeParticipacion) : null,
                        observaciones,
                        activo
                    })
                });

                const data = await parseApiResponse(response);
                if (response.ok) {
                    if (socioModal) socioModal.hide();
                    limpiarFormularioSocio();
                    await cargarSocios();
                } else {
                    if (response.status === 400 && /sesión|Authorization|Bearer|autenticado|permisos/i.test(data.error || "")) {
                        manejarSesionInvalida(data.error);
                        return;
                    }
                    alertText.textContent = data.error || "No se pudo registrar el socio.";
                    alertDiv.classList.remove("d-none");
                }
            } catch (error) {
                console.error(error);
                alertText.textContent = "Error de red al registrar el socio.";
                alertDiv.classList.remove("d-none");
            } finally {
                confirmBtn.disabled = false;
                spinner.classList.add("d-none");
            }
        });
    }

    const repartoForm = document.getElementById("repartoForm");
    if (repartoForm) {
        repartoForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const fechaDistribucion = document.getElementById("repartoFecha").value;
            const referencia = document.getElementById("repartoReferencia").value.trim();
            const descripcion = document.getElementById("repartoDescripcion").value.trim();
            const spinner = document.getElementById("repartoSpinner");
            const confirmBtn = document.getElementById("btnConfirmarReparto");
            const alertDiv = document.getElementById("repartoAlert");
            const alertText = document.getElementById("repartoAlertText");

            const distribuciones = Array.from(document.querySelectorAll(".reparto-monto"))
                .map((input) => ({
                    socioId: input.dataset.socioId,
                    monto: input.value ? parseFloat(input.value) : 0
                }))
                .filter((item) => item.monto > 0);

            if (!fechaDistribucion) {
                alertText.textContent = "La fecha del reparto es obligatoria.";
                alertDiv.classList.remove("d-none");
                return;
            }
            if (distribuciones.length === 0) {
                alertText.textContent = "Debe ingresar al menos un monto para repartir.";
                alertDiv.classList.remove("d-none");
                return;
            }

            spinner.classList.remove("d-none");
            confirmBtn.disabled = true;
            alertDiv.classList.add("d-none");

            try {
                const response = await fetch("/api/distribuciones-socios", {
                    method: "POST",
                    headers: getAuthHeaders({
                        "Content-Type": "application/json"
                    }),
                    body: JSON.stringify({
                        fechaDistribucion,
                        referencia,
                        descripcion,
                        distribuciones
                    })
                });

                const data = await parseApiResponse(response);
                if (response.ok) {
                    if (repartoModal) repartoModal.hide();
                    repartoForm.reset();
                    if (repartoFechaInput) {
                        repartoFechaInput.value = new Date().toISOString().split('T')[0];
                    }
                    await cargarSocios();
                    const filtros = obtenerFiltrosActuales();
                    cargarEgresos(filtros.fechaInicio, filtros.fechaFin, filtros.tipoSalida, filtros.rubro);
                    alert(`Reparto registrado por ${formatCOP(data.totalDistribuido || 0)}.`);
                } else {
                    if (response.status === 400 && /sesión|Authorization|Bearer|autenticado|permisos/i.test(data.error || "")) {
                        manejarSesionInvalida(data.error);
                        return;
                    }
                    alertText.textContent = data.error || "No se pudo registrar el reparto.";
                    alertDiv.classList.remove("d-none");
                }
            } catch (error) {
                console.error(error);
                alertText.textContent = "Error de red al registrar el reparto.";
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
                        method: "POST",
                        headers: getAuthHeaders()
                    });
                    if (response.ok) {
                        alert("Base de datos restablecida exitosamente.");
                        window.location.reload();
                    } else {
                        const data = await parseApiResponse(response);
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
