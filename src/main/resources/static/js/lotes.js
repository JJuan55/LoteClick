/**
 * LoteClick - JS Controller para Inventario de Lotes y Ficha de Compradores (Módulo 1)
 * Soporta cierre de ventas, amortización, subida de archivos y filtros avanzados.
 */
document.addEventListener("DOMContentLoaded", () => {
    // Componentes del Inventario
    const etapaSelect = document.getElementById("etapaSelect");
    const estadoFilter = document.getElementById("estadoFilter");
    const lotesGrid = document.getElementById("lotesGrid");
    
    // Contadores
    const countDisponible = document.getElementById("countDisponible");
    const countSeparado = document.getElementById("countSeparado");
    const countVendido = document.getElementById("countVendido");

    // Instancia de Offcanvas
    const offcanvasElement = document.getElementById("loteDetailsOffcanvas");
    const offcanvas = new bootstrap.Offcanvas(offcanvasElement);

    // Componentes del Offcanvas
    const detailNumeroLote = document.getElementById("detailNumeroLote");
    const detailStatusBadge = document.getElementById("detailStatusBadge");
    const detailArea = document.getElementById("detailArea");
    const detailPrecioBase = document.getElementById("detailPrecioBase");
    const detailEtapa = document.getElementById("detailEtapa");
    const loteActionContainer = document.getElementById("loteActionContainer");

    // Instancia de Modal (Ficha de Comprador)
    const compradorModalElement = document.getElementById("compradorModal");
    const compradorModal = new bootstrap.Modal(compradorModalElement);

    // Pasos del Modal
    const modalStepComprador = document.getElementById("modalStepComprador");
    const modalStepCredito = document.getElementById("modalStepCredito");
    const btnSiguienteStep = document.getElementById("btnSiguienteStep");
    const btnAtrasStep = document.getElementById("btnAtrasStep");
    const btnConfirmarVenta = document.getElementById("btnConfirmarVenta");

    // Componentes del Modal - Paso 1 (Comprador)
    const modalLoteNum = document.getElementById("modalLoteNum");
    const buscarCedulaInput = document.getElementById("buscarCedula");
    const btnBuscarComprador = document.getElementById("btnBuscarComprador");
    
    const buscarAlert = document.getElementById("buscarAlert");
    const buscarAlertText = document.getElementById("buscarAlertText");
    const alertIcon = document.getElementById("alertIcon");

    const compradorForm = document.getElementById("compradorForm");
    const compradorNombreInput = document.getElementById("compradorNombre");
    const compradorTelefonoInput = document.getElementById("compradorTelefono");
    const compradorCorreoInput = document.getElementById("compradorCorreo");
    const compradorDireccionInput = document.getElementById("compradorDireccion");
    const btnGuardarComprador = document.getElementById("btnGuardarComprador");

    // Componentes del Modal - Paso 2 (Crédito)
    const creditPrecioPactado = document.getElementById("creditPrecioPactado");
    const creditCuotaSeparacion = document.getElementById("creditCuotaSeparacion");
    const creditPlazoMeses = document.getElementById("creditPlazoMeses");
    const creditFechaVenta = document.getElementById("creditFechaVenta");
    const creditDocumentoPropiedad = document.getElementById("creditDocumentoPropiedad");
    const creditoAlert = document.getElementById("creditoAlert");
    const creditoAlertText = document.getElementById("creditoAlertText");
    const creditSpinner = document.getElementById("creditSpinner");
    const creditBtnText = document.getElementById("creditBtnText");

    // Variables de estado del controlador
    let lotesActivos = []; // Caché de lotes de la etapa actual
    let loteSeleccionado = null;
    let compradorSeleccionado = null; // Guardará el comprador activo

    // Cargar lotes al iniciar
    if (etapaSelect) {
        cargarLotes(etapaSelect.value);
        
        // Escuchar cambios de etapa
        etapaSelect.addEventListener("change", (e) => {
            cargarLotes(e.target.value);
        });
    }

    // Escuchar cambios de estado de filtro
    if (estadoFilter) {
        estadoFilter.addEventListener("change", () => {
            renderLotesFiltrados();
        });
    }

    /**
     * Formatea un número como divisa USD
     */
    function formatearPrecio(valor) {
        return new Intl.NumberFormat('es-CO', {
            style: 'currency',
            currency: 'COP',
            minimumFractionDigits: 0
        }).format(valor);
    }

    /**
     * Obtiene los lotes desde la API filtrados por etapa
     */
    async function cargarLotes(etapaId) {
        lotesGrid.innerHTML = `
            <div class="col-12 text-center py-5">
                <div class="spinner-border text-navy" role="status">
                    <span class="visually-hidden">Cargando...</span>
                </div>
                <p class="mt-2 text-muted">Obteniendo plano de lotes...</p>
            </div>
        `;

        try {
            const response = await fetch(`/api/lotes?etapaId=${etapaId}`, {
                headers: getAuthHeaders()
            });
            if (!response.ok) {
                throw new Error("No se pudo obtener el inventario de lotes.");
            }

            lotesActivos = await response.json();
            
            // Actualizar contadores del mapa basados en el total de la etapa
            let disp = 0, sep = 0, vend = 0;
            lotesActivos.forEach(l => {
                if (l.estado === "DISPONIBLE") disp++;
                else if (l.estado === "SEPARADO") sep++;
                else if (l.estado === "VENDIDO") vend++;
            });
            actualizarContadores(disp, sep, vend);

            // Renderizar la cuadrícula aplicando el filtro secundario de estado
            renderLotesFiltrados();

        } catch (error) {
            console.error("Error al cargar lotes:", error);
            lotesGrid.innerHTML = `
                <div class="col-12 alert alert-danger border-danger bg-danger bg-opacity-10 text-danger" role="alert">
                    <h5 class="alert-heading"><i class="bi bi-exclamation-triangle-fill"></i> Error de conexión</h5>
                    <p class="mb-0">No se pudo cargar el mapa del inventario de lotes. Verifique que el servidor esté activo.</p>
                </div>
            `;
            actualizarContadores(0, 0, 0);
        }
    }

    /**
     * Renderiza las tarjetas de lotes en la grilla filtrando según el selector de Estado
     */
    function renderLotesFiltrados() {
        const filterVal = estadoFilter.value;
        lotesGrid.innerHTML = "";

        // Filtrar array en memoria
        const lotesFiltrados = lotesActivos.filter(l => {
            if (filterVal === "TODOS") return true;
            return l.estado === filterVal;
        });

        if (lotesFiltrados.length === 0) {
            lotesGrid.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-funnel text-muted fs-1"></i>
                    <p class="mt-2 text-muted">No se encontraron lotes que coincidan con el estado de filtro.</p>
                </div>
            `;
            return;
        }

        lotesFiltrados.forEach((lote, index) => {
            const col = document.createElement("div");
            col.className = "col-md-3 col-sm-6 animate__animated animate__fadeInUp";
            col.style.animationDelay = `${Math.min(index * 0.04, 1.0)}s`;

            let badgeColor = "bg-success";
            let statusText = "Disponible";
            if (lote.estado === "SEPARADO") {
                badgeColor = "bg-warning text-dark";
                statusText = "Separado";
            } else if (lote.estado === "VENDIDO") {
                badgeColor = "bg-danger";
                statusText = "Vendido";
            }

            col.innerHTML = `
                <div class="card lote-card status-${lote.estado.toLowerCase()} h-100 p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="lote-card-title">Lote ${lote.numeroLote}</span>
                        <span class="badge lote-badge ${badgeColor}">${statusText}</span>
                    </div>
                    <div class="lote-card-metric"><i class="bi bi-aspect-ratio text-success"></i> <strong>${lote.areaM2}</strong> m²</div>
                    <div class="lote-card-metric"><i class="bi bi-cash-stack text-success"></i> <strong>${formatearPrecio(lote.precioBase)}</strong></div>
                </div>
            `;

            // Clic abre el offcanvas
            col.querySelector(".lote-card").addEventListener("click", () => {
                mostrarDetallesLote(lote);
            });

            lotesGrid.appendChild(col);
        });
    }

    /**
     * Actualiza la sección de contadores superiores
     */
    function actualizarContadores(disp, sep, vend) {
        countDisponible.textContent = disp;
        countSeparado.textContent = sep;
        countVendido.textContent = vend;
    }

    /**
     * Llena el Offcanvas con los detalles del lote y abre el panel
     */
    function mostrarDetallesLote(lote) {
        loteSeleccionado = lote;
        detailNumeroLote.textContent = `Lote ${lote.numeroLote}`;
        detailArea.textContent = `${lote.areaM2.toFixed(2)} m²`;
        detailPrecioBase.textContent = `${formatearPrecio(lote.precioBase)}`;
        detailEtapa.textContent = lote.etapa ? lote.etapa.nombreEtapa : "Etapa N/A";

        // Cargar imagen dinámica basada en la etapa o la imagen personalizada del lote
        const detailLoteImage = document.getElementById("detailLoteImage");
        if (detailLoteImage) {
            let imgUrl = lote.urlImagen;
            if (!imgUrl) {
                imgUrl = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=600&q=80"; // Etapa 1
                const etapaNombre = lote.etapa ? lote.etapa.nombreEtapa : "";
                if (etapaNombre.includes("2")) {
                    imgUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=600&q=80"; // Etapa 2
                } else if (etapaNombre.includes("3")) {
                    imgUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?auto=format&fit=crop&w=600&q=80"; // Etapa 3
                } else if (etapaNombre.includes("4")) {
                    imgUrl = "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=600&q=80"; // Etapa 4
                }
            }
            detailLoteImage.src = imgUrl;
        }

        // Cargar descripción dinámica o personalizada
        const detailDescripcion = document.getElementById("detailDescripcion");
        if (detailDescripcion) {
            let desc = lote.descripcion;
            if (!desc) {
                desc = "Lote plano con excelente topografía, acceso directo a vías internas de la parcelación, disponibilidad para conexión de servicios públicos domiciliarios (agua y luz) y una espectacular vista panorámica al valle y Mirador.";
                const etapaNombre = lote.etapa ? lote.etapa.nombreEtapa : "";
                if (etapaNombre.includes("2")) {
                    desc = "Espectacular lote campestre con topografía de loma suave, ideal para construir casa vacacional. Cuenta con árboles nativos frutales, senderos ecológicos y acceso a zonas comunes de la parcelación.";
                } else if (etapaNombre.includes("3")) {
                    desc = "Exclusivo lote de campo rodeado de bosque nativo, perfecto para los amantes del silencio y el avistamiento de aves. Clima templado, linderos naturales definidos y reserva de agua propia.";
                } else if (etapaNombre.includes("4")) {
                    desc = "Lote con vista panorámica premium hacia la cordillera y la represa. Ubicado en la parte alta de la parcelación, cuenta con portería de seguridad, vías pavimentadas de acceso y todos los servicios básicos.";
                }
            }
            detailDescripcion.textContent = desc;
        }

        // Estilizar el badge del Offcanvas
        detailStatusBadge.className = "badge text-uppercase mb-2";
        if (lote.estado === "DISPONIBLE") {
            detailStatusBadge.classList.add("bg-success");
            detailStatusBadge.textContent = "Disponible";
            
            loteActionContainer.innerHTML = `
                <button class="btn btn-success p-3 rounded-3 fw-bold shadow-sm" id="btnIniciarProcesoVenta">
                    <i class="bi bi-cart-plus-fill"></i> Iniciar Proceso de Venta
                </button>
            `;
            
            document.getElementById("btnIniciarProcesoVenta").addEventListener("click", () => {
                offcanvas.hide();
                abrirFichaComprador(lote);
            });

        } else if (lote.estado === "SEPARADO") {
            detailStatusBadge.classList.add("bg-warning", "text-dark");
            detailStatusBadge.textContent = "Separado en Amortización";
            
            loteActionContainer.innerHTML = `
                <div class="alert alert-warning border-warning bg-warning bg-opacity-10 text-dark small" role="alert">
                    <i class="bi bi-info-circle-fill"></i> Este lote ya cuenta con un abono inicial y se encuentra en estado de amortización por cuotas.
                </div>
                <button class="btn btn-navy p-2.5 rounded-3 fw-semibold text-white" id="btnVerCartera">
                    <i class="bi bi-person-card-heading"></i> Ver Estado de Cuenta (Cartera)
                </button>
            `;

            document.getElementById("btnVerCartera").addEventListener("click", () => {
                // Redirigir a cartera buscando el lote
                offcanvas.hide();
                // Consultamos el contrato para sacar la cédula y redirigir
                redirigirACarteraPorLote(lote.id);
            });

        } else {
            detailStatusBadge.classList.add("bg-danger");
            detailStatusBadge.textContent = "Vendido / Pagado Total";
            
            loteActionContainer.innerHTML = `
                <div class="alert alert-danger border-danger bg-danger bg-opacity-10 text-danger small" role="alert">
                    <i class="bi bi-lock-fill"></i> Este lote ha sido totalmente pagado y liquidado. La escritura está asignada.
                </div>
            `;
        }

        offcanvas.show();
    }

    /**
     * Redirige a cartera buscando el lote
     */
    async function redirigirACarteraPorLote(loteId) {
        try {
            // Buscamos el contrato de venta para este lote en backend, pero como no hay endpoint directo
            // de búsqueda por lote, consultamos todos los de la cartera o tiramos directo a cartera.html
            // Para simplificar, abrimos cartera.html directamente
            window.location.href = "cartera.html";
        } catch (e) {
            window.location.href = "cartera.html";
        }
    }

    /**
     * Prepara y abre el Modal de la Ficha de Comprador
     */
    function abrirFichaComprador(lote) {
        loteSeleccionado = lote;
        compradorSeleccionado = null;
        modalLoteNum.textContent = lote.numeroLote;
        
        // Resetear vistas de pasos
        modalStepComprador.classList.remove("d-none");
        modalStepCredito.classList.add("d-none");
        
        // Resetear botones paso 1
        btnGuardarComprador.classList.remove("d-none");
        btnSiguienteStep.classList.add("d-none");
        
        // Resetear inputs del paso 2 (Valores realistas en COP con base en el precio del lote)
        const precioBase = Math.round(lote.precioBase);
        const separacionMin = Math.round(precioBase * 0.1);
        creditPrecioPactado.value = precioBase;
        creditCuotaSeparacion.value = separacionMin;
        creditCuotaSeparacion.min = separacionMin;
        creditCuotaSeparacion.step = 100000;
        creditPlazoMeses.value = "";
        creditDocumentoPropiedad.value = "";
        
        // Pre-cargar fecha actual en el input date
        const hoy = new Date().toISOString().split('T')[0];
        creditFechaVenta.value = hoy;
        
        // Ocultar alertas
        creditoAlert.classList.add("d-none");
        creditoAlertText.textContent = "";

        // Resetear buscador e inputs
        buscarCedulaInput.value = "";
        compradorForm.reset();
        toggleFormInputs(false);
        
        buscarAlert.className = "alert py-2 d-none";
        buscarAlertText.textContent = "";

        // Abrir modal
        compradorModal.show();
    }

    /**
     * Habilita o deshabilita los inputs del formulario de comprador
     */
    function toggleFormInputs(enabled) {
        compradorNombreInput.disabled = !enabled;
        compradorTelefonoInput.disabled = !enabled;
        compradorCorreoInput.disabled = !enabled;
        compradorDireccionInput.disabled = !enabled;
        btnGuardarComprador.disabled = !enabled;
        
        if (enabled) {
            compradorNombreInput.classList.remove("bg-light");
            compradorTelefonoInput.classList.remove("bg-light");
            compradorCorreoInput.classList.remove("bg-light");
            compradorDireccionInput.classList.remove("bg-light");
        } else {
            compradorNombreInput.classList.add("bg-light");
            compradorTelefonoInput.classList.add("bg-light");
            compradorCorreoInput.classList.add("bg-light");
            compradorDireccionInput.classList.add("bg-light");
        }
    }

    /**
     * Muestra alertas informativas dentro del Modal de Comprador
     */
    function mostrarAlertaModal(tipo, mensaje) {
        buscarAlert.className = `alert py-2 d-flex align-items-center gap-2 alert-${tipo}`;
        buscarAlertText.textContent = mensaje;
        
        if (tipo === "success") {
            alertIcon.className = "bi bi-check-circle-fill fs-5";
        } else if (tipo === "warning") {
            alertIcon.className = "bi bi-exclamation-triangle-fill fs-5";
        } else {
            alertIcon.className = "bi bi-info-circle-fill fs-5";
        }
        
        buscarAlert.classList.remove("d-none");
    }

    // Buscar comprador por cédula
    btnBuscarComprador.addEventListener("click", async () => {
        const cedula = buscarCedulaInput.value.trim();
        if (!cedula) {
            mostrarAlertaModal("warning", "Por favor ingrese un número de cédula válido.");
            return;
        }

        buscarAlert.className = "alert py-2 text-center text-muted bg-light d-flex align-items-center justify-content-center gap-2";
        buscarAlertText.innerHTML = `<span class="spinner-border spinner-border-sm" role="status"></span> Buscando en el sistema...`;
        buscarAlert.classList.remove("d-none");

        try {
            const response = await fetch(`/api/compradores/buscar/${cedula}`, {
                headers: getAuthHeaders()
            });
            
            if (response.ok) {
                // Comprador encontrado (200 OK)
                const comprador = await response.json();
                compradorSeleccionado = comprador;
                
                compradorNombreInput.value = comprador.nombre;
                compradorTelefonoInput.value = comprador.telefono || "";
                compradorCorreoInput.value = comprador.correo || "";
                compradorDireccionInput.value = comprador.direccion || "";

                toggleFormInputs(false);
                mostrarAlertaModal("success", `¡Comprador existente encontrado! (${comprador.nombre}). Procede a configurar la financiación.`);
                
                // Mostrar botón de transición al Paso 2 y ocultar Guardar
                btnGuardarComprador.classList.add("d-none");
                btnSiguienteStep.classList.remove("d-none");

            } else if (response.status === 404) {
                // Comprador nuevo (404 Not Found)
                compradorSeleccionado = null;
                compradorForm.reset();
                toggleFormInputs(true); // Habilitar registro
                mostrarAlertaModal("info", "El cliente no está registrado. Ingrese los datos para crear su Ficha de Comprador.");
                
                btnGuardarComprador.classList.remove("d-none");
                btnGuardarComprador.disabled = false;
                btnGuardarComprador.textContent = "Registrar Comprador";
                btnSiguienteStep.classList.add("d-none");
            } else {
                const data = await response.json();
                mostrarAlertaModal("danger", data.error || "Ocurrió un error al buscar al comprador.");
            }
        } catch (error) {
            console.error("Error al buscar comprador:", error);
            mostrarAlertaModal("danger", "Error de red al conectar con el servidor.");
        }
    });

    // Guardar comprador nuevo por POST
    compradorForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        
        const cedula = buscarCedulaInput.value.trim();
        const nombre = compradorNombreInput.value.trim();
        const telefono = compradorTelefonoInput.value.trim();
        const correo = compradorCorreoInput.value.trim();
        const direccion = compradorDireccionInput.value.trim();

        if (!cedula || !nombre || !telefono || !correo || !direccion) {
            mostrarAlertaModal("warning", "Todos los campos de la Ficha del Comprador son obligatorios (Cédula, Nombre, Teléfono, Correo y Dirección).");
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(correo)) {
            mostrarAlertaModal("warning", "El formato del correo electrónico ingresado es inválido.");
            return;
        }

        btnGuardarComprador.disabled = true;
        btnGuardarComprador.innerHTML = `<span class="spinner-border spinner-border-sm" role="status"></span> Guardando...`;

        try {
            const response = await fetch("/api/compradores", {
                method: "POST",
                headers: getAuthHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({
                    cedula,
                    nombre,
                    telefono,
                    correo,
                    direccion
                })
            });

            const data = await response.json();

            if (response.ok) {
                compradorSeleccionado = data;
                mostrarAlertaModal("success", "¡Ficha de comprador registrada correctamente!");
                btnGuardarComprador.innerHTML = `<i class="bi bi-check-circle-fill"></i> Registrado`;
                toggleFormInputs(false);
                
                // Mostrar botón de Siguiente y ocultar Guardar
                btnGuardarComprador.classList.add("d-none");
                btnSiguienteStep.classList.remove("d-none");

                // Auto transición en caliente tras 1.2 segundos
                setTimeout(() => {
                    transitionToStep2();
                }, 1200);

            } else {
                mostrarAlertaModal("danger", data.error || "Error al guardar el comprador.");
                btnGuardarComprador.disabled = false;
                btnGuardarComprador.textContent = "Registrar Comprador";
            }

        } catch (error) {
            console.error("Error al guardar comprador:", error);
            mostrarAlertaModal("danger", "Error de red al guardar los datos del comprador.");
            btnGuardarComprador.disabled = false;
            btnGuardarComprador.textContent = "Registrar Comprador";
        }
    });

    // Cambios de pasos (Navegación del Modal)
    btnSiguienteStep.addEventListener("click", () => {
        transitionToStep2();
    });

    btnAtrasStep.addEventListener("click", () => {
        modalStepCredito.classList.add("d-none");
        modalStepComprador.classList.remove("d-none");
    });

    function transitionToStep2() {
        modalStepComprador.classList.add("d-none");
        modalStepCredito.classList.remove("d-none");
    }

    // Confirmar y firmar el contrato (Paso 2 Submit)
    btnConfirmarVenta.addEventListener("click", async () => {
        // Validar campos
        const precio = parseFloat(creditPrecioPactado.value);
        const separacion = parseFloat(creditCuotaSeparacion.value);
        const plazo = parseInt(creditPlazoMeses.value);
        const fecha = creditFechaVenta.value;
        const file = creditDocumentoPropiedad.files[0];

        // Ocultar alertas
        creditoAlert.classList.add("d-none");

        if (isNaN(precio) || isNaN(separacion) || isNaN(plazo) || !fecha || !file) {
            mostrarAlertaCredito("Todos los campos marcados con (*) y el Título de Propiedad digitalizado son obligatorios.");
            return;
        }

        if (separacion > precio) {
            mostrarAlertaCredito("La cuota de separación no puede ser superior al precio total pactado.");
            return;
        }

        const separacionMinima = precio * 0.1;
        if (separacion < separacionMinima) {
            mostrarAlertaCredito(`La cuota de separación debe ser de al menos el 10% del precio total pactado (Mínimo: ${formatearPrecio(separacionMinima)} COP).`);
            return;
        }

        if (plazo <= 0) {
            mostrarAlertaCredito("El plazo en meses debe ser mayor a cero.");
            return;
        }

        // Validar que la cuota mensual resultante no sea inferior a 500.000 COP
        const saldo = precio - separacion;
        if (saldo > 0) {
            const cuotaMensual = Math.round(saldo / plazo);
            if (cuotaMensual < 500000) {
                mostrarAlertaCredito(`La cuota mensual resultante (${formatearPrecio(cuotaMensual)}) no puede ser inferior a $ 500.000 COP. Por favor reduzca el plazo en meses o incremente el abono de separación.`);
                return;
            }
        }

        // Formar FormData
        const formData = new FormData();
        formData.append("loteId", loteSeleccionado.id);
        formData.append("compradorId", compradorSeleccionado.id);
        formData.append("precioVentaPactado", precio);
        formData.append("cuotaSeparacion", separacion);
        formData.append("plazoMeses", plazo);
        formData.append("fechaVenta", fecha);
        formData.append("documentoPropiedad", file);

        // Feedback de carga
        setCreditLoading(true);

        try {
            const response = await fetch("/api/ventas", {
                method: "POST",
                headers: getAuthHeaders(),
                body: formData
            });

            const data = await response.json();

            if (response.ok) {
                // Éxito: Ocultar modal, recargar lotes de la etapa actual
                compradorModal.hide();
                
                // Cargar datos en el modal de éxito elaborado
                document.getElementById("exitoLote").textContent = `Lote ${loteSeleccionado.numeroLote} (${loteSeleccionado.etapa ? loteSeleccionado.etapa.nombreEtapa : 'N/A'})`;
                document.getElementById("exitoCliente").textContent = compradorSeleccionado.nombre;
                document.getElementById("exitoPrecio").textContent = formatearPrecio(precio);
                document.getElementById("exitoSeparacion").textContent = formatearPrecio(separacion);
                document.getElementById("exitoMeses").textContent = `${plazo} meses`;
                
                // Configurar enlace de descarga de PDF del contrato
                const btnDescargar = document.getElementById("btnDescargarContratoExito");
                if (btnDescargar) {
                    btnDescargar.href = data.urlPdfContrato || "#";
                }

                // Configurar botón para ir a la cartera
                const btnIrACartera = document.getElementById("btnIrACarteraExito");
                if (btnIrACartera) {
                    btnIrACartera.onclick = () => {
                        const exitoModal = bootstrap.Modal.getInstance(document.getElementById("ventaExitoModal"));
                        if (exitoModal) exitoModal.hide();
                        window.location.href = `cartera.html?cedula=${compradorSeleccionado.cedula}`;
                    };
                }

                // Mostrar el modal de éxito elaborado
                const exitoModalInstance = new bootstrap.Modal(document.getElementById("ventaExitoModal"));
                exitoModalInstance.show();

                // Recargar grilla de lotes y tabla de ventas
                if (etapaSelect) {
                    cargarLotes(etapaSelect.value);
                }
                cargarMisVentas();

            } else {
                mostrarAlertaCredito(data.error || "Ocurrió un error al registrar el contrato de venta.");
                setCreditLoading(false);
            }

        } catch (error) {
            console.error("Error al registrar venta:", error);
            mostrarAlertaCredito("Error de red al conectar con el servidor. Verifique conexión.");
            setCreditLoading(false);
        }
    });

    function mostrarAlertaCredito(mensaje) {
        creditoAlertText.textContent = mensaje;
        creditoAlert.classList.remove("d-none");
    }

    function setCreditLoading(isLoading) {
        if (isLoading) {
            btnConfirmarVenta.disabled = true;
            btnAtrasStep.disabled = true;
            creditSpinner.classList.remove("d-none");
            creditBtnText.textContent = "Procesando...";
        } else {
            btnConfirmarVenta.disabled = false;
            btnAtrasStep.disabled = false;
            creditSpinner.classList.add("d-none");
            creditBtnText.textContent = "Firmar Contrato";
        }
    }

    // Event listener para evitar que la cuota de separación disminuya del 10%
    creditCuotaSeparacion.addEventListener("change", () => {
        const val = parseFloat(creditCuotaSeparacion.value);
        const precio = parseFloat(creditPrecioPactado.value);
        if (isNaN(precio)) return;
        const minVal = Math.round(precio * 0.1);
        if (isNaN(val) || val < minVal) {
            creditCuotaSeparacion.value = minVal;
        }
    });

    // === Lógica para Registrar Nuevo Lote (Solo Admin) ===
    const addLoteForm = document.getElementById("addLoteForm");
    const addLoteModalElement = document.getElementById("addLoteModal");
    const addLoteAlert = document.getElementById("addLoteAlert");
    const addLoteAlertText = document.getElementById("addLoteAlertText");
    const addLoteSpinner = document.getElementById("addLoteSpinner");
    const btnConfirmarAddLote = document.getElementById("btnConfirmarAddLote");
    const loteEtapaSelect = document.getElementById("loteEtapa");
    const loteNumeroDisplay = document.getElementById("loteNumeroDisplay");
    const btnAbrirLoteModal = document.getElementById("btnAbrirLoteModal");

    // Función para auto-completar el número sugerido de lote
    async function actualizarNumeroLoteSugerido() {
        if (!loteEtapaSelect || !loteNumeroDisplay) return;
        const etapaId = loteEtapaSelect.value;
        loteNumeroDisplay.textContent = "Calculando...";
        try {
            const response = await fetch(`/api/lotes?etapaId=${etapaId}`, {
                headers: getAuthHeaders()
            });
            if (response.ok) {
                const lotes = await response.json();
                const nextLoteNum = lotes.length > 0 ? lotes[lotes.length - 1].numeroLote + 1 : 1;
                loteNumeroDisplay.textContent = `Lote ${nextLoteNum}`;
            } else {
                loteNumeroDisplay.textContent = "Error al calcular";
            }
        } catch (error) {
            console.error("Error al sugerir número de lote:", error);
            loteNumeroDisplay.textContent = "Error al calcular";
        }
    }

    if (loteEtapaSelect) {
        loteEtapaSelect.addEventListener("change", actualizarNumeroLoteSugerido);
    }
    if (btnAbrirLoteModal) {
        btnAbrirLoteModal.addEventListener("click", actualizarNumeroLoteSugerido);
    }

    if (addLoteForm) {
        addLoteForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (addLoteAlert) addLoteAlert.classList.add("d-none");
            
            const etapaId = document.getElementById("loteEtapa").value;
            const areaM2 = document.getElementById("loteArea").value;
            const precioBase = document.getElementById("lotePrecio").value;
            const descripcion = document.getElementById("loteDescripcion").value;
            const imagenFile = document.getElementById("loteImagen").files[0];

            if (addLoteSpinner) addLoteSpinner.classList.remove("d-none");
            if (btnConfirmarAddLote) btnConfirmarAddLote.disabled = true;

            try {
                // Construir multipart FormData
                const formData = new FormData();
                formData.append("etapaId", etapaId);
                formData.append("areaM2", areaM2);
                formData.append("precioBase", precioBase);
                if (descripcion) {
                    formData.append("descripcion", descripcion);
                }
                if (imagenFile) {
                    formData.append("imagenLote", imagenFile);
                }

                const response = await fetch("/api/lotes", {
                    method: "POST",
                    headers: getAuthHeaders(), // Sin Content-Type, lo infiere el navegador
                    body: formData
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.error || "Error al crear el lote.");
                }

                // Éxito: resetear form, cerrar modal, refrescar grilla
                addLoteForm.reset();
                
                const addLoteModal = bootstrap.Modal.getOrCreateInstance(addLoteModalElement);
                if (addLoteModal) addLoteModal.hide();
                
                // Forzar remoción del backdrop
                setTimeout(() => {
                    const backdrop = document.querySelector(".modal-backdrop");
                    if (backdrop) backdrop.remove();
                    document.body.classList.remove("modal-open");
                    document.body.style.overflow = "";
                    document.body.style.paddingRight = "";
                }, 300);

                // Refrescar lotes
                if (etapaSelect) {
                    cargarLotes(etapaSelect.value);
                }

            } catch (error) {
                console.error(error);
                if (addLoteAlertText && addLoteAlert) {
                    addLoteAlertText.textContent = error.message;
                    addLoteAlert.classList.remove("d-none");
                }
            } finally {
                if (addLoteSpinner) addLoteSpinner.classList.add("d-none");
                if (btnConfirmarAddLote) btnConfirmarAddLote.disabled = false;
            }
        });
    }

});

