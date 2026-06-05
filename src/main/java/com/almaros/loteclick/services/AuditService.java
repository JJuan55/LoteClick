package com.almaros.loteclick.services;

import com.almaros.loteclick.models.BitacoraAuditoria;
import com.almaros.loteclick.models.Usuario;
import com.almaros.loteclick.repositories.BitacoraAuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private BitacoraAuditoriaRepository bitacoraAuditoriaRepository;

    /**
     * Registra un log de auditoría en la base de datos.
     *
     * @param usuario  Usuario interno que realiza la acción
     * @param accion   Acción ejecutada (ej: CREAR_USUARIO, ELIMINAR_EGRESO)
     * @param detalles Detalles descriptivos de la acción
     */
    public void registrarAccion(Usuario usuario, String accion, String detalles) {
        BitacoraAuditoria log = new BitacoraAuditoria();
        log.setFechaHora(LocalDateTime.now());
        log.setUsuarioCorreo(usuario.getCorreo());
        log.setUsuarioNombre(usuario.getNombreCompleto());
        log.setRol(usuario.getRol() != null ? usuario.getRol().getNombreRol() : "SIN_ROL");
        log.setAccion(accion);
        log.setDetalles(detalles);
        bitacoraAuditoriaRepository.save(log);
    }
}
