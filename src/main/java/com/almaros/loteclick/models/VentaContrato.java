package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'ventas_contratos' en Supabase.
 * Modela el acuerdo de venta, plazos y documentos.
 */
@Entity
@Table(name = "ventas_contratos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "lote_id", nullable = false, unique = true)
    private Lote lote;

    @ManyToOne
    @JoinColumn(name = "comprador_id", nullable = false)
    private Comprador comprador;

    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Column(name = "precio_venta_pactado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVentaPactado;

    @Column(name = "cuota_separacion", nullable = false, precision = 12, scale = 2)
    private BigDecimal cuotaSeparacion;

    @Column(name = "plazo_meses", nullable = false)
    private Integer plazoMeses;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    @Column(name = "url_pdf_contrato", length = 255)
    private String urlPdfContrato;

    @Column(name = "url_pdf_propiedad", length = 255)
    private String urlPdfPropiedad;
}
