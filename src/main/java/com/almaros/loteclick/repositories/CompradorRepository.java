package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.Comprador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad Comprador.
 */
@Repository
public interface CompradorRepository extends JpaRepository<Comprador, UUID> {

    List<Comprador> findAllByOrderByNombreAsc();

    /**
     * Busca un comprador por su número de cédula.
     * @param cedula Cédula del comprador.
     * @return Un Optional con el Comprador si existe.
     */
    Optional<Comprador> findByCedula(String cedula);
}
