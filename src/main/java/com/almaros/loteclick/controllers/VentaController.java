package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.Comprador;
import com.almaros.loteclick.models.CuotaAmortizacion;
import com.almaros.loteclick.models.Usuario;
import com.almaros.loteclick.models.VentaContrato;
import com.almaros.loteclick.models.PagoIngreso;
import com.almaros.loteclick.repositories.CompradorRepository;
import com.almaros.loteclick.repositories.CuotaAmortizacionRepository;
import com.almaros.loteclick.repositories.VentaContratoRepository;
import com.almaros.loteclick.repositories.PagoIngresoRepository;
import com.almaros.loteclick.services.SessionService;
import com.almaros.loteclick.services.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Controlador REST para procesar las ventas y consultar estados de cuenta.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CompradorRepository compradorRepository;

    @Autowired
    private VentaContratoRepository ventaContratoRepository;

    @Autowired
    private CuotaAmortizacionRepository cuotaAmortizacionRepository;

    @Autowired
    private PagoIngresoRepository pagoIngresoRepository;
    @Autowired
    private SessionService sessionService;

    /**
     * Endpoint para registrar una nueva venta con su archivo binario (Multipart).
     * Recibe parámetros por multipart/form-data.
     */
    @PostMapping("/ventas")
    public ResponseEntity<?> registrarVenta(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("loteId") UUID loteId,
            @RequestParam("compradorId") UUID compradorId,
            @RequestParam("precioVentaPactado") BigDecimal precioVentaPactado,
            @RequestParam("cuotaSeparacion") BigDecimal cuotaSeparacion,
            @RequestParam("plazoMeses") Integer plazoMeses,
            @RequestParam("fechaVenta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaVenta,
            @RequestParam(value = "documentoPropiedad", required = false) MultipartFile documentoPropiedad) {

        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("VENDEDOR", "ADMINISTRADOR"),
                    "No tiene permisos para registrar ventas."
            );
            // Validaciones básicas de negocio
            if (precioVentaPactado.compareTo(cuotaSeparacion) < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "La cuota de separación no puede ser superior al precio total pactado."));
            }
            BigDecimal separacionMinima = precioVentaPactado.multiply(BigDecimal.valueOf(0.1));
            if (cuotaSeparacion.compareTo(separacionMinima) < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "La cuota de separación debe ser de al menos el 10% del precio total pactado."));
            }
            if (plazoMeses <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El plazo en meses debe ser mayor a cero."));
            }
            
            // Validar que la cuota mensual resultante no sea ridículamente baja
            BigDecimal saldo = precioVentaPactado.subtract(cuotaSeparacion);
            if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cuotaMensual = saldo.divide(BigDecimal.valueOf(plazoMeses), 0, RoundingMode.HALF_UP);
                if (cuotaMensual.compareTo(BigDecimal.valueOf(500000)) < 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "La cuota mensual calculada no puede ser inferior a $ 500.000 COP. Por favor reduzca el plazo en meses o incremente el abono inicial."));
                }
            }

            VentaContrato contrato = ventaService.registrarVenta(
                    loteId, compradorId, usuario.getCorreo(),
                    precioVentaPactado, cuotaSeparacion, 
                    plazoMeses, fechaVenta, documentoPropiedad
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(contrato);

        } catch (IllegalStateException e) {
            // Error de negocio: Lote ocupado u otra regla
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Error de argumentos faltantes
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocurrió un error inesperado al procesar la venta: " + e.getMessage()));
        }
    }

    /**
     * Devuelve el estado de cuenta consolidado de un comprador buscando por su cédula.
     * Endpoint: GET /api/clientes/{cedula}/estado-cuenta
     */
    @GetMapping("/clientes/{cedula}/estado-cuenta")
    public ResponseEntity<?> obtenerEstadoCuenta(@RequestHeader("Authorization") String authorizationHeader,
                                                 @PathVariable String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cédula es requerida"));
        }

        try {
            sessionService.obtenerUsuarioAutenticado(authorizationHeader, List.of("VENDEDOR", "CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar estados de cuenta.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        Optional<Comprador> compradorOpt = compradorRepository.findByCedula(cedula.trim());
        if (compradorOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se encontró ningún comprador con la cédula ingresada."));
        }

        Comprador comprador = compradorOpt.get();
        List<VentaContrato> contratos = ventaContratoRepository.findByCompradorId(comprador.getId());

        List<Map<String, Object>> contratosConCuotas = new ArrayList<>();

        for (VentaContrato c : contratos) {
            List<CuotaAmortizacion> cuotas = cuotaAmortizacionRepository.findByVentaIdOrderByNumeroCuotaAsc(c.getId());
            
            List<Map<String, Object>> cuotasConRecibo = new ArrayList<>();
            for (CuotaAmortizacion cuota : cuotas) {
                Map<String, Object> mapCuota = new HashMap<>();
                mapCuota.put("id", cuota.getId());
                mapCuota.put("numeroCuota", cuota.getNumeroCuota());
                mapCuota.put("montoCuota", cuota.getMontoCuota());
                mapCuota.put("fechaVencimiento", cuota.getFechaVencimiento());
                mapCuota.put("estadoPago", cuota.getEstadoPago());
                
                // Buscar si existe un recibo de pago para esta cuota
                List<PagoIngreso> pagos = pagoIngresoRepository.findByCuotaId(cuota.getId());
                if (!pagos.isEmpty()) {
                    mapCuota.put("urlPdfRecibo", pagos.get(0).getUrlPdfRecibo());
                } else {
                    mapCuota.put("urlPdfRecibo", null);
                }
                cuotasConRecibo.add(mapCuota);
            }
            
            Map<String, Object> mapContrato = new HashMap<>();
            mapContrato.put("id", c.getId());
            mapContrato.put("lote", c.getLote());
            mapContrato.put("precioVentaPactado", c.getPrecioVentaPactado());
            mapContrato.put("cuotaSeparacion", c.getCuotaSeparacion());
            mapContrato.put("plazoMeses", c.getPlazoMeses());
            mapContrato.put("fechaVenta", c.getFechaVenta());
            mapContrato.put("urlPdfContrato", c.getUrlPdfContrato());
            mapContrato.put("urlPdfPropiedad", c.getUrlPdfPropiedad());
            mapContrato.put("cuotas", cuotasConRecibo);

            contratosConCuotas.add(mapContrato);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comprador", comprador);
        response.put("contratos", contratosConCuotas);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/ventas/mis-ventas
     * Devuelve las ventas realizadas por el vendedor autenticado, o todas si es administrador.
     */
    @GetMapping("/ventas/mis-ventas")
    public ResponseEntity<?> obtenerMisVentas(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("VENDEDOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar este registro de ventas."
            );

            List<VentaContrato> contratos;
            if (usuario.getRol().getNombreRol().equals("ADMINISTRADOR")) {
                contratos = ventaContratoRepository.findAll();
            } else {
                contratos = ventaContratoRepository.findByVendedorId(usuario.getId());
            }

            List<Map<String, Object>> responseList = new ArrayList<>();
            for (VentaContrato c : contratos) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("numeroLote", c.getLote() != null ? c.getLote().getNumeroLote() : null);
                map.put("etapaNombre", (c.getLote() != null && c.getLote().getEtapa() != null) ? c.getLote().getEtapa().getNombreEtapa() : "N/A");
                map.put("vendedorNombre", c.getVendedor() != null ? c.getVendedor().getNombreCompleto() : "N/A");
                map.put("compradorNombre", c.getComprador() != null ? c.getComprador().getNombre() : "N/A");
                map.put("precioVentaPactado", c.getPrecioVentaPactado());
                map.put("cuotaSeparacion", c.getCuotaSeparacion());
                map.put("plazoMeses", c.getPlazoMeses());
                map.put("fechaVenta", c.getFechaVenta());
                responseList.add(map);
            }

            return ResponseEntity.ok(responseList);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al consultar las ventas: " + e.getMessage()));
        }
    }
}
