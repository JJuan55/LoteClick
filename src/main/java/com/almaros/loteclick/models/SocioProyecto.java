package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "socios_proyecto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocioProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "porcentaje_participacion", precision = 5, scale = 2)
    private BigDecimal porcentajeParticipacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
