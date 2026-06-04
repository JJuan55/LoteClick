package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'egresos' en Supabase.
 * Registra las salidas de dinero contables clasificadas por rubros operativos.
 */
@Entity
@Table(name = "egresos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Egreso {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "contador_id", nullable = false)
    private Usuario contador;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_egreso", nullable = false)
    private LocalDate fechaEgreso;

    @Column(name = "rubro", nullable = false, length = 50)
    private String rubro; // MAQUINARIA, SUMINISTRO_AGUA, SERVICIOS_PUBLICOS, EXCAVACION, OTROS

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
