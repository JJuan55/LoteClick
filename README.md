# LoteClick

Sistema web para la gestión comercial y financiera del proyecto inmobiliario **Mirador de San Antonio** de **ALMAROS**. La aplicación permite iniciar sesión por rol, consultar inventario de lotes, registrar compradores, crear ventas, gestionar cartera y registrar egresos.

## Stack

- Java 21
- Spring Boot 3.3
- Maven 3.8+
- PostgreSQL en Supabase
- Frontend estático servido por Spring Boot (`HTML`, `Bootstrap`, `JavaScript`)

## Requisitos

- `Java 21`
- `Maven 3.8` o superior
- Acceso a internet para descargar dependencias de Maven
- Conectividad a la base de datos configurada en `src/main/resources/application.properties`

## Ejecución rápida

1. Clona el repositorio y entra a la carpeta del proyecto.
2. Ejecuta:

```bash
mvn spring-boot:run
```

3. Abre en el navegador:

```text
http://localhost:8082
```

El puerto está definido actualmente en `server.port=8082`.

## Compilar el proyecto

```bash
mvn clean package
```

Si solo quieres validar compilación sin pruebas:

```bash
mvn -DskipTests package
```

## Qué se siembra automáticamente

Al iniciar, la clase `DatabaseInitializer` crea datos demo si la base está vacía:

- Roles: `ADMINISTRADOR`, `CONTADOR`, `VENDEDOR`
- 4 etapas
- 18 lotes por etapa
- Compradores de prueba
- Contratos y cuotas de amortización de ejemplo

## Usuarios de prueba

Estos usuarios se crean automáticamente si la tabla `usuarios` está vacía:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@almaros.com` | `admin123` |
| Contador | `contador@almaros.com` | `contador123` |
| Vendedor | `vendedor@almaros.com` | `vendedor123` |
| Usuario inactivo | `inactivo@almaros.com` | `inactivo123` |

El usuario inactivo sirve para probar el mensaje de cuenta deshabilitada.

## Compradores de prueba

También quedan sembrados estos compradores:

| Nombre | Cédula | Correo |
|---|---|---|
| Juan Valdez | `111222` | `juan.valdez@email.com` |
| María Cardona | `987654` | `maria.cardona@email.com` |

Son útiles para probar la pantalla de cartera y el estado de cuenta.

## Cómo visualizar y probar la app

### Login

- URL: `http://localhost:8082`
- El login consume `POST /api/auth/login`

### Redirección por rol

- `ADMINISTRADOR` -> `dashboard.html`
- `CONTADOR` -> `egresos.html`
- `VENDEDOR` -> `inventario.html`

### Flujo sugerido de prueba

1. Inicia sesión como `vendedor@almaros.com`.
2. Entra a `Inventario Lotes` y abre un lote `DISPONIBLE`.
3. Registra o busca un comprador.
4. Finaliza una venta.
5. La app redirige a `cartera.html` para revisar cuotas.
6. Busca por cédula `111222` o `987654`.
7. Registra un abono y verifica el comprobante generado.
8. Inicia sesión como `contador@almaros.com`.
9. Entra a `Egresos` y registra un gasto operativo.
10. Usa el botón `Restablecer BD` para volver al estado semilla.

## Pantallas principales

- `index.html`: inicio de sesión
- `dashboard.html`: vista general financiera
- `inventario.html`: lotes por etapa y proceso de venta
- `cartera.html`: estado de cuenta, cuotas y recibos
- `egresos.html`: registro y liquidación de egresos

## Archivos generados

Los contratos, recibos y documentos de propiedad se guardan así:

- Preferiblemente en Supabase Storage, si existe `supabase.key`
- Si no, en fallback local:
  - `src/main/resources/static/uploads`
  - `target/classes/static/uploads`

Luego quedan accesibles desde URLs tipo:

```text
/uploads/nombre-del-archivo
```

## Endpoints principales

- `POST /api/auth/login`
- `GET /api/lotes?etapaId=1`
- `GET /api/compradores/buscar/{cedula}`
- `POST /api/compradores`
- `POST /api/ventas`
- `GET /api/clientes/{cedula}/estado-cuenta`
- `POST /api/pagos`
- `POST /api/egresos`
- `GET /api/egresos/liquidar`
- `POST /api/test/reset`

## Configuración actual

La conexión a PostgreSQL/Supabase está definida directamente en:

- [src/main/resources/application.properties](/home/david/Documentos/LoteClick/src/main/resources/application.properties)

Hoy el repositorio ya trae `url`, `username` y `password` de base de datos embebidos. Eso permite correr el proyecto más rápido, pero no es una práctica recomendable para producción. Lo correcto sería mover esas credenciales a variables de entorno o a un archivo local no versionado.

## Estructura útil del proyecto

```text
src/main/java/com/almaros/loteclick/
  config/
  controllers/
  models/
  repositories/
  services/

src/main/resources/
  application.properties
  static/
```

## Observaciones

- La aplicación depende de la base de datos remota configurada en Supabase.
- El frontend usa `sessionStorage` para conservar la sesión del usuario.
- El botón `Restablecer BD` está disponible en vistas operativas para volver a los datos demo.
- Hay archivos de ejemplo ya presentes en `src/main/resources/static/uploads`.
