package com.almaros.loteclick.controllers;

import com.almaros.loteclick.models.Usuario;
import com.almaros.loteclick.repositories.UsuarioRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para la autenticación y control de accesos.
 * Expone el endpoint POST /api/auth/login.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Endpoint para autenticar a un usuario interno.
     * @param request JSON con correo y contrasena
     * @return Datos del usuario y su rol en caso de éxito, o un mensaje de error.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Validación de campos obligatorios
        if (request.getCorreo() == null || request.getCorreo().trim().isEmpty() ||
            request.getContrasena() == null || request.getContrasena().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El correo y la contraseña son requeridos"));
        }

        // Buscar el usuario en la base de datos por correo
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(request.getCorreo().trim().toLowerCase());

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas. Intente nuevamente"));
        }

        Usuario usuario = usuarioOpt.get();

        // Validar si el usuario está activo (CU01 Excepción B)
        if (usuario.getActivo() != null && !usuario.getActivo()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cuenta deshabilitada. Contacte al Administrador"));
        }

        // Validar contraseña con BCrypt (CU01 Excepción A)
        try {
            if (!BCrypt.checkpw(request.getContrasena(), usuario.getContrasenaHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciales incorrectas. Intente nuevamente"));
            }
        } catch (IllegalArgumentException e) {
            // Manejar hashes mal formados en base de datos para evitar fallos de ejecución
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno en el formato de credenciales persistidas."));
        }

        // Obtener el nombre del rol asociado
        String nombreRol = (usuario.getRol() != null) ? usuario.getRol().getNombreRol() : "SIN_ROL";

        // Retornar JSON de éxito
        return ResponseEntity.ok(Map.of(
                "nombreCompleto", usuario.getNombreCompleto(),
                "correo", usuario.getCorreo(),
                "rol", nombreRol
        ));
    }

    /**
     * Clase DTO para capturar la petición de login.
     */
    @Data
    public static class LoginRequest {
        private String correo;
        private String contrasena;
    }
}
