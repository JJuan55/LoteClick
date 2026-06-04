package com.almaros.loteclick.repositories;

import com.almaros.loteclick.models.SocioProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SocioProyectoRepository extends JpaRepository<SocioProyecto, UUID> {
    List<SocioProyecto> findAllByOrderByNombreAsc();
    List<SocioProyecto> findAllByActivoTrueOrderByNombreAsc();
}
