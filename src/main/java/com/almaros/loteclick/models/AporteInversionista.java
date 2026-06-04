package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "aportes_inversionistas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AporteInversionista {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre_inversionista", nullable = false, length = 120)
    private String nombreInversionista;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_aporte", nullable = false)
    private LocalDate fechaAporte;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;
}
