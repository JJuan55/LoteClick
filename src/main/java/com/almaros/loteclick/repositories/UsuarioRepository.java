package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca un usuario por su correo electrónico corporativo.
     * @param correo Correo electrónico del empleado
     * @return Un Optional con el Usuario si existe
     */
    Optional<Usuario> findByCorreo(String correo);
}
