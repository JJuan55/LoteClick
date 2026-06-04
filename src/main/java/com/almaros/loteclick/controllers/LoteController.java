package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.Lote;
import com.almaros.loteclick.repositories.LoteRepository;
import com.almaros.loteclick.services.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar el inventario de lotes.
 * Expone el endpoint GET /api/lotes.
 */
@RestController
@RequestMapping("/api/lotes")
@CrossOrigin(origins = "*")
public class LoteController {

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private SessionService sessionService;

    /**
     * Endpoint para consultar los lotes.
     * Si se pasa el parámetro opcional etapaId, filtra por esa etapa.
     * De lo contrario, devuelve todos los lotes.
     * @param etapaId ID de la etapa (opcional)
     * @return Lista de lotes
     */
    @GetMapping
    public ResponseEntity<?> listarLotes(@RequestHeader("Authorization") String authorizationHeader,
                                         @RequestParam(required = false) Integer etapaId) {
        try {
            sessionService.obtenerUsuarioAutenticado(
                    authorizationHeader,
                    List.of("VENDEDOR", "CONTADOR", "ADMINISTRADOR"),
                    "No tiene permisos para consultar el inventario de lotes."
            );
            List<Lote> lotes;
            if (etapaId != null) {
                lotes = loteRepository.findByEtapaIdOrderByNumeroLoteAsc(etapaId);
            } else {
                lotes = loteRepository.findAll();
            }
            return ResponseEntity.ok(lotes);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
