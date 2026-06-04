package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "distribuciones_socios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionSocio {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "socio_id", nullable = false)
    private SocioProyecto socio;

    @ManyToOne
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_distribucion", nullable = false)
    private LocalDate fechaDistribucion;

    @Column(name = "referencia", length = 160)
    private String referencia;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
