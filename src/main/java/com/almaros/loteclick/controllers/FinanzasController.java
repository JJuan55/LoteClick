package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.Egreso;
import com.almaros.loteclick.models.PagoIngreso;
import com.almaros.loteclick.models.AporteInversionista;
import com.almaros.loteclick.models.DistribucionSocio;
import com.almaros.loteclick.models.SocioProyecto;
import com.almaros.loteclick.models.Usuario;
import com.almaros.loteclick.services.FinanzasService;
import com.almaros.loteclick.services.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
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

    @Autowired
    private SessionService sessionService;

    /**
     * Endpoint para registrar el pago (abono) de una cuota de amortización.
     * POST /api/pagos
     */
    @PostMapping("/pagos")
    public ResponseEntity<?> registrarPago(@RequestHeader("Authorization") String authorizationHeader,
                                           @RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("cuotaId") == null || payload.get("montoPagado") == null || payload.get("concepto") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios: cuotaId, montoPagado y concepto."));
            }

            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("VENDEDOR", "CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para registrar pagos de cartera."
            );

            UUID cuotaId = UUID.fromString((String) payload.get("cuotaId"));
            BigDecimal montoPagado = new BigDecimal(payload.get("montoPagado").toString());
            String concepto = (String) payload.get("concepto");

            if (montoPagado.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El monto de abono debe ser un valor numérico superior a cero."));
            }

            PagoIngreso pago = finanzasService.registrarPago(cuotaId, usuario.getCorreo(), montoPagado, concepto);
            
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
    public ResponseEntity<?> registrarEgreso(@RequestHeader("Authorization") String authorizationHeader,
                                             @RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("monto") == null || payload.get("fechaEgreso") == null || payload.get("rubro") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios: monto, fechaEgreso y rubro."));
            }

            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para registrar egresos."
            );

            BigDecimal monto = new BigDecimal(payload.get("monto").toString());
            LocalDate fechaEgreso = LocalDate.parse((String) payload.get("fechaEgreso"));
            String rubro = (String) payload.get("rubro");
            String descripcion = (String) payload.get("descripcion");

            if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "El monto del egreso debe ser superior a cero."));
            }

            Egreso egreso = finanzasService.registrarEgreso(usuario.getCorreo(), monto, fechaEgreso, rubro, descripcion);
            
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
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "tipoSalida", required = false) String tipoSalida,
            @RequestParam(value = "rubro", required = false) String rubro) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar egresos."
            );
            return ResponseEntity.ok(finanzasService.obtenerLiquidacionCaja(usuario.getCorreo(), fechaInicio, fechaFin, tipoSalida, rubro));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno al liquidar los egresos: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para obtener el consolidado de egresos por rubro.
     * GET /api/egresos/resumen
     */
    @GetMapping("/egresos/resumen")
    public ResponseEntity<?> resumenEgresos(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "tipoSalida", required = false) String tipoSalida,
            @RequestParam(value = "rubro", required = false) String rubro) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar egresos."
            );
            return ResponseEntity.ok(finanzasService.obtenerResumenEgresos(usuario.getCorreo(), fechaInicio, fechaFin, tipoSalida, rubro));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno al calcular el resumen de egresos: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para consultar el resumen financiero general del dashboard.
     * GET /api/dashboard/resumen
     */
    @GetMapping("/dashboard/resumen")
    public ResponseEntity<?> obtenerResumenDashboard(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar el dashboard financiero."
            );
            return ResponseEntity.ok(finanzasService.obtenerResumenFinanciero(usuario.getCorreo()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error interno al calcular el resumen financiero: " + e.getMessage()));
        }
    }

    @GetMapping("/ingresos")
    public ResponseEntity<?> obtenerIngresos(@RequestHeader("Authorization") String authorizationHeader,
                                             @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                             @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar ingresos."
            );
            return ResponseEntity.ok(finanzasService.obtenerListadoIngresos(usuario.getCorreo(), fechaInicio, fechaFin));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al consultar ingresos: " + e.getMessage()));
        }
    }

    @GetMapping("/ingresos/resumen")
    public ResponseEntity<?> resumenIngresos(@RequestHeader("Authorization") String authorizationHeader,
                                             @RequestParam(value = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                             @RequestParam(value = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar ingresos."
            );
            return ResponseEntity.ok(finanzasService.obtenerResumenIngresos(usuario.getCorreo(), fechaInicio, fechaFin));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al calcular el resumen de ingresos: " + e.getMessage()));
        }
    }

    @PostMapping("/ingresos/inversionistas")
    public ResponseEntity<?> registrarAporteInversionista(@RequestHeader("Authorization") String authorizationHeader,
                                                          @RequestBody Map<String, Object> payload) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para registrar aportes de inversionistas."
            );

            String nombre = payload.get("nombreInversionista") != null ? payload.get("nombreInversionista").toString() : null;
            UUID socioId = payload.get("socioId") != null && !payload.get("socioId").toString().isBlank()
                    ? UUID.fromString(payload.get("socioId").toString())
                    : null;
            BigDecimal monto = payload.get("monto") != null ? new BigDecimal(payload.get("monto").toString()) : null;
            LocalDate fecha = payload.get("fechaAporte") != null ? LocalDate.parse(payload.get("fechaAporte").toString()) : null;
            String descripcion = payload.get("descripcion") != null ? payload.get("descripcion").toString() : null;

            AporteInversionista aporte = finanzasService.registrarAporteInversionista(usuario.getCorreo(), nombre, socioId, monto, fecha, descripcion);
            return ResponseEntity.ok(Map.of(
                    "id", aporte.getId(),
                    "nombreInversionista", aporte.getNombreInversionista(),
                    "monto", aporte.getMonto(),
                    "fechaAporte", aporte.getFechaAporte().toString(),
                    "descripcion", aporte.getDescripcion() != null ? aporte.getDescripcion() : ""
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al registrar el aporte: " + e.getMessage()));
        }
    }



    @GetMapping("/socios")
    public ResponseEntity<?> listarSocios(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar socios."
            );

            return ResponseEntity.ok(finanzasService.obtenerSociosProyecto(usuario.getCorreo()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al listar socios: " + e.getMessage()));
        }
    }

    @PostMapping("/socios")
    public ResponseEntity<?> registrarSocio(@RequestHeader("Authorization") String authorizationHeader,
                                            @RequestBody Map<String, Object> payload) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para registrar socios."
            );

            String nombre = payload.get("nombre") != null ? payload.get("nombre").toString() : null;
            String telefono = payload.get("telefono") != null ? payload.get("telefono").toString() : null;
            String correo = payload.get("correo") != null ? payload.get("correo").toString() : null;
            BigDecimal porcentajeParticipacion = obtenerPorcentajeParticipacion(payload);
            String observaciones = payload.get("observaciones") != null ? payload.get("observaciones").toString() : null;

            SocioProyecto socio = finanzasService.registrarSocioProyecto(
                    usuario.getCorreo(),
                    nombre,
                    telefono,
                    correo,
                    porcentajeParticipacion,
                    observaciones
            );

            return ResponseEntity.ok(respuestaSocio(socio));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al registrar socio: " + e.getMessage()));
        }
    }

    @PutMapping("/socios/{id}")
    public ResponseEntity<?> actualizarSocio(@RequestHeader("Authorization") String authorizationHeader,
                                             @PathVariable UUID id,
                                             @RequestBody Map<String, Object> payload) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para editar socios."
            );

            String nombre = payload.get("nombre") != null ? payload.get("nombre").toString() : null;
            String telefono = payload.get("telefono") != null ? payload.get("telefono").toString() : null;
            String correo = payload.get("correo") != null ? payload.get("correo").toString() : null;
            BigDecimal porcentajeParticipacion = obtenerPorcentajeParticipacion(payload);
            String observaciones = payload.get("observaciones") != null ? payload.get("observaciones").toString() : null;
            Boolean activo = payload.get("activo") != null ? Boolean.parseBoolean(payload.get("activo").toString()) : null;

            SocioProyecto socio = finanzasService.actualizarSocioProyecto(
                    usuario.getCorreo(),
                    id,
                    nombre,
                    telefono,
                    correo,
                    porcentajeParticipacion,
                    observaciones,
                    activo
            );

            return ResponseEntity.ok(respuestaSocio(socio));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al editar socio: " + e.getMessage()));
        }
    }

    @DeleteMapping("/socios/{id}")
    public ResponseEntity<?> eliminarSocio(@RequestHeader("Authorization") String authorizationHeader,
                                           @PathVariable UUID id) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para eliminar socios."
            );

            SocioProyecto socio = finanzasService.desactivarSocioProyecto(usuario.getCorreo(), id);
            return ResponseEntity.ok(Map.of(
                    "id", socio.getId(),
                    "nombre", socio.getNombre(),
                    "activo", socio.getActivo()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al eliminar socio: " + e.getMessage()));
        }
    }

    @PostMapping("/distribuciones-socios")
    public ResponseEntity<?> registrarDistribucionSocios(@RequestHeader("Authorization") String authorizationHeader,
                                                         @RequestBody Map<String, Object> payload) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para registrar distribuciones a socios."
            );

            LocalDate fechaDistribucion = payload.get("fechaDistribucion") != null
                    ? LocalDate.parse(payload.get("fechaDistribucion").toString())
                    : null;
            String referencia = payload.get("referencia") != null ? payload.get("referencia").toString() : null;
            String descripcion = payload.get("descripcion") != null ? payload.get("descripcion").toString() : null;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> distribuciones = payload.get("distribuciones") instanceof List<?>
                    ? (List<Map<String, Object>>) payload.get("distribuciones")
                    : List.of();

            List<DistribucionSocio> registro = finanzasService.registrarDistribucionSocios(
                    usuario.getCorreo(),
                    fechaDistribucion,
                    referencia,
                    descripcion,
                    distribuciones
            );

            BigDecimal totalDistribuido = registro.stream()
                    .map(DistribucionSocio::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ResponseEntity.ok(Map.of(
                    "cantidad", registro.size(),
                    "totalDistribuido", totalDistribuido
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al registrar distribucion: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de utilidad para desarrollo y auditoría que limpia transacciones temporales
     * y restablece la base de datos a su estado original (semilla).
     * POST /api/test/reset
     */
    @PostMapping("/test/reset")
    public ResponseEntity<?> resetDatabase(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            Usuario usuario = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR"),
                    "Solo un administrador puede restablecer la base de datos."
            );
            finanzasService.resetDatabase(usuario.getCorreo());
            return ResponseEntity.ok(Map.of("message", "Base de datos restablecida correctamente al estado semilla original."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al restablecer la base de datos: " + e.getMessage()));
        }
    }

    private Map<String, Object> respuestaSocio(SocioProyecto socio) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", socio.getId());
        response.put("nombre", socio.getNombre());
        response.put("telefono", socio.getTelefono() != null ? socio.getTelefono() : "");
        response.put("correo", socio.getCorreo() != null ? socio.getCorreo() : "");
        response.put("porcentajeParticipacion", socio.getPorcentajeParticipacion());
        response.put("activo", socio.getActivo());
        return response;
    }

    private BigDecimal obtenerPorcentajeParticipacion(Map<String, Object> payload) {
        if (payload.get("porcentajeParticipacion") == null) {
            return null;
        }
        String valor = payload.get("porcentajeParticipacion").toString().trim();
        if (valor.isEmpty()) {
            return null;
        }
        return new BigDecimal(valor);
    }
}
