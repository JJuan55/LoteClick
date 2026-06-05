package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.BitacoraAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BitacoraAuditoriaRepository extends JpaRepository<BitacoraAuditoria, UUID> {
    List<BitacoraAuditoria> findAllByOrderByFechaHoraDesc();
}
