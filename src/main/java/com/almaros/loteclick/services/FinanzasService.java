package com.almaros.loteclick.services;

import com.almaros.loteclick.models.*;
import com.almaros.loteclick.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio transaccional encargado del control contable y financiero:
 * recaudos de cuotas, egresos por rubro y generación de recibos de caja.
 */
@Service
public class FinanzasService {

    @Autowired
    private PagoIngresoRepository pagoIngresoRepository;

    @Autowired
    private EgresoRepository egresoRepository;

    @Autowired
    private AporteInversionistaRepository aporteInversionistaRepository;

    @Autowired
    private SocioProyectoRepository socioProyectoRepository;

    @Autowired
    private DistribucionSocioRepository distribucionSocioRepository;

    @Autowired
    private CuotaAmortizacionRepository cuotaAmortizacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VentaContratoRepository ventaContratoRepository;

    @Autowired
    private CompradorRepository compradorRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private AuditService auditService;

    /**
     * Registra un abono a una cuota mensual de manera atómica.
     * Cambia el estado de la cuota a PAGADA e inserta el registro en pagos_ingresos.
     */
    @Transactional(rollbackFor = Exception.class)
    public PagoIngreso registrarPago(UUID cuotaId, String usuarioCorreo, BigDecimal montoPagado, String concepto) throws IOException {
        // 1. Validar existencia de la cuota
        CuotaAmortizacion cuota = cuotaAmortizacionRepository.findById(cuotaId)
                .orElseThrow(() -> new IllegalArgumentException("Cuota de amortización no encontrada."));

        if ("PAGADA".equalsIgnoreCase(cuota.getEstadoPago())) {
            throw new IllegalStateException("Esta cuota ya ha sido cancelada.");
        }

        // 2. Obtener el usuario (vendedor/administrador)
        Usuario usuario = obtenerUsuarioActivoPorCorreo(usuarioCorreo);
        validarRol(usuario, List.of("VENDEDOR", "CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para registrar pagos de cartera.");

        // 3. Mutar el estado de la cuota a PAGADA
        cuota.setEstadoPago("PAGADA");
        cuotaAmortizacionRepository.save(cuota);

        // 3.1 Verificar si todas las cuotas de esta venta están pagadas
        VentaContrato venta = cuota.getVenta();
        List<CuotaAmortizacion> todasCuotas = cuotaAmortizacionRepository.findByVentaIdOrderByNumeroCuotaAsc(venta.getId());
        boolean todasPagadas = true;
        for (CuotaAmortizacion c : todasCuotas) {
            String estado = c.getId().equals(cuota.getId()) ? "PAGADA" : c.getEstadoPago();
            if (!"PAGADA".equalsIgnoreCase(estado)) {
                todasPagadas = false;
                break;
            }
        }

        if (todasPagadas) {
            Lote lote = venta.getLote();
            if (lote != null) {
                lote.setEstado("VENDIDO");
                loteRepository.save(lote);
                auditService.registrarAccion(
                        usuario,
                        "LOTE_VENDIDO",
                        String.format("El Lote %d (Etapa %s) ha sido totalmente pagado y su estado cambió a VENDIDO.", 
                                lote.getNumeroLote(), lote.getEtapa().getNombreEtapa())
                );
                System.out.println(">>> LOTE COMPLETAMENTE PAGADO: Lote " + lote.getNumeroLote() + " cambió su estado a VENDIDO.");
            }
        }

        // 4. Generar recibo de caja único
        String reciboNum = String.format("RC-%05d", pagoIngresoRepository.count() + 1);
        String urlRecibo = storageService.generarReciboPdfSimulado(
                reciboNum,
                venta.getLote().getNumeroLote().toString(),
                venta.getComprador().getNombre(),
                venta.getComprador().getCedula(),
                montoPagado.toString(),
                concepto,
                usuario.getNombreCompleto()
        );

        // 5. Crear la transacción de ingreso
        PagoIngreso pago = new PagoIngreso();
        pago.setCuota(cuota);
        pago.setVenta(venta);
        pago.setUsuario(usuario);
        pago.setMontoPagado(montoPagado);
        pago.setFechaPago(LocalDateTime.now());
        pago.setConcepto(concepto);
        pago.setUrlPdfRecibo(urlRecibo);

        System.out.println(">>> RECAUDO REGISTRADO: Recibo " + reciboNum + " por valor de " + montoPagado + " COP.");
        return pagoIngresoRepository.save(pago);
    }

    /**
     * Registra un egreso de caja operativo por rubro y lo deduce de las utilidades.
     */
    @Transactional(rollbackFor = Exception.class)
    public Egreso registrarEgreso(String contadorCorreo, BigDecimal monto, LocalDate fechaEgreso, String rubro, String descripcion) {
        // 1. Validar rubro permitido
        List<String> rubrosValidos = List.of("MAQUINARIA", "SUMINISTRO_AGUA", "SERVICIOS_PUBLICOS", "EXCAVACION", "OTROS");
        String rubroUpper = rubro.trim().toUpperCase();
        if (!rubrosValidos.contains(rubroUpper)) {
            throw new IllegalArgumentException("El rubro '" + rubro + "' es inválido. Categorías válidas: " + rubrosValidos);
        }

        // 2. Buscar al contador/administrador
        Usuario contador = obtenerUsuarioActivoPorCorreo(contadorCorreo);
        validarRol(contador, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para registrar egresos.");

        // 3. Crear el egreso
        Egreso egreso = new Egreso();
        egreso.setContador(contador);
        egreso.setMonto(monto);
        egreso.setFechaEgreso(fechaEgreso);
        egreso.setRubro(rubroUpper);
        egreso.setDescripcion(descripcion);

        System.out.println(">>> EGRESO REGISTRADO: Rubro " + rubroUpper + " por valor de " + monto + " COP.");
        return egresoRepository.save(egreso);
    }

    /**
     * Obtiene el listado cronológico de egresos. Filtra por fechas de forma opcional.
     */
    public List<Egreso> obtenerEgresosLiquidar(String correoUsuario, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para consultar egresos.");

        if (fechaInicio != null && fechaFin != null) {
            return egresoRepository.findByFechaEgresoBetweenOrderByFechaEgresoAsc(fechaInicio, fechaFin);
        }
        return egresoRepository.findAllByOrderByFechaEgresoDesc();
    }

    public List<Map<String, Object>> obtenerLiquidacionCaja(String correoUsuario, LocalDate fechaInicio, LocalDate fechaFin,
                                                            String tipoSalida, String rubro) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para consultar la liquidacion de caja.");

        String tipoNormalizado = normalizarFiltro(tipoSalida);
        String rubroNormalizado = normalizarFiltro(rubro);
        List<Map<String, Object>> movimientos = new ArrayList<>();

        for (Egreso egreso : obtenerEgresosLiquidar(correoUsuario, fechaInicio, fechaFin)) {
            if (tipoNormalizado != null && !"OPERATIVO".equals(tipoNormalizado)) {
                continue;
            }
            if (rubroNormalizado != null && !rubroNormalizado.equalsIgnoreCase(egreso.getRubro())) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", egreso.getId());
            item.put("fecha", egreso.getFechaEgreso().toString());
            item.put("tipoSalida", "OPERATIVO");
            item.put("rubro", egreso.getRubro());
            item.put("descripcion", egreso.getDescripcion() != null ? egreso.getDescripcion() : "");
            item.put("monto", egreso.getMonto());
            item.put("registradoPor", egreso.getContador().getNombreCompleto());
            item.put("beneficiario", "Operacion");
            item.put("referencia", "");
            movimientos.add(item);
        }

        List<DistribucionSocio> distribuciones = (fechaInicio != null && fechaFin != null)
                ? distribucionSocioRepository.findByFechaDistribucionBetweenOrderByFechaDistribucionDesc(fechaInicio, fechaFin)
                : distribucionSocioRepository.findAllByOrderByFechaDistribucionDesc();

        for (DistribucionSocio distribucion : distribuciones) {
            if (tipoNormalizado != null && !"DISTRIBUCION_SOCIOS".equals(tipoNormalizado)) {
                continue;
            }
            if (rubroNormalizado != null && !"DISTRIBUCION_SOCIOS".equals(rubroNormalizado)) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", distribucion.getId());
            item.put("fecha", distribucion.getFechaDistribucion().toString());
            item.put("tipoSalida", "DISTRIBUCION_SOCIOS");
            item.put("rubro", "DISTRIBUCION_SOCIOS");
            item.put("descripcion", distribucion.getDescripcion() != null ? distribucion.getDescripcion() : "Reparto a socio");
            item.put("monto", distribucion.getMonto());
            item.put("registradoPor", distribucion.getRegistradoPor().getNombreCompleto());
            item.put("beneficiario", distribucion.getSocio().getNombre());
            item.put("referencia", distribucion.getReferencia() != null ? distribucion.getReferencia() : "");
            movimientos.add(item);
        }

        movimientos.sort((a, b) -> ((String) b.get("fecha")).compareTo((String) a.get("fecha")));
        return movimientos;
    }

    /**
     * Calcula los indicadores financieros principales del dashboard.
     * Se considera la cuota de separación como dinero ya recaudado porque el flujo
     * actual la persiste dentro del contrato, no como un PagoIngreso independiente.
     */
    public Map<String, Object> obtenerResumenFinanciero(String correoUsuario) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para consultar el dashboard financiero.");

        List<VentaContrato> ventas = ventaContratoRepository.findAll();
        List<PagoIngreso> pagos = pagoIngresoRepository.findAll();
        List<Egreso> egresos = egresoRepository.findAll();
        List<DistribucionSocio> distribuciones = distribucionSocioRepository.findAll();

        BigDecimal ventasEfectivas = ventas.stream()
                .map(VentaContrato::getPrecioVentaPactado)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSeparaciones = ventas.stream()
                .map(VentaContrato::getCuotaSeparacion)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagos = pagos.stream()
                .map(PagoIngreso::getMontoPagado)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEgresosOperativos = egresos.stream()
                .map(Egreso::getMonto)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDistribuciones = distribuciones.stream()
                .map(DistribucionSocio::getMonto)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRecaudado = totalSeparaciones.add(totalPagos);
        BigDecimal totalSalidasCaja = totalEgresosOperativos.add(totalDistribuciones);
        BigDecimal pendienteRecaudo = ventasEfectivas.subtract(totalRecaudado).max(BigDecimal.ZERO);
        BigDecimal utilidadNeta = totalRecaudado.subtract(totalSalidasCaja);
        BigDecimal saldoCaja = obtenerResumenIngresos(correoUsuario, null, null).get("totalIngresos") instanceof BigDecimal totalIngresos
                ? totalIngresos.subtract(totalSalidasCaja)
                : BigDecimal.ZERO;

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("ventasEfectivas", ventasEfectivas);
        resumen.put("totalRecaudado", totalRecaudado);
        resumen.put("pendienteRecaudo", pendienteRecaudo);
        resumen.put("utilidadNeta", utilidadNeta);
        resumen.put("totalEgresos", totalEgresosOperativos);
        resumen.put("totalDistribucionesSocios", totalDistribuciones);
        resumen.put("totalSalidasCaja", totalSalidasCaja);
        resumen.put("saldoCaja", saldoCaja);
        resumen.put("cantidadVentas", ventas.size());
        resumen.put("cantidadPagos", pagos.size());
        resumen.put("cantidadEgresos", egresos.size());
        resumen.put("cantidadDistribucionesSocios", distribuciones.size());
        return resumen;
    }

    /**
     * Calcula la liquidación contable de egresos consolidada por rubro.
     */
    public Map<String, Object> obtenerResumenEgresos(String correoUsuario, LocalDate fechaInicio, LocalDate fechaFin,
                                                     String tipoSalida, String rubro) {
        List<Map<String, Object>> egresos = obtenerLiquidacionCaja(correoUsuario, fechaInicio, fechaFin, tipoSalida, rubro);

        BigDecimal totalEgresos = BigDecimal.ZERO;
        Map<String, BigDecimal> acumuladoPorRubro = new HashMap<>();
        Map<String, Integer> cantidadPorRubro = new HashMap<>();
        BigDecimal totalDistribucionesSocios = BigDecimal.ZERO;

        for (Map<String, Object> egreso : egresos) {
            BigDecimal monto = egreso.get("monto") instanceof BigDecimal value ? value : BigDecimal.ZERO;
            String rubroMovimiento = egreso.get("rubro") != null ? egreso.get("rubro").toString() : "SIN_RUBRO";
            String tipoMovimiento = egreso.get("tipoSalida") != null ? egreso.get("tipoSalida").toString() : "";

            totalEgresos = totalEgresos.add(monto);
            acumuladoPorRubro.put(rubroMovimiento, acumuladoPorRubro.getOrDefault(rubroMovimiento, BigDecimal.ZERO).add(monto));
            cantidadPorRubro.put(rubroMovimiento, cantidadPorRubro.getOrDefault(rubroMovimiento, 0) + 1);
            if ("DISTRIBUCION_SOCIOS".equals(tipoMovimiento)) {
                totalDistribucionesSocios = totalDistribucionesSocios.add(monto);
            }
        }

        List<Map<String, Object>> rubros = acumuladoPorRubro.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("rubro", entry.getKey());
                    map.put("total", entry.getValue());
                    map.put("cantidad", cantidadPorRubro.getOrDefault(entry.getKey(), 0));
                    return map;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("total")).compareTo((BigDecimal) a.get("total")))
                .toList();

        Map<String, Object> rubroMayor = rubros.isEmpty() ? null : rubros.get(0);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalEgresos", totalEgresos);
        resumen.put("cantidadEgresos", egresos.size());
        resumen.put("cantidadRubros", rubros.size());
        resumen.put("rubroMayor", rubroMayor);
        resumen.put("rubros", rubros);
        resumen.put("totalDistribucionesSocios", totalDistribucionesSocios);
        return resumen;
    }

    @Transactional(rollbackFor = Exception.class)
    public AporteInversionista registrarAporteInversionista(String correoUsuario, String nombreInversionista,
                                                            BigDecimal monto, LocalDate fechaAporte, String descripcion) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para registrar aportes de inversionistas.");

        if (nombreInversionista == null || nombreInversionista.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del inversionista es obligatorio.");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del aporte debe ser superior a cero.");
        }
        if (fechaAporte == null) {
            throw new IllegalArgumentException("La fecha del aporte es obligatoria.");
        }

        AporteInversionista aporte = new AporteInversionista();
        aporte.setNombreInversionista(nombreInversionista.trim());
        aporte.setMonto(monto);
        aporte.setFechaAporte(fechaAporte);
        aporte.setDescripcion(descripcion);
        aporte.setRegistradoPor(usuario);
        return aporteInversionistaRepository.save(aporte);
    }

    public List<Map<String, Object>> obtenerListadoIngresos(String correoUsuario, LocalDate fechaInicio, LocalDate fechaFin) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para consultar ingresos.");

        List<Map<String, Object>> movimientos = new ArrayList<>();

        for (VentaContrato venta : ventaContratoRepository.findAll()) {
            if (estaEnRango(venta.getFechaVenta(), fechaInicio, fechaFin) && venta.getCuotaSeparacion() != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("fecha", venta.getFechaVenta().toString());
                item.put("origen", "VENTA");
                item.put("tipo", "CUOTA_SEPARACION");
                item.put("referencia", "Lote " + venta.getLote().getNumeroLote());
                item.put("detalle", "Cuota de separación - " + venta.getComprador().getNombre());
                item.put("monto", venta.getCuotaSeparacion());
                movimientos.add(item);
            }
        }

        for (PagoIngreso pago : pagoIngresoRepository.findAll()) {
            LocalDate fechaPago = pago.getFechaPago().toLocalDate();
            if (estaEnRango(fechaPago, fechaInicio, fechaFin)) {
                Map<String, Object> item = new HashMap<>();
                item.put("fecha", fechaPago.toString());
                item.put("origen", "VENTA");
                item.put("tipo", pago.getConcepto());
                item.put("referencia", "Lote " + pago.getVenta().getLote().getNumeroLote());
                item.put("detalle", pago.getVenta().getComprador().getNombre());
                item.put("monto", pago.getMontoPagado());
                movimientos.add(item);
            }
        }

        List<AporteInversionista> aportes = (fechaInicio != null && fechaFin != null)
                ? aporteInversionistaRepository.findByFechaAporteBetweenOrderByFechaAporteDesc(fechaInicio, fechaFin)
                : aporteInversionistaRepository.findAllByOrderByFechaAporteDesc();

        for (AporteInversionista aporte : aportes) {
            Map<String, Object> item = new HashMap<>();
            item.put("fecha", aporte.getFechaAporte().toString());
            item.put("origen", "INVERSIONISTA");
            item.put("tipo", "APORTE_CAPITAL");
            item.put("referencia", aporte.getNombreInversionista());
            item.put("detalle", aporte.getDescripcion() != null ? aporte.getDescripcion() : "Aporte de capital");
            item.put("monto", aporte.getMonto());
            movimientos.add(item);
        }

        movimientos.sort((a, b) -> ((String) b.get("fecha")).compareTo((String) a.get("fecha")));
        return movimientos;
    }

    public Map<String, Object> obtenerResumenIngresos(String correoUsuario, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> ingresos = obtenerListadoIngresos(correoUsuario, fechaInicio, fechaFin);

        BigDecimal totalIngresos = ingresos.stream()
                .map(i -> (BigDecimal) i.get("monto"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVentas = ingresos.stream()
                .filter(i -> "VENTA".equals(i.get("origen")))
                .map(i -> (BigDecimal) i.get("monto"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInversionistas = ingresos.stream()
                .filter(i -> "INVERSIONISTA".equals(i.get("origen")))
                .map(i -> (BigDecimal) i.get("monto"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> salidasCaja = obtenerLiquidacionCaja(correoUsuario, fechaInicio, fechaFin, null, null);
        BigDecimal totalSalidasCaja = salidasCaja.stream()
                .map(i -> (BigDecimal) i.get("monto"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoCaja = totalIngresos.subtract(totalSalidasCaja);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalIngresos", totalIngresos);
        resumen.put("totalVentas", totalVentas);
        resumen.put("totalInversionistas", totalInversionistas);
        resumen.put("cantidadIngresos", ingresos.size());
        resumen.put("totalSalidasCaja", totalSalidasCaja);
        resumen.put("saldoCaja", saldoCaja);
        return resumen;
    }

    @Transactional(rollbackFor = Exception.class)
    public SocioProyecto registrarSocioProyecto(String correoUsuario, String nombre, String telefono, String correo,
                                                BigDecimal porcentajeParticipacion, String observaciones) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para registrar socios.");

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del socio es obligatorio.");
        }
        if (porcentajeParticipacion != null &&
                (porcentajeParticipacion.compareTo(BigDecimal.ZERO) < 0 || porcentajeParticipacion.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("El porcentaje de participacion debe estar entre 0 y 100.");
        }

        SocioProyecto socio = new SocioProyecto();
        socio.setNombre(nombre.trim());
        socio.setTelefono(telefono);
        socio.setCorreo(correo != null ? correo.trim().toLowerCase() : null);
        socio.setPorcentajeParticipacion(porcentajeParticipacion);
        socio.setObservaciones(observaciones);
        socio.setActivo(true);
        return socioProyectoRepository.save(socio);
    }

    public List<Map<String, Object>> obtenerSociosProyecto(String correoUsuario) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para consultar socios.");

        List<DistribucionSocio> distribuciones = distribucionSocioRepository.findAll();
        Map<UUID, BigDecimal> totalPorSocio = new HashMap<>();
        for (DistribucionSocio distribucion : distribuciones) {
            UUID socioId = distribucion.getSocio().getId();
            totalPorSocio.put(socioId, totalPorSocio.getOrDefault(socioId, BigDecimal.ZERO).add(distribucion.getMonto()));
        }

        List<Map<String, Object>> socios = new ArrayList<>();
        for (SocioProyecto socio : socioProyectoRepository.findAllByOrderByNombreAsc()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", socio.getId());
            item.put("nombre", socio.getNombre());
            item.put("telefono", socio.getTelefono() != null ? socio.getTelefono() : "");
            item.put("correo", socio.getCorreo() != null ? socio.getCorreo() : "");
            item.put("porcentajeParticipacion", socio.getPorcentajeParticipacion());
            item.put("activo", socio.getActivo());
            item.put("observaciones", socio.getObservaciones() != null ? socio.getObservaciones() : "");
            item.put("totalRecibido", totalPorSocio.getOrDefault(socio.getId(), BigDecimal.ZERO));
            socios.add(item);
        }
        return socios;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<DistribucionSocio> registrarDistribucionSocios(String correoUsuario, LocalDate fechaDistribucion,
                                                               String referencia, String descripcion,
                                                               List<Map<String, Object>> distribucionesPayload) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("CONTADOR", "ADMINISTRADOR"),
                "No tiene permisos para registrar distribuciones a socios.");

        if (fechaDistribucion == null) {
            throw new IllegalArgumentException("La fecha de distribucion es obligatoria.");
        }
        if (distribucionesPayload == null || distribucionesPayload.isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos un socio con monto a distribuir.");
        }

        List<DistribucionSocio> distribuciones = new ArrayList<>();
        for (Map<String, Object> item : distribucionesPayload) {
            if (item.get("socioId") == null || item.get("monto") == null) {
                continue;
            }

            UUID socioId = UUID.fromString(item.get("socioId").toString());
            BigDecimal monto = new BigDecimal(item.get("monto").toString());
            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            SocioProyecto socio = socioProyectoRepository.findById(socioId)
                    .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado: " + socioId));

            if (socio.getActivo() != null && !socio.getActivo()) {
                throw new IllegalArgumentException("No se puede distribuir dinero a un socio inactivo: " + socio.getNombre());
            }

            DistribucionSocio distribucion = new DistribucionSocio();
            distribucion.setSocio(socio);
            distribucion.setRegistradoPor(usuario);
            distribucion.setMonto(monto);
            distribucion.setFechaDistribucion(fechaDistribucion);
            distribucion.setReferencia(referencia);
            distribucion.setDescripcion(descripcion);
            distribuciones.add(distribucion);
        }

        if (distribuciones.isEmpty()) {
            throw new IllegalArgumentException("No hay montos validos para registrar distribucion.");
        }

        return distribucionSocioRepository.saveAll(distribuciones);
    }

    /**
     * Utilidad para restablecer la base de datos a su estado original (semilla).
     * Elimina todos los registros de egresos, pagos_ingresos, y limpia los lotes y compradores de prueba.
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetDatabase(String correoUsuario) {
        Usuario usuario = obtenerUsuarioActivoPorCorreo(correoUsuario);
        validarRol(usuario, List.of("ADMINISTRADOR"),
                "Solo un administrador puede restablecer la base de datos.");

        // 1. Limpiar pagos/ingresos
        pagoIngresoRepository.deleteAll();
        
        // 2. Limpiar egresos
        egresoRepository.deleteAll();

        // 2.1 Limpiar aportes de inversionistas
        aporteInversionistaRepository.deleteAll();

        // 2.2 Limpiar distribuciones a socios
        distribucionSocioRepository.deleteAll();
        
        // 3. Limpiar cuotas de amortización
        cuotaAmortizacionRepository.deleteAll();
        
        // 4. Limpiar ventas/contratos
        ventaContratoRepository.deleteAll();
        
        // 5. Borrar compradores no semilla (mantener 111222 y 987654)
        List<Comprador> compradores = compradorRepository.findAll();
        for (Comprador comp : compradores) {
            if (!"111222".equals(comp.getCedula()) && !"987654".equals(comp.getCedula())) {
                compradorRepository.delete(comp);
            }
        }
        
        // 6. Restablecer todos los lotes a su estado original (y mantener precio base)
        List<Lote> lotes = loteRepository.findAll();
        for (Lote lote : lotes) {
            int i = lote.getNumeroLote();
            if (i <= 12) {
                lote.setEstado("DISPONIBLE");
            } else if (i <= 15) {
                lote.setEstado("SEPARADO");
            } else if (i <= 18) {
                lote.setEstado("VENDIDO");
            } else {
                lote.setEstado("DISPONIBLE");
            }
            loteRepository.save(lote);
        }
        
        // 7. Volver a sembrar los contratos de prueba iniciales
        Etapa etapa1 = etapaRepository.findByNombreEtapa("Etapa 1").orElse(null);
        Etapa etapa2 = etapaRepository.findByNombreEtapa("Etapa 2").orElse(null);
        Etapa etapa3 = etapaRepository.findByNombreEtapa("Etapa 3").orElse(null);
        
        if (etapa1 != null && etapa2 != null && etapa3 != null) {
            Comprador juan = compradorRepository.findByCedula("111222").orElse(null);
            Comprador maria = compradorRepository.findByCedula("987654").orElse(null);
            Usuario vendedor = usuarioRepository.findByCorreo("vendedor@almaros.com").orElse(null);
            if (vendedor == null) {
                vendedor = usuarioRepository.findAll().stream().findFirst().orElse(null);
            }
            
            if (juan != null && maria != null && vendedor != null) {
                crearContratoReset(juan, 13, etapa1, "SEPARADO", 12, vendedor);
                crearContratoReset(juan, 14, etapa1, "SEPARADO", 24, vendedor);
                crearContratoReset(juan, 16, etapa2, "VENDIDO", 10, vendedor);
                
                crearContratoReset(maria, 13, etapa2, "SEPARADO", 18, vendedor);
                crearContratoReset(maria, 15, etapa3, "SEPARADO", 12, vendedor);
            }
        }
        // Re-sembrar aportes demo
        Usuario admin = usuarioRepository.findByCorreo("admin@almaros.com").orElse(null);
        if (admin != null && aporteInversionistaRepository.count() == 0) {
            AporteInversionista aporte1 = new AporteInversionista();
            aporte1.setNombreInversionista("Socio Capital Norte");
            aporte1.setMonto(BigDecimal.valueOf(45000000L));
            aporte1.setFechaAporte(LocalDate.now().minusMonths(3));
            aporte1.setDescripcion("Capital inicial para apertura de vías internas.");
            aporte1.setRegistradoPor(admin);

            AporteInversionista aporte2 = new AporteInversionista();
            aporte2.setNombreInversionista("Fondo Inmobiliario Andes");
            aporte2.setMonto(BigDecimal.valueOf(30000000L));
            aporte2.setFechaAporte(LocalDate.now().minusMonths(1));
            aporte2.setDescripcion("Refuerzo de caja para urbanismo y servicios.");
            aporte2.setRegistradoPor(admin);
            aporteInversionistaRepository.saveAll(List.of(aporte1, aporte2));
        }
        sembrarSociosYDistribucionesDemo(admin);
        System.out.println(">>> LA BASE DE DATOS FUE RESTABLECIDA CORRECTAMENTE AL ESTADO SEMILLA.");
    }

    private void sembrarSociosYDistribucionesDemo(Usuario admin) {
        if (admin == null) {
            return;
        }

        if (socioProyectoRepository.count() == 0) {
            SocioProyecto socio1 = new SocioProyecto(null, "Socio 1", "3001112233", "socio1@almaros.com",
                    BigDecimal.valueOf(25), true, "Participacion fundadora");
            SocioProyecto socio2 = new SocioProyecto(null, "Socio 2", "3001112244", "socio2@almaros.com",
                    BigDecimal.valueOf(25), true, "Participacion fundadora");
            SocioProyecto socio3 = new SocioProyecto(null, "Socio 3", "3001112255", "socio3@almaros.com",
                    BigDecimal.valueOf(25), true, "Participacion fundadora");
            SocioProyecto socio4 = new SocioProyecto(null, "Socio 4", "3001112266", "socio4@almaros.com",
                    BigDecimal.valueOf(25), true, "Participacion fundadora");
            socioProyectoRepository.saveAll(List.of(socio1, socio2, socio3, socio4));
        }

        if (distribucionSocioRepository.count() == 0) {
            List<SocioProyecto> socios = socioProyectoRepository.findAllByActivoTrueOrderByNombreAsc();
            if (socios.size() >= 3) {
                List<DistribucionSocio> demo = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    DistribucionSocio distribucion = new DistribucionSocio();
                    distribucion.setSocio(socios.get(i));
                    distribucion.setRegistradoPor(admin);
                    distribucion.setMonto(BigDecimal.valueOf(5000000L));
                    distribucion.setFechaDistribucion(LocalDate.now().minusDays(20));
                    distribucion.setReferencia("Venta Lote 13 - abono inicial");
                    distribucion.setDescripcion("Distribucion parcial de recaudo a socios");
                    demo.add(distribucion);
                }
                distribucionSocioRepository.saveAll(demo);
            }
        }
    }

    private boolean estaEnRango(LocalDate fecha, LocalDate inicio, LocalDate fin) {
        if (fecha == null) return false;
        if (inicio != null && fecha.isBefore(inicio)) return false;
        if (fin != null && fecha.isAfter(fin)) return false;
        return true;
    }

    private String normalizarFiltro(String valor) {
        if (valor == null || valor.trim().isEmpty() || "TODOS".equalsIgnoreCase(valor.trim())) {
            return null;
        }
        return valor.trim().toUpperCase();
    }

    private Usuario obtenerUsuarioActivoPorCorreo(String correoUsuario) {
        if (correoUsuario == null || correoUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo del usuario autenticado es obligatorio.");
        }

        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Usuario interno no encontrado."));

        if (usuario.getActivo() != null && !usuario.getActivo()) {
            throw new IllegalStateException("La cuenta del usuario autenticado se encuentra inactiva.");
        }

        return usuario;
    }

    private void validarRol(Usuario usuario, List<String> rolesPermitidos, String mensajeError) {
        String rol = usuario.getRol() != null ? usuario.getRol().getNombreRol() : null;
        if (rol == null || !rolesPermitidos.contains(rol)) {
            throw new IllegalStateException(mensajeError);
        }
    }

    private void crearContratoReset(Comprador comprador, int numeroLote, Etapa etapa, String estado, int plazo, Usuario vendedor) {
        Lote lote = loteRepository.findByNumeroLoteAndEtapaId(numeroLote, etapa.getId()).orElse(null);
        if (lote == null) return;

        lote.setEstado(estado);
        loteRepository.save(lote);

        Optional<VentaContrato> contratoExistente = ventaContratoRepository.findByLoteId(lote.getId());
        if (contratoExistente.isEmpty()) {
            VentaContrato contrato = new VentaContrato();
            contrato.setLote(lote);
            contrato.setComprador(comprador);
            contrato.setVendedor(vendedor);
            contrato.setPrecioVentaPactado(lote.getPrecioBase());
            BigDecimal separacion = lote.getPrecioBase().multiply(BigDecimal.valueOf(0.1));
            contrato.setCuotaSeparacion(separacion);
            contrato.setPlazoMeses(plazo);
            contrato.setFechaVenta(LocalDate.now().minusMonths(2));
            
            // Generar los PDFs reales
            try {
                String urlContrato = storageService.generarContratoPdfSimulado(
                        String.valueOf(numeroLote),
                        etapa.getNombreEtapa(),
                        comprador.getNombre(),
                        comprador.getCedula(),
                        lote.getPrecioBase().toString(),
                        separacion.toString(),
                        plazo
                );
                contrato.setUrlPdfContrato(urlContrato);

                String urlPropiedad = storageService.generarTituloPropiedadPdfSimulado(
                        String.valueOf(numeroLote),
                        etapa.getNombreEtapa(),
                        comprador.getNombre(),
                        comprador.getCedula()
                );
                contrato.setUrlPdfPropiedad(urlPropiedad);
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback in case of error
                contrato.setUrlPdfContrato("/uploads/contrato_test_lote_" + numeroLote + "_" + etapa.getNombreEtapa().replace(" ", "") + ".pdf");
                contrato.setUrlPdfPropiedad("/uploads/propiedad_test_lote_" + numeroLote + "_" + etapa.getNombreEtapa().replace(" ", "") + ".pdf");
            }
            
            VentaContrato guardado = ventaContratoRepository.save(contrato);

            BigDecimal saldo = lote.getPrecioBase().subtract(separacion);
            BigDecimal montoCuota = saldo.divide(BigDecimal.valueOf(plazo), 0, java.math.RoundingMode.HALF_UP);
            
            List<CuotaAmortizacion> cuotas = new java.util.ArrayList<>();
            for (int i = 1; i <= plazo; i++) {
                CuotaAmortizacion cuota = new CuotaAmortizacion();
                cuota.setVenta(guardado);
                cuota.setNumeroCuota(i);
                cuota.setMontoCuota(montoCuota);
                cuota.setFechaVencimiento(contrato.getFechaVenta().plusMonths(i));
                if (i <= 2) {
                    cuota.setEstadoPago("PAGADA");
                } else if (cuota.getFechaVencimiento().isBefore(LocalDate.now())) {
                    cuota.setEstadoPago("VENCIDA");
                } else {
                    cuota.setEstadoPago("PENDIENTE");
                }
                
                cuota = cuotaAmortizacionRepository.save(cuota);
                
                // Si la cuota está pagada, crear el recibo y el PagoIngreso
                if ("PAGADA".equals(cuota.getEstadoPago())) {
                    PagoIngreso pago = new PagoIngreso();
                    pago.setCuota(cuota);
                    pago.setVenta(guardado);
                    pago.setUsuario(vendedor);
                    pago.setMontoPagado(montoCuota);
                    pago.setFechaPago(contrato.getFechaVenta().plusMonths(i).atStartOfDay());
                    pago.setConcepto("Abono de cuota " + i + " - Semilla");
                    
                    try {
                        String numRecibo = "RC-" + String.format("%05d", (int)(Math.random() * 100000));
                        String urlRecibo = storageService.generarReciboPdfSimulado(
                            numRecibo,
                            lote.getNumeroLote().toString(),
                            comprador.getNombre(),
                            comprador.getCedula(),
                            montoCuota.toString(),
                            pago.getConcepto(),
                            vendedor.getNombreCompleto()
                        );
                        pago.setUrlPdfRecibo(urlRecibo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    pagoIngresoRepository.save(pago);
                }
            }
        }
    }
}
