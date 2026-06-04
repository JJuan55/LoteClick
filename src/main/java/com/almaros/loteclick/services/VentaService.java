package com.almaros.loteclick.services;

import com.almaros.loteclick.models.*;
import com.almaros.loteclick.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de la lógica transaccional de registro de ventas,
 * generación de cuotas de amortización y mutación del estado del lote.
 */
@Service
public class VentaService {

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private CompradorRepository compradorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VentaContratoRepository ventaContratoRepository;

    @Autowired
    private CuotaAmortizacionRepository cuotaAmortizacionRepository;

    @Autowired
    private StorageService storageService;

    /**
     * Registra una venta completa en forma atómica.
     * Genera amortización y guarda los contratos en el Storage.
     */
    @Transactional(rollbackFor = Exception.class)
    public VentaContrato registrarVenta(UUID loteId, UUID compradorId, String vendedorCorreo, 
                                        BigDecimal precioVentaPactado, BigDecimal cuotaSeparacion, 
                                        Integer plazoMeses, LocalDate fechaVenta, 
                                        MultipartFile documentoPropiedad) throws IOException {
        
        // 1. Validar existencia y disponibilidad del lote
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado"));

        if (!lote.getEstado().equalsIgnoreCase("DISPONIBLE")) {
            throw new IllegalStateException("Error: El lote ya no se encuentra disponible");
        }

        // 2. Obtener comprador
        Comprador comprador = compradorRepository.findById(compradorId)
                .orElseThrow(() -> new IllegalArgumentException("Comprador no encontrado"));

        // 3. Obtener vendedor (usuario por correo)
        Usuario vendedor = usuarioRepository.findByCorreo(vendedorCorreo.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));

        if (vendedor.getActivo() != null && !vendedor.getActivo()) {
            throw new IllegalStateException("La cuenta del usuario autenticado se encuentra inactiva.");
        }

        String rol = vendedor.getRol() != null ? vendedor.getRol().getNombreRol() : null;
        if (rol == null || !List.of("VENDEDOR", "ADMINISTRADOR").contains(rol)) {
            throw new IllegalStateException("No tiene permisos para registrar ventas.");
        }

        // 4. Subir documento de propiedad
        String urlPropiedad = "";
        if (documentoPropiedad != null && !documentoPropiedad.isEmpty()) {
            urlPropiedad = storageService.guardarDocumentoPropiedad(documentoPropiedad);
        }

        // 5. Generar contrato PDF simulado
        String urlContrato = storageService.generarContratoPdfSimulado(
                lote.getNumeroLote().toString(),
                lote.getEtapa() != null ? lote.getEtapa().getNombreEtapa() : "Etapa N/A",
                comprador.getNombre(),
                comprador.getCedula(),
                precioVentaPactado.toString(),
                cuotaSeparacion.toString(),
                plazoMeses
        );

        // 6. Crear y registrar contrato de venta
        VentaContrato venta = new VentaContrato();
        venta.setLote(lote);
        venta.setComprador(comprador);
        venta.setVendedor(vendedor);
        venta.setPrecioVentaPactado(precioVentaPactado);
        venta.setCuotaSeparacion(cuotaSeparacion);
        venta.setPlazoMeses(plazoMeses);
        venta.setFechaVenta(fechaVenta);
        venta.setUrlPdfContrato(urlContrato);
        venta.setUrlPdfPropiedad(urlPropiedad);

        VentaContrato ventaGuardada = ventaContratoRepository.save(venta);

        // 7. Mutar el estado del lote a "SEPARADO"
        lote.setEstado("SEPARADO");
        loteRepository.save(lote);

        // 8. Calcular amortización y registrar cuotas
        BigDecimal saldo = precioVentaPactado.subtract(cuotaSeparacion);
        
        if (saldo.compareTo(BigDecimal.ZERO) > 0 && plazoMeses > 0) {
            BigDecimal plazoDec = BigDecimal.valueOf(plazoMeses);
            // Redondear cuota mensual base a pesos enteros (sin centavos para COP)
            BigDecimal cuotaMensualBase = saldo.divide(plDec(plazoMeses), 0, RoundingMode.HALF_UP);
            
            List<CuotaAmortizacion> cuotas = new ArrayList<>();
            BigDecimal sumaCuotasParciales = BigDecimal.ZERO;

            for (int i = 1; i <= plazoMeses; i++) {
                CuotaAmortizacion cuota = new CuotaAmortizacion();
                cuota.setVenta(ventaGuardada);
                cuota.setNumeroCuota(i);
                cuota.setFechaVencimiento(fechaVenta.plusMonths(i));
                cuota.setEstadoPago("PENDIENTE");

                // Asignar monto a la cuota
                if (i < plazoMeses) {
                    cuota.setMontoCuota(cuotaMensualBase);
                    sumaCuotasParciales = sumaCuotasParciales.add(cuotaMensualBase);
                } else {
                    // La última cuota ajusta diferencias por redondeo
                    BigDecimal ultimaCuota = saldo.subtract(sumaCuotasParciales);
                    cuota.setMontoCuota(ultimaCuota);
                }
                
                cuotas.add(cuota);
            }
            
            cuotaAmortizacionRepository.saveAll(cuotas);
        }

        System.out.println(">>> VENTA REGISTRADA EXITOSAMENTE para Lote " + lote.getNumeroLote() + " - Comprador " + comprador.getNombre());
        return ventaGuardada;
    }

    private BigDecimal plDec(int p) {
        return BigDecimal.valueOf(p);
    }
}
