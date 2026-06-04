package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad Lote.
 */
@Repository
public interface LoteRepository extends JpaRepository<Lote, UUID> {

    /**
     * Obtiene el listado de lotes asociados a una etapa del proyecto.
     * @param etapaId ID de la etapa (1, 2, 3, 4)
     * @return Lista de lotes ordenados por su número de lote de forma ascendente.
     */
    List<Lote> findByEtapaIdOrderByNumeroLoteAsc(Integer etapaId);

    /**
     * Busca un lote específico por su número y el ID de su etapa.
     */
    java.util.Optional<Lote> findByNumeroLoteAndEtapaId(Integer numeroLote, Integer etapaId);
}
