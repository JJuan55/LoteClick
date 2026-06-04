package com.almaros.loteclick.services;

import com.almaros.loteclick.models.Usuario;
import com.almaros.loteclick.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona sesiones opacas de backend para no depender del correo enviado por el frontend.
 * Las sesiones viven en memoria del servidor mientras la aplicación esté encendida.
 */
@Service
public class SessionService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public String crearSesion(Usuario usuario) {
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        sessions.put(token, new SessionData(usuario.getCorreo(), LocalDateTime.now()));
        return token;
    }

    public Usuario obtenerUsuarioAutenticado(String authorizationHeader, List<String> rolesPermitidos, String mensajeErrorRol) {
        String token = extraerBearerToken(authorizationHeader);
        SessionData sessionData = sessions.get(token);
        if (sessionData == null) {
            throw new IllegalStateException("La sesión no es válida o ha expirado. Inicie sesión nuevamente.");
        }

        Usuario usuario = usuarioRepository.findByCorreo(sessionData.correo())
                .orElseThrow(() -> new IllegalArgumentException("Usuario interno no encontrado."));

        if (usuario.getActivo() != null && !usuario.getActivo()) {
            throw new IllegalStateException("La cuenta del usuario autenticado se encuentra inactiva.");
        }

        String rol = usuario.getRol() != null ? usuario.getRol().getNombreRol() : null;
        if (rolesPermitidos != null && !rolesPermitidos.isEmpty() && (rol == null || !rolesPermitidos.contains(rol))) {
            throw new IllegalStateException(mensajeErrorRol);
        }

        return usuario;
    }

    private String extraerBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("El encabezado Authorization es obligatorio.");
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new IllegalArgumentException("El encabezado Authorization debe usar el esquema Bearer.");
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("El token de sesión es obligatorio.");
        }
        return token;
    }

    private record SessionData(String correo, LocalDateTime fechaCreacion) {
    }
}
