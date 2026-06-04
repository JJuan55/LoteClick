package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio JPA para operaciones sobre la entidad Role.
 */
@Repository
public interface RolRepository extends JpaRepository<Role, Integer> {
    
    /**
     * Busca un rol por su nombre corporativo.
     * @param nombreRol Nombre del rol (ej: ADMINISTRADOR, VENDEDOR, CONTADOR)
     * @return Un Optional con el Rol si existe
     */
    Optional<Role> findByNombreRol(String nombreRol);
}
