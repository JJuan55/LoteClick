package com.almaros.loteclick.config;

import com.almaros.loteclick.models.*;
import com.almaros.loteclick.repositories.*;
import com.almaros.loteclick.services.StorageService;
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
    private StorageService storageService;
    @Autowired
    private PagoIngresoRepository pagoIngresoRepository;


    @Override
    public void run(String... args) throws Exception {
        // 1. Asegurar la existencia de los roles básicos
        Role adminRol = registrarRolSiNoExiste("ADMINISTRADOR");
        Role contadorRol = registrarRolSiNoExiste("CONTADOR");
        Role vendedorRol = registrarRolSiNoExiste("VENDEDOR");

        // Remove any user with name containing "suspendido" or email "suspendido@almaros.com" or name "Asesor Suspendido"
        usuarioRepository.findAll().forEach(u -> {
            if (u.getNombreCompleto().equalsIgnoreCase("Asesor Suspendido") || 
                u.getCorreo().equalsIgnoreCase("suspendido@almaros.com") || 
                u.getNombreCompleto().toLowerCase().contains("suspendido")) {
                usuarioRepository.delete(u);
                System.out.println(">>> SE ELIMINÓ EL USUARIO SUSPENDIDO: " + u.getCorreo());
            }
        });

        // 2. Insertar usuarios de prueba de forma idempotente
        registrarUsuarioSiNoExiste("Gerente Almaros", "admin@almaros.com", "admin123", adminRol);
        registrarUsuarioSiNoExiste("Contador Almaros", "contador@almaros.com", "contador123", contadorRol);
        registrarUsuarioSiNoExiste("Vendedor Almaros", "vendedor@almaros.com", "vendedor123", vendedorRol);

        // 20 Vendedores realistas adicionales
        String[] nombresVendedores = {
            "Carlos Andrés Mendoza", "Laura Camila Restrepo", "Andrés Felipe Gómez", "Diana Patricia Pinzón",
            "Juan Esteban Rojas", "Sandra Milena Silva", "Mateo Alejandro Ortiz", "Natalia Sofia Castro",
            "Diego Fernando Valencia", "Valentina María Rincón", "Luis Eduardo Beltrán", "Paula Andrea Herrera",
            "Jorge Mario Zuluaga", "Gabriela Inés Pardo", "Santiago José Cardona", "Juliana Andrea Muñoz",
            "Camilo Andrés Torres", "Mariana Lucía Ramírez", "Daniel Felipe Martínez", "Carolina Inés Franco"
        };

        for (int i = 0; i < nombresVendedores.length; i++) {
            String correo = nombresVendedores[i].toLowerCase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace(" ", ".") + "@almaros.com";
            registrarUsuarioSiNoExiste(nombresVendedores[i], correo, "vendedor123", vendedorRol);
        }

        System.out.println("------------------------------------------------------------------");
        System.out.println(">>> SE SINCRONIZARON LOS USUARIOS DE PRUEBA EXITOSAMENTE.");
        System.out.println("------------------------------------------------------------------");

        // 3. Asegurar la existencia de las 4 etapas
        Etapa etapa1 = registrarEtapaSiNoExiste("Etapa 1");
        Etapa etapa2 = registrarEtapaSiNoExiste("Etapa 2");
        Etapa etapa3 = registrarEtapaSiNoExiste("Etapa 3");
        Etapa etapa4 = registrarEtapaSiNoExiste("Etapa 4");

        // 4. Sembrar y/o actualizar lotes a precios realistas en COP y añadir más lotes de ejemplo si es necesario
        List<Etapa> etapas = List.of(etapa1, etapa2, etapa3, etapa4);
        List<Lote> lotesParaGuardar = new ArrayList<>();

        for (Etapa et : etapas) {
            int cantidadLotes = 23; // 23 lotes por etapa (92 lotes en total, 5 nuevos DISPONIBLES)
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
                    // 1-12 disponibles, 13-15 separados, 16-18 vendidos, 19-23 disponibles
                    if (i <= 12) {
                        lote.setEstado("DISPONIBLE");
                    } else if (i <= 15) {
                        lote.setEstado("SEPARADO");
                    } else if (i <= 18) {
                        lote.setEstado("VENDIDO");
                    } else {
                        lote.setEstado("DISPONIBLE");
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

        // 5. Sembrar 50 clientes (compradores) realistas adicionales
        String[] nombresCompradores = {
            "Fernando Antonio Castro", "Gloria Patricia Restrepo", "Oscar Eduardo Díaz", "Martha Cecilia Rincón",
            "Javier Andrés Medina", "Diana Carolina Ospina", "Mauricio Alberto Beltrán", "Olga Lucía Londoño",
            "Gustavo Adolfo Salazar", "Claudia Patricia Vargas", "Ricardo Andrés Silva", "Liliana María Franco",
            "Álvaro José Gutiérrez", "Sandra Milena Jaramillo", "Héctor Fabio Muñoz", "Beatriz Elena Ortiz",
            "César Augusto Rojas", "Adriana María Cardona", "Iván Darío Bedoya", "Mónica Andrea Henao",
            "Jaime Alberto Montoya", "Angela María Villegas", "Rubén Darío Herrera", "Paola Andrea Toro",
            "Jorge Iván Giraldo", "Patricia Elena Correa", "William Orlando Marín", "Yolanda Andrea Serna",
            "Alexander Felipe Betancur", "Consuelo María Agudelo", "Walter Antonio Ramírez", "Nelly Lucía Yepes",
            "Carlos Mario Arango", "Luz Marina Jiménez", "Henry Alberto Duque", "Dora Cecilia Osorio",
            "Alonso de Jesús Mesa", "Elvia María Zapata", "Gabriel Jaime Palacio", "Victoria Eugenia Hoyos",
            "Raúl Hernán Suárez", "Lucía Inés Martínez", "Hugo Hernán González", "Isabel Cristina Ruiz",
            "Nelson de Jesús Restrepo", "Clara Inés Gómez", "Luis Fernando Rivera", "Alicia María Peña",
            "René Andrés Cañas", "Silvia Elena Berrío"
        };

        for (int i = 0; i < nombresCompradores.length; i++) {
            String cedula = String.valueOf(500001 + i);
            if (compradorRepository.findByCedula(cedula).isEmpty()) {
                Comprador c = new Comprador();
                c.setCedula(cedula);
                c.setNombre(nombresCompradores[i]);
                c.setTelefono("315" + String.format("%07d", 1000000 + i));
                String correo = nombresCompradores[i].toLowerCase()
                        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                        .replace(" ", ".") + "@email.com";
                c.setCorreo(correo);
                c.setDireccion("Calle " + (10 + i) + " # " + (5 + i) + "-" + (20 + i) + ", Bogotá");
                compradorRepository.save(c);
            }
        }
        
        System.out.println(">>> SE SEMBRARON COMPRADORES DE PRUEBA Y CONTRATOS MULTI-PROPIEDAD EXITOSAMENTE (con 50 compradores adicionales).");
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

    private void registrarUsuarioSiNoExiste(String nombreCompleto, String correo, String contrasena, Role rol) {
        String correoNormalizado = correo.toLowerCase();
        if (usuarioRepository.findByCorreo(correoNormalizado).isEmpty()) {
            Usuario u = new Usuario();
            u.setNombreCompleto(nombreCompleto);
            u.setCorreo(correoNormalizado);
            u.setContrasenaHash(BCrypt.hashpw(contrasena, BCrypt.gensalt()));
            u.setRol(rol);
            u.setActivo(true);
            usuarioRepository.save(u);
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
