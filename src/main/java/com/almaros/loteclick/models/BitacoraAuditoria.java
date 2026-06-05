package com.almaros.loteclick.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que modela el log inalterable de auditoría interna.
 * Registra quién ejecutó cada acción financiera o de configuración crítica.
 */
@Entity
@Table(name = "bitacora_auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "usuario_correo", nullable = false, length = 120)
    private String usuarioCorreo;

    @Column(name = "usuario_nombre", nullable = false, length = 120)
    private String usuarioNombre;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "detalles", nullable = false, columnDefinition = "TEXT")
    private String detalles;
}
