package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.PagoIngreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad PagoIngreso.
 */
@Repository
public interface PagoIngresoRepository extends JpaRepository<PagoIngreso, UUID> {

    /**
     * Obtiene todos los pagos asociados a un contrato de venta.
     */
    List<PagoIngreso> findByVentaId(UUID ventaId);

    /**
     * Obtiene el pago asociado a una cuota de amortización específica.
     */
    List<PagoIngreso> findByCuotaId(UUID cuotaId);

    @Query("SELECT COALESCE(SUM(p.montoPagado), 0) FROM PagoIngreso p")
    BigDecimal sumMontoPagado();
}

