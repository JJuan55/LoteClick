package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.CuotaAmortizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad CuotaAmortizacion.
 */
@Repository
public interface CuotaAmortizacionRepository extends JpaRepository<CuotaAmortizacion, UUID> {

    /**
     * Busca las cuotas de amortización asociadas a una venta ordenadas por su número de cuota.
     * @param ventaId UUID de la venta.
     * @return Lista de cuotas ordenadas de forma ascendente.
     */
    List<CuotaAmortizacion> findByVentaIdOrderByNumeroCuotaAsc(UUID ventaId);
}
