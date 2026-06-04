package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.AporteInversionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AporteInversionistaRepository extends JpaRepository<AporteInversionista, UUID> {
    List<AporteInversionista> findByFechaAporteBetweenOrderByFechaAporteDesc(LocalDate start, LocalDate end);
    List<AporteInversionista> findAllByOrderByFechaAporteDesc();
}
