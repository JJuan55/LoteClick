package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.Comprador;
import com.almaros.loteclick.repositories.CompradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para la gestión de la ficha de compradores.
 * Expone endpoints para buscar y registrar compradores.
 */
@RestController
@RequestMapping("/api/compradores")
@CrossOrigin(origins = "*")
public class CompradorController {

    @Autowired
    private CompradorRepository compradorRepository;

    /**
     * Busca un comprador por su número de cédula.
     * @param cedula Cédula del comprador.
     * @return El comprador si existe, o 404 Not Found si es nuevo.
     */
    @GetMapping("/buscar/{cedula}")
    public ResponseEntity<?> buscarPorCedula(@PathVariable String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cédula es obligatoria"));
        }
        
        Optional<Comprador> compradorOpt = compradorRepository.findByCedula(cedula.trim());
        if (compradorOpt.isPresent()) {
            return ResponseEntity.ok(compradorOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Comprador no encontrado", "nuevo", true));
        }
    }

    /**
     * Registra un nuevo comprador en la base de datos de manera pasiva.
     * @param comprador Datos del comprador.
     * @return Comprador registrado con su UUID recién creado.
     */
    @PostMapping
    public ResponseEntity<?> registrarComprador(@RequestBody Comprador comprador) {
        // Validar campos obligatorios
        if (comprador.getCedula() == null || comprador.getCedula().trim().isEmpty() ||
            comprador.getNombre() == null || comprador.getNombre().trim().isEmpty() ||
            comprador.getTelefono() == null || comprador.getTelefono().trim().isEmpty() ||
            comprador.getCorreo() == null || comprador.getCorreo().trim().isEmpty() ||
            comprador.getDireccion() == null || comprador.getDireccion().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos de la Ficha del Comprador son obligatorios (Cédula, Nombre, Teléfono, Correo y Dirección)."));
        }

        // Validar formato del correo
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!comprador.getCorreo().trim().matches(emailRegex)) {
            return ResponseEntity.badRequest().body(Map.of("error", "El formato del correo electrónico ingresado es inválido."));
        }

        // Validar si la cédula ya existe
        Optional<Comprador> duplicado = compradorRepository.findByCedula(comprador.getCedula().trim());
        if (duplicado.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Esta cédula de comprador ya se encuentra registrada"));
        }

        // Limpiar espacios en los campos
        comprador.setCedula(comprador.getCedula().trim());
        comprador.setNombre(comprador.getNombre().trim());
        if (comprador.getTelefono() != null) comprador.setTelefono(comprador.getTelefono().trim());
        if (comprador.getDireccion() != null) comprador.setDireccion(comprador.getDireccion().trim());
        if (comprador.getCorreo() != null) comprador.setCorreo(comprador.getCorreo().trim().toLowerCase());

        Comprador guardado = compradorRepository.save(comprador);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }
}
