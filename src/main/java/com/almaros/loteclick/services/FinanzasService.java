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
import java.util.List;
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
        Usuario usuario = usuarioRepository.findByCorreo(usuarioCorreo.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Usuario interno no encontrado."));

        // 3. Mutar el estado de la cuota a PAGADA
        cuota.setEstadoPago("PAGADA");
        cuotaAmortizacionRepository.save(cuota);

        // 4. Generar recibo de caja único
        String reciboNum = String.format("RC-%05d", pagoIngresoRepository.count() + 1);
        VentaContrato venta = cuota.getVenta();
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
        Usuario contador = usuarioRepository.findByCorreo(contadorCorreo.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Usuario contable no encontrado."));

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
    public List<Egreso> obtenerEgresosLiquidar(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null) {
            return egresoRepository.findByFechaEgresoBetweenOrderByFechaEgresoAsc(fechaInicio, fechaFin);
        }
        return egresoRepository.findAllByOrderByFechaEgresoDesc();
    }

    /**
     * Utilidad para restablecer la base de datos a su estado original (semilla).
     * Elimina todos los registros de egresos, pagos_ingresos, y limpia los lotes y compradores de prueba.
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetDatabase() {
        // 1. Limpiar pagos/ingresos
        pagoIngresoRepository.deleteAll();
        
        // 2. Limpiar egresos
        egresoRepository.deleteAll();
        
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
            } else {
                lote.setEstado("VENDIDO");
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
        System.out.println(">>> LA BASE DE DATOS FUE RESTABLECIDA CORRECTAMENTE AL ESTADO SEMILLA.");
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
            contrato.setUrlPdfContrato("/uploads/contrato_test_lote_" + numeroLote + "_" + etapa.getNombreEtapa().replace(" ", "") + ".txt");
            contrato.setUrlPdfPropiedad("/uploads/propiedad_test_lote_" + numeroLote + "_" + etapa.getNombreEtapa().replace(" ", "") + ".pdf");
            
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
                cuotas.add(cuota);
            }
            cuotaAmortizacionRepository.saveAll(cuotas);
        }
    }
}
