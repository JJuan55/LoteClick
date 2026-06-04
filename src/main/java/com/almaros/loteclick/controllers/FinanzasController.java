package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.Egreso;
import com.almaros.loteclick.models.PagoIngreso;
import com.almaros.loteclick.services.FinanzasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para operaciones contables y financieras de LoteClick:
 * abonos/recaudos ordinarios y control de egresos por rubros.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FinanzasController {

    @Autowired
    private FinanzasService finanzasService;

    /**
     * Endpoint para registrar el pago (abono) de una cuota de amortización.
     * POST /api/pagos
     */
    @PostMapping("/pagos")
    public ResponseEntity<?> registrarPago(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("cuotaId") == null || payload.get("usuarioCorreo") == null ||
                payload.get("montoPagado") == null || payload.get("concepto") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios: cuotaId, usuarioCorreo, montoPagado y concepto."));
            }

            UUID cuotaId = UUID.fromString((String) payload.get("cuotaId"));
            String usuarioCorreo = (String) payload.get("usuarioCorreo");
            BigDecimal montoPagado = new BigDecimal(payload.get("montoPagado").toString());
            String concepto = (String) payload.get("concepto");

            if (montoPagado.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El monto de abono debe ser un valor numérico superior a cero."));
            }

            PagoIngreso pago = finanzasService.registrarPago(cuotaId, usuarioCorreo, montoPagado, concepto);
            
            // Retornar una estructura limpia para evitar fallos de serialización de Jackson con proxies de Hibernate
            return ResponseEntity.ok(Map.of(
                "id", pago.getId(),
                "montoPagado", pago.getMontoPagado(),
                "fechaPago", pago.getFechaPago().toString(),
                "concepto", pago.getConcepto(),
                "urlPdfRecibo", pago.getUrlPdfRecibo() != null ? pago.getUrlPdfRecibo() : ""
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al procesar el pago: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para registrar un egreso operativo clasificado por rubro.
     * POST /api/egresos
     */
    @PostMapping("/egresos")
    public ResponseEntity<?> registrarEgreso(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("contadorCorreo") == null || payload.get("monto") == null ||
                payload.get("fechaEgreso") == null || payload.get("rubro") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios: contadorCorreo, monto, fechaEgreso y rubro."));
            }

            String contadorCorreo = (String) payload.get("contadorCorreo");
            BigDecimal monto = new BigDecimal(payload.get("monto").toString());
            LocalDate fechaEgreso = LocalDate.parse((String) payload.get("fechaEgreso"));
            String rubro = (String) payload.get("rubro");
            String descripcion = (String) payload.get("descripcion");

            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El monto del egreso debe ser superior a cero."));
            }

            Egreso egreso = finanzasService.registrarEgreso(contadorCorreo, monto, fechaEgreso, rubro, descripcion);
            
            // Retornar estructura de mapa limpia
            return ResponseEntity.ok(Map.of(
                "id", egreso.getId(),
                "monto", egreso.getMonto(),
                "fechaEgreso", egreso.getFechaEgreso().toString(),
                "rubro", egreso.getRubro(),
                "descripcion", egreso.getDescripcion() != null ? egreso.getDescripcion() : "",
                "contador", Map.of("nombreCompleto", egreso.getContador().getNombreCompleto())
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al registrar el egreso: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para liquidar y auditar los egresos. Permite filtrar opcionalmente por fechas.
     * GET /api/egresos/liquidar
     */
    @GetMapping("/egresos/liquidar")
    public ResponseEntity<?> liquidarEgresos(
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            List<Egreso> egresos = finanzasService.obtenerEgresosLiquidar(fechaInicio, fechaFin);
            
            // Transformar la lista para remover proxies perezosos de Hibernate antes de enviar la respuesta JSON
            List<Map<String, Object>> result = egresos.stream().map(eg -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", eg.getId());
                map.put("monto", eg.getMonto());
                map.put("fechaEgreso", eg.getFechaEgreso().toString());
                map.put("rubro", eg.getRubro());
                map.put("descripcion", eg.getDescripcion() != null ? eg.getDescripcion() : "");
                map.put("contador", Map.of("nombreCompleto", eg.getContador().getNombreCompleto()));
                return map;
            }).toList();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al liquidar los egresos: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de utilidad para desarrollo y auditoría que limpia transacciones temporales
     * y restablece la base de datos a su estado original (semilla).
     * POST /api/test/reset
     */
    @PostMapping("/test/reset")
    public ResponseEntity<?> resetDatabase() {
        try {
            finanzasService.resetDatabase();
            return ResponseEntity.ok(Map.of("message", "Base de datos restablecida correctamente al estado semilla original."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al restablecer la base de datos: " + e.getMessage()));
        }
    }
}
