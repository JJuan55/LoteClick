name: arquitectura-spring
description: "Aplica los patrones de desarrollo, seguridad e integridad para el backend de LoteClick en Spring Boot con Supabase."

# Reglas de Desarrollo Backend
1. [cite_start]Todos los controladores que atiendan al frontend deben usar `@RestController` y mapearse bajo la ruta base `/api/`.
2. [cite_start]La persistencia de datos debe mapear con total exactitud las entidades y tipos de datos del script DDL de Supabase provisto en CONTEXT.md.
3. [cite_start]Las contraseñas en la tabla `usuarios` se deben encriptar estrictamente con la librería de BCrypt de Spring Security antes de persistirse.
4. [cite_start]Todo método de servicio que registre ventas, altere cuotas de amortización o guarde egresos debe llevar la anotación `@Transactional` para asegurar la atomicidad e integridad de la base de datos.
5. Queda terminantemente prohibido incorporar dependencias o lógica de pasarelas de pago online o gestión de pagos en especie. [cite_start]Todo se procesa de forma manual y en dinero real.
