package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad Egreso.
 */
@Repository
public interface EgresoRepository extends JpaRepository<Egreso, UUID> {

    /**
     * Obtiene egresos registrados en un rango de fechas ordenados de forma cronológica ascendente.
     */
    List<Egreso> findByFechaEgresoBetweenOrderByFechaEgresoAsc(LocalDate start, LocalDate end);

    /**
     * Obtiene todos los egresos ordenados de forma cronológica descendente (más recientes primero).
     */
    List<Egreso> findAllByOrderByFechaEgresoDesc();

    @Query("SELECT COALESCE(SUM(e.monto), 0) FROM Egreso e")
    BigDecimal sumMonto();
}

