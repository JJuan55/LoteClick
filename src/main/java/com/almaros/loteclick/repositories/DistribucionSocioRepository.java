package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.DistribucionSocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DistribucionSocioRepository extends JpaRepository<DistribucionSocio, UUID> {
    List<DistribucionSocio> findByFechaDistribucionBetweenOrderByFechaDistribucionDesc(LocalDate start, LocalDate end);
    List<DistribucionSocio> findAllByOrderByFechaDistribucionDesc();
}
