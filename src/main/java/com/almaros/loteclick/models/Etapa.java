package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que mapea la tabla 'etapas' en Supabase.
 * Clasifica los lotes por zonas o etapas de parcelación (Etapa 1, 2, 3, 4).
 */
@Entity
@Table(name = "etapas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Etapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_etapa", unique = true, nullable = false, length = 50)
    private String nombreEtapa;
}
