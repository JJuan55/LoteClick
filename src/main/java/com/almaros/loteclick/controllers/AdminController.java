package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.*;
import com.almaros.loteclick.repositories.*;
import com.almaros.loteclick.services.AuditService;
import com.almaros.loteclick.services.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private VentaContratoRepository ventaContratoRepository;

    @Autowired
    private PagoIngresoRepository pagoIngresoRepository;

    @Autowired
    private EgresoRepository egresoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private BitacoraAuditoriaRepository bitacoraAuditoriaRepository;

    @Autowired
    private com.almaros.loteclick.services.StorageService storageService;

    /**
     * GET /api/analitica/dashboard
     * Calcula los KPIs en tiempo real utilizando consultas de agregación directas.
     */
    @GetMapping("/analitica/dashboard")
    public ResponseEntity<?> getDashboardData(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // Permitido para ADMINISTRADOR y CONTADOR
            Usuario adminUser = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR", "CONTADOR"),
                    "Acceso denegado. No tiene permisos para consultar la analítica del dashboard."
            );

            // Consultas agregadas directas (SUM)
            BigDecimal ventasEfectivas = ventaContratoRepository.sumPrecioVentaPactado();
            BigDecimal sumCuotasSeparacion = ventaContratoRepository.sumCuotaSeparacion();
            BigDecimal sumAbonos = pagoIngresoRepository.sumMontoPagado();

            BigDecimal totalRecaudado = sumAbonos.add(sumCuotasSeparacion);
            BigDecimal pendienteRecaudo = ventasEfectivas.subtract(totalRecaudado).max(BigDecimal.ZERO);

            BigDecimal sumEgresos = egresoRepository.sumMonto();
            BigDecimal utilidadNeta = totalRecaudado.subtract(sumEgresos);

            // Agrupación mensual para Ingresos vs Egresos
            List<VentaContrato> ventas = ventaContratoRepository.findAll();
            List<PagoIngreso> pagos = pagoIngresoRepository.findAll();
            List<Egreso> egresos = egresoRepository.findAll();

            // Mapa para agrupar ingresos y egresos por "YYYY-MM"
            Map<String, MonthlyData> monthlyMap = new TreeMap<>();

            // 1. Sumar cuotas de separación de contratos
            for (VentaContrato v : ventas) {
                if (v.getFechaVenta() != null && v.getCuotaSeparacion() != null) {
                    String yearMonth = v.getFechaVenta().toString().substring(0, 7); // YYYY-MM
                    monthlyMap.computeIfAbsent(yearMonth, k -> new MonthlyData(k))
                            .addIngresos(v.getCuotaSeparacion());
                }
            }

            // 2. Sumar abonos de cuotas
            for (PagoIngreso p : pagos) {
                if (p.getFechaPago() != null && p.getMontoPagado() != null) {
                    String yearMonth = p.getFechaPago().toString().substring(0, 7); // YYYY-MM
                    monthlyMap.computeIfAbsent(yearMonth, k -> new MonthlyData(k))
                            .addIngresos(p.getMontoPagado());
                }
            }

            // 3. Sumar egresos operativos
            for (Egreso e : egresos) {
                if (e.getFechaEgreso() != null && e.getMonto() != null) {
                    String yearMonth = e.getFechaEgreso().toString().substring(0, 7); // YYYY-MM
                    monthlyMap.computeIfAbsent(yearMonth, k -> new MonthlyData(k))
                            .addEgresos(e.getMonto());
                }
            }

            // Convertir el mapa ordenado a una lista presentable
            List<Map<String, Object>> mensualList = new ArrayList<>();
            for (Map.Entry<String, MonthlyData> entry : monthlyMap.entrySet()) {
                String ym = entry.getKey();
                MonthlyData md = entry.getValue();

                // Traducir YYYY-MM a "Mes Año" en español
                String[] parts = ym.split("-");
                int year = Integer.parseInt(parts[0]);
                int monthVal = Integer.parseInt(parts[1]);
                LocalDate dummyDate = LocalDate.of(year, monthVal, 1);
                String monthName = dummyDate.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
                // Capitalizar el nombre del mes
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

                Map<String, Object> mData = new HashMap<>();
                mData.put("mesRaw", ym);
                mData.put("mes", monthName + " " + year);
                mData.put("ingresos", md.getIngresos());
                mData.put("egresos", md.getEgresos());
                mensualList.add(mData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("ventasEfectivas", ventasEfectivas);
            response.put("totalRecaudado", totalRecaudado);
            response.put("pendienteRecaudo", pendienteRecaudo);
            response.put("utilidadNeta", utilidadNeta);
            response.put("mensual", mensualList);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el dashboard analítico: " + e.getMessage()));
        }
    }

    /**
     * POST /api/usuarios/registro
     * Registro exclusivo de personal interno (VENDEDOR o CONTADOR) por el Administrador.
     */
    @PostMapping("/usuarios/registro")
    public ResponseEntity<?> registrarUsuario(@RequestHeader("Authorization") String authorizationHeader,
                                              @RequestBody Map<String, String> payload) {
        try {
            Usuario adminUser = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR"),
                    "Acceso denegado. Solo el Administrador puede registrar personal interno."
            );

            String nombreCompleto = payload.get("nombreCompleto");
            String correo = payload.get("correo");
            String contrasena = payload.get("contrasena");
            String rolNombre = payload.get("rolNombre");

            if (nombreCompleto == null || nombreCompleto.trim().isEmpty() ||
                correo == null || correo.trim().isEmpty() ||
                contrasena == null || contrasena.trim().isEmpty() ||
                rolNombre == null || rolNombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios."));
            }

            String correoNormalizado = correo.trim().toLowerCase();
            rolNombre = rolNombre.trim().toUpperCase();

            if (!rolNombre.equals("VENDEDOR") && !rolNombre.equals("CONTADOR")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El rol asignado debe ser VENDEDOR o CONTADOR."));
            }

            // Validar correo duplicado
            if (usuarioRepository.findByCorreo(correoNormalizado).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El correo ya se encuentra registrado."));
            }

            // Buscar Rol
            Role rol = rolRepository.findByNombreRol(rolNombre)
                    .orElseThrow(() -> new IllegalArgumentException("El rol especificado no existe en el sistema."));

            // Encriptar contraseña con BCrypt
            String hash = BCrypt.hashpw(contrasena, BCrypt.gensalt());

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombreCompleto(nombreCompleto.trim());
            nuevoUsuario.setCorreo(correoNormalizado);
            nuevoUsuario.setContrasenaHash(hash);
            nuevoUsuario.setRol(rol);
            nuevoUsuario.setActivo(true);

            Usuario saved = usuarioRepository.save(nuevoUsuario);

            // Registrar en auditoría
            auditService.registrarAccion(
                    adminUser,
                    "CREAR_USUARIO",
                    String.format("El Administrador %s creó al usuario %s con rol %s",
                            adminUser.getNombreCompleto(), saved.getCorreo(), rol.getNombreRol())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "nombreCompleto", saved.getNombreCompleto(),
                    "correo", saved.getCorreo(),
                    "rol", rol.getNombreRol()
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al registrar el usuario: " + e.getMessage()));
        }
    }

    /**
     * GET /api/usuarios
     * Listar los empleados actuales en la plataforma.
     */
    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR"),
                    "Acceso denegado. Solo el Administrador puede listar usuarios."
            );

            List<Usuario> usuarios = usuarioRepository.findAll();
            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Usuario u : usuarios) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("nombreCompleto", u.getNombreCompleto());
                m.put("correo", u.getCorreo());
                m.put("rol", u.getRol() != null ? u.getRol().getNombreRol() : "SIN_ROL");
                m.put("activo", u.getActivo() != null ? u.getActivo() : true);
                responseList.add(m);
            }

            return ResponseEntity.ok(responseList);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al listar usuarios: " + e.getMessage()));
        }
    }

    /**
     * POST /api/lotes
     * Registro de un nuevo lote por el Administrador.
     */
    @PostMapping(value = "/lotes", consumes = {"multipart/form-data"})
    public ResponseEntity<?> registrarLote(@RequestHeader("Authorization") String authorizationHeader,
                                           @RequestParam("etapaId") Integer etapaId,
                                           @RequestParam("areaM2") BigDecimal areaM2,
                                           @RequestParam("precioBase") BigDecimal precioBase,
                                           @RequestParam(value = "descripcion", required = false) String descripcion,
                                           @RequestParam(value = "imagenLote", required = false) org.springframework.web.multipart.MultipartFile imagenLote) {
        try {
            Usuario adminUser = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR"),
                    "Acceso denegado. Solo el Administrador puede registrar nuevos lotes."
            );

            // Validar existencia de etapa
            Etapa etapa = etapaRepository.findById(etapaId)
                    .orElseThrow(() -> new IllegalArgumentException("La etapa especificada no existe."));

            // Auto-calcular el número de lote para esta etapa
            List<Lote> list = loteRepository.findByEtapaIdOrderByNumeroLoteAsc(etapaId);
            int numeroLote = list.isEmpty() ? 1 : list.get(list.size() - 1).getNumeroLote() + 1;

            // Subir imagen si se proporciona
            String urlImagen = null;
            if (imagenLote != null && !imagenLote.isEmpty()) {
                urlImagen = storageService.guardarImagenLote(imagenLote);
            }

            Lote nuevoLote = new Lote();
            nuevoLote.setNumeroLote(numeroLote);
            nuevoLote.setEtapa(etapa);
            nuevoLote.setAreaM2(areaM2);
            nuevoLote.setPrecioBase(precioBase);
            nuevoLote.setEstado("DISPONIBLE"); // Estado por defecto
            nuevoLote.setDescripcion(descripcion);
            nuevoLote.setUrlImagen(urlImagen);

            Lote saved = loteRepository.save(nuevoLote);

            // Registrar en auditoría
            auditService.registrarAccion(
                    adminUser,
                    "CREAR_LOTE",
                    String.format("El Administrador %s registró el Lote %d en la Etapa %s con un área de %.1f m2 y precio base de %s COP",
                            adminUser.getNombreCompleto(), saved.getNumeroLote(), etapa.getNombreEtapa(), saved.getAreaM2().doubleValue(), saved.getPrecioBase().setScale(0).toString())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al crear el lote: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/egresos/{id}
     * Eliminación de egresos operativos por el Administrador. Recalcula la utilidad neta de inmediato.
     */
    @DeleteMapping("/egresos/{id}")
    public ResponseEntity<?> eliminarEgreso(@RequestHeader("Authorization") String authorizationHeader,
                                            @PathVariable("id") UUID id) {
        try {
            Usuario adminUser = sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR"),
                    "Acceso denegado. Solo el Administrador puede eliminar egresos."
            );

            Egreso egreso = egresoRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("El egreso operativo no existe en la base de datos."));

            // Guardamos información para la auditoría
            BigDecimal monto = egreso.getMonto();
            String rubro = egreso.getRubro();

            // Eliminar registro
            egresoRepository.delete(egreso);

            // Registrar en auditoría
            auditService.registrarAccion(
                    adminUser,
                    "ELIMINAR_EGRESO",
                    String.format("El Administrador %s eliminó el egreso operativo ID %s de rubro %s por valor de %s COP",
                            adminUser.getNombreCompleto(), id.toString(), rubro, monto.setScale(0).toString())
            );

            return ResponseEntity.ok(Map.of("message", "Egreso operativo eliminado con éxito."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al eliminar el egreso: " + e.getMessage()));
        }
    }

    /**
     * GET /api/auditoria
     * Obtiene el listado completo de logs de auditoría para el administrador.
     */
    @GetMapping("/auditoria")
    public ResponseEntity<?> getLogsAuditoria(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("ADMINISTRADOR"),
                    "Acceso denegado. Solo el Administrador puede consultar los logs de auditoría."
            );

            List<BitacoraAuditoria> logs = bitacoraAuditoriaRepository.findAllByOrderByFechaHoraDesc();
            return ResponseEntity.ok(logs);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al consultar la bitácora: " + e.getMessage()));
        }
    }

    /**
     * Clase auxiliar interna para agrupar ingresos y egresos mensuales.
     */
    private static class MonthlyData {
        private final String key;
        private BigDecimal ingresos = BigDecimal.ZERO;
        private BigDecimal egresos = BigDecimal.ZERO;

        public MonthlyData(String key) {
            this.key = key;
        }

        public void addIngresos(BigDecimal val) {
            if (val != null) {
                this.ingresos = this.ingresos.add(val);
            }
        }

        public void addEgresos(BigDecimal val) {
            if (val != null) {
                this.egresos = this.egresos.add(val);
            }
        }

        public BigDecimal getIngresos() {
            return ingresos;
        }

        public BigDecimal getEgresos() {
            return egresos;
        }
    }
}
