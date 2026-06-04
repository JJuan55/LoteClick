package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'pagos_ingresos' en Supabase.
 * Registra los abonos financieros realizados sobre las cuotas mensuales.
 */
@Entity
@Table(name = "pagos_ingresos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cuota_id", nullable = false)
    private CuotaAmortizacion cuota;

    @ManyToOne
    @JoinColumn(name = "venta_id", nullable = false)
    private VentaContrato venta;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    @Column(name = "concepto", nullable = false, length = 50)
    private String concepto;

    @Column(name = "url_pdf_recibo", length = 255)
    private String urlPdfRecibo;
}
