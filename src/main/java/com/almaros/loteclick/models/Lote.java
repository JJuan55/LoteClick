package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que mapea la tabla 'lotes' en Supabase.
 * Almacena la información técnica de las parcelas del proyecto Mirador de San Antonio.
 */
@Entity
@Table(
    name = "lotes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero_lote", "etapa_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero_lote", nullable = false)
    private Integer numeroLote;

    @ManyToOne
    @JoinColumn(name = "etapa_id", nullable = false)
    private Etapa etapa;

    @Column(name = "area_m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "DISPONIBLE"; // DISPONIBLE, SEPARADO, VENDIDO
}
