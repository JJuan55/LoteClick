package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio JPA para operaciones sobre la entidad Etapa.
 */
@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Integer> {

    /**
     * Busca una etapa por su nombre.
     * @param nombreEtapa Nombre (ej: Etapa 1)
     * @return Un Optional con la Etapa si existe
     */
    Optional<Etapa> findByNombreEtapa(String nombreEtapa);
}
