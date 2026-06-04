package com.almaros.loteclick.config;

import com.almaros.loteclick.models.Role;
import com.almaros.loteclick.models.Usuario;
import com.almaros.loteclick.models.Etapa;
import com.almaros.loteclick.models.Lote;
import com.almaros.loteclick.models.Comprador;
import com.almaros.loteclick.models.VentaContrato;
import com.almaros.loteclick.models.CuotaAmortizacion;
import com.almaros.loteclick.models.AporteInversionista;
import com.almaros.loteclick.models.DistribucionSocio;
import com.almaros.loteclick.models.SocioProyecto;
import com.almaros.loteclick.repositories.RolRepository;
import com.almaros.loteclick.repositories.UsuarioRepository;
import com.almaros.loteclick.repositories.EtapaRepository;
import com.almaros.loteclick.repositories.LoteRepository;
import com.almaros.loteclick.repositories.CompradorRepository;
import com.almaros.loteclick.repositories.VentaContratoRepository;
import com.almaros.loteclick.repositories.CuotaAmortizacionRepository;
import com.almaros.loteclick.repositories.AporteInversionistaRepository;
import com.almaros.loteclick.repositories.DistribucionSocioRepository;
import com.almaros.loteclick.repositories.SocioProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que se ejecuta al iniciar la aplicación para garantizar que los roles,
 * usuarios de prueba, etapas y lotes iniciales existan en la base de datos de Supabase.
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private CompradorRepository compradorRepository;

    @Autowired
    private VentaContratoRepository ventaContratoRepository;

    @Autowired
    private CuotaAmortizacionRepository cuotaAmortizacionRepository;

    @Autowired
    private AporteInversionistaRepository aporteInversionistaRepository;

    @Autowired
    private SocioProyectoRepository socioProyectoRepository;

    @Autowired
    private DistribucionSocioRepository distribucionSocioRepository;


    @Override
    public void run(String... args) throws Exception {
        // 1. Asegurar la existencia de los roles básicos
        Role adminRol = registrarRolSiNoExiste("ADMINISTRADOR");
        Role contadorRol = registrarRolSiNoExiste("CONTADOR");
        Role vendedorRol = registrarRolSiNoExiste("VENDEDOR");

        // 2. Insertar usuarios de prueba si no existen registros en la tabla usuarios
        if (usuarioRepository.count() == 0) {
            // Usuario Administrador
            Usuario admin = new Usuario();
            admin.setNombreCompleto("Gerente Almaros");
            admin.setCorreo("admin@almaros.com");
            admin.setContrasenaHash(BCrypt.hashpw("admin123", BCrypt.gensalt()));
            admin.setRol(adminRol);
            admin.setActivo(true);
            usuarioRepository.save(admin);

            // Usuario Contador
            Usuario contador = new Usuario();
            contador.setNombreCompleto("Contador Almaros");
            contador.setCorreo("contador@almaros.com");
            contador.setContrasenaHash(BCrypt.hashpw("contador123", BCrypt.gensalt()));
            contador.setRol(contadorRol);
            contador.setActivo(true);
            usuarioRepository.save(contador);

            // Usuario Vendedor
            Usuario vendedor = new Usuario();
            vendedor.setNombreCompleto("Vendedor Almaros");
            vendedor.setCorreo("vendedor@almaros.com");
            vendedor.setContrasenaHash(BCrypt.hashpw("vendedor123", BCrypt.gensalt()));
            vendedor.setRol(vendedorRol);
            vendedor.setActivo(true);
            usuarioRepository.save(vendedor);

            // Usuario inactivo (para validar la excepción de cuenta deshabilitada)
            Usuario inactivo = new Usuario();
            inactivo.setNombreCompleto("Asesor Suspendido");
            inactivo.setCorreo("inactivo@almaros.com");
            inactivo.setContrasenaHash(BCrypt.hashpw("inactivo123", BCrypt.gensalt()));
            inactivo.setRol(vendedorRol);
            inactivo.setActivo(false);
            usuarioRepository.save(inactivo);

            System.out.println("------------------------------------------------------------------");
            System.out.println(">>> SE SEMBRARON LOS USUARIOS DE PRUEBA EXITOSAMENTE:");
            System.out.println("  - Admin: admin@almaros.com | Clave: admin123");
            System.out.println("  - Contador: contador@almaros.com | Clave: contador123");
            System.out.println("  - Vendedor: vendedor@almaros.com | Clave: vendedor123");
            System.out.println("  - Inactivo: inactivo@almaros.com | Clave: inactivo123");
            System.out.println("------------------------------------------------------------------");
        }

        // 3. Asegurar la existencia de las 4 etapas
        Etapa etapa1 = registrarEtapaSiNoExiste("Etapa 1");
        Etapa etapa2 = registrarEtapaSiNoExiste("Etapa 2");
        Etapa etapa3 = registrarEtapaSiNoExiste("Etapa 3");
        Etapa etapa4 = registrarEtapaSiNoExiste("Etapa 4");

        // 4. Sembrar y/o actualizar lotes a precios realistas en COP y añadir más lotes de ejemplo si es necesario
        List<Etapa> etapas = List.of(etapa1, etapa2, etapa3, etapa4);
        List<Lote> lotesParaGuardar = new ArrayList<>();

        for (Etapa et : etapas) {
            int cantidadLotes = 18; // 18 lotes por etapa (72 lotes en total)
            for (int i = 1; i <= cantidadLotes; i++) {
                Optional<Lote> loteExistenteOpt = loteRepository.findByNumeroLoteAndEtapaId(i, et.getId());
                Lote lote;
                if (loteExistenteOpt.isPresent()) {
                    lote = loteExistenteOpt.get();
                } else {
                    lote = new Lote();
                    lote.setNumeroLote(i);
                    lote.setEtapa(et);
                    
                    // Asignar estado por defecto a los nuevos lotes de prueba
                    // 1-12 disponibles, 13-15 separados, 16-18 vendidos
                    if (i <= 12) {
                        lote.setEstado("DISPONIBLE");
                    } else if (i <= 15) {
                        lote.setEstado("SEPARADO");
                    } else {
                        lote.setEstado("VENDIDO");
                    }
                }

                // Área simulada: 150 + i * 20 metros cuadrados (ej. 170m2, 190m2, ..., 510m2)
                BigDecimal area = BigDecimal.valueOf(150.00 + (i * 20.00));
                lote.setAreaM2(area);
                
                // Si el precio base es nulo o es menor a 1,000,000 COP, lo actualizamos a un valor realista en COP
                // Precio base realista en COP: 50.000.000 COP base + area * 250.000 COP/m2
                // Por ejemplo: para 170m2, precio = 50.000.000 + 42.500.000 = 92.500.000 COP
                if (lote.getPrecioBase() == null || lote.getPrecioBase().compareTo(BigDecimal.valueOf(1000000)) < 0) {
                    BigDecimal precio = BigDecimal.valueOf(50000000L).add(area.multiply(BigDecimal.valueOf(250000L)));
                    lote.setPrecioBase(precio);
                }

                lotesParaGuardar.add(lote);
            }
        }
        loteRepository.saveAll(lotesParaGuardar);
        System.out.println(">>> SE SINCRONIZÓ EL INVENTARIO DE LOTES (18 lotes por etapa, precios realistas en COP).");

        // 5. Sembrar compradores y contratos de prueba para el test de multi-propiedad
        sembrarCompradoresYContratosDePrueba(etapa1, etapa2, etapa3);
        sembrarAportesInversionistasDePrueba();
        sembrarSociosYDistribucionesDePrueba();
    }

    private void sembrarCompradoresYContratosDePrueba(Etapa etapa1, Etapa etapa2, Etapa etapa3) {
        Usuario vendedor = usuarioRepository.findByCorreo("vendedor@almaros.com").orElse(null);
        if (vendedor == null) {
            vendedor = usuarioRepository.findAll().stream().findFirst().orElse(null);
        }
        if (vendedor == null) return; // No se puede sembrar sin vendedor

        // 1. Crear Juan Valdez con cédula 111222
        Comprador juan = compradorRepository.findByCedula("111222").orElse(null);
        if (juan == null) {
            juan = new Comprador();
            juan.setCedula("111222");
            juan.setNombre("Juan Valdez");
            juan.setTelefono("3123456789");
            juan.setCorreo("juan.valdez@email.com");
            juan.setDireccion("Calle 10 # 5-20, Medellín");
            juan = compradorRepository.save(juan);
        }

        // 2. Crear María Cardona con cédula 987654
        Comprador maria = compradorRepository.findByCedula("987654").orElse(null);
        if (maria == null) {
            maria = new Comprador();
            maria.setCedula("987654");
            maria.setNombre("María Cardona");
            maria.setTelefono("3219876543");
            maria.setCorreo("maria.cardona@email.com");
            maria.setDireccion("Carrera 45 # 12-30, Envigado");
            maria = compradorRepository.save(maria);
        }

        // 3. Asociar Lotes y crear contratos para Juan Valdez (Lote 13 y 14 de Etapa 1, Lote 16 de Etapa 2)
        crearContratoSiNoExiste(juan, 13, etapa1, "SEPARADO", 12, vendedor);
        crearContratoSiNoExiste(juan, 14, etapa1, "SEPARADO", 24, vendedor);
        crearContratoSiNoExiste(juan, 16, etapa2, "VENDIDO", 10, vendedor);

        // 4. Asociar Lotes y crear contratos para María Cardona (Lote 13 de Etapa 2, Lote 15 de Etapa 3)
        crearContratoSiNoExiste(maria, 13, etapa2, "SEPARADO", 18, vendedor);
        crearContratoSiNoExiste(maria, 15, etapa3, "SEPARADO", 12, vendedor);
        
        System.out.println(">>> SE SEMBRARON COMPRADORES DE PRUEBA Y CONTRATOS MULTI-PROPIEDAD EXITOSAMENTE.");
    }

    private void crearContratoSiNoExiste(Comprador comprador, int numeroLote, Etapa etapa, String estado, int plazo, Usuario vendedor) {
        Lote lote = loteRepository.findByNumeroLoteAndEtapaId(numeroLote, etapa.getId()).orElse(null);
        if (lote == null) return;

        // Forzar estado del lote
        lote.setEstado(estado);
        loteRepository.save(lote);

        Optional<VentaContrato> contratoExistente = ventaContratoRepository.findByLoteId(lote.getId());
        if (contratoExistente.isEmpty()) {
            VentaContrato contrato = new VentaContrato();
            contrato.setLote(lote);
            contrato.setComprador(comprador);
            contrato.setVendedor(vendedor);
            contrato.setPrecioVentaPactado(lote.getPrecioBase());
            // Cuota de separación: 10%
            BigDecimal separacion = lote.getPrecioBase().multiply(BigDecimal.valueOf(0.1));
            contrato.setCuotaSeparacion(separacion);
            contrato.setPlazoMeses(plazo);
            contrato.setFechaVenta(LocalDate.now().minusMonths(2));
            contrato.setUrlPdfContrato("/uploads/contrato_test_lote_" + numeroLote + "_" + etapa.getNombreEtapa().replace(" ", "") + ".txt");
            contrato.setUrlPdfPropiedad("/uploads/propiedad_test_lote_" + numeroLote + "_" + etapa.getNombreEtapa().replace(" ", "") + ".pdf");
            
            VentaContrato guardado = ventaContratoRepository.save(contrato);

            // Crear cuotas de amortización
            BigDecimal saldo = lote.getPrecioBase().subtract(separacion);
            BigDecimal montoCuota = saldo.divide(BigDecimal.valueOf(plazo), 0, java.math.RoundingMode.HALF_UP);
            
            List<CuotaAmortizacion> cuotas = new ArrayList<>();
            for (int i = 1; i <= plazo; i++) {
                CuotaAmortizacion cuota = new CuotaAmortizacion();
                cuota.setVenta(guardado);
                cuota.setNumeroCuota(i);
                cuota.setMontoCuota(montoCuota);
                cuota.setFechaVencimiento(contrato.getFechaVenta().plusMonths(i));
                // Primeras 2 cuotas pagadas para probar la semaforización
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

    private void sembrarAportesInversionistasDePrueba() {
        if (aporteInversionistaRepository.count() > 0) return;

        Usuario admin = usuarioRepository.findByCorreo("admin@almaros.com").orElse(null);
        if (admin == null) return;

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

    private void sembrarSociosYDistribucionesDePrueba() {
        Usuario admin = usuarioRepository.findByCorreo("admin@almaros.com").orElse(null);
        if (admin == null) return;

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
                List<DistribucionSocio> distribuciones = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    DistribucionSocio distribucion = new DistribucionSocio();
                    distribucion.setSocio(socios.get(i));
                    distribucion.setRegistradoPor(admin);
                    distribucion.setMonto(BigDecimal.valueOf(5000000L));
                    distribucion.setFechaDistribucion(LocalDate.now().minusDays(20));
                    distribucion.setReferencia("Venta Lote 13 - abono inicial");
                    distribucion.setDescripcion("Distribucion parcial de recaudo a socios");
                    distribuciones.add(distribucion);
                }
                distribucionSocioRepository.saveAll(distribuciones);
            }
        }
    }


    private Role registrarRolSiNoExiste(String nombreRol) {
        Optional<Role> roleOpt = rolRepository.findByNombreRol(nombreRol);
        if (roleOpt.isEmpty()) {
            Role role = new Role();
            role.setNombreRol(nombreRol);
            return rolRepository.save(role);
        }
        return roleOpt.get();
    }

    private Etapa registrarEtapaSiNoExiste(String nombreEtapa) {
        Optional<Etapa> etapaOpt = etapaRepository.findByNombreEtapa(nombreEtapa);
        if (etapaOpt.isEmpty()) {
            Etapa etapa = new Etapa();
            etapa.setNombreEtapa(nombreEtapa);
            return etapaRepository.save(etapa);
        }
        return etapaOpt.get();
    }
}
