package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.VentaContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para operaciones sobre la entidad VentaContrato.
 */
@Repository
public interface VentaContratoRepository extends JpaRepository<VentaContrato, UUID> {

    /**
     * Busca el contrato de venta activo para un lote.
     * @param loteId UUID del lote.
     * @return Optional con el contrato si existe.
     */
    Optional<VentaContrato> findByLoteId(UUID loteId);

    /**
     * Busca los contratos asociados a un comprador.
     * @param compradorId UUID del comprador.
     * @return Lista de contratos de venta.
     */
    List<VentaContrato> findByCompradorId(UUID compradorId);

    List<VentaContrato> findByVendedorId(UUID vendedorId);

    @Query("SELECT COALESCE(SUM(v.precioVentaPactado), 0) FROM VentaContrato v")
    BigDecimal sumPrecioVentaPactado();

    @Query("SELECT COALESCE(SUM(v.cuotaSeparacion), 0) FROM VentaContrato v")
    BigDecimal sumCuotaSeparacion();
}

