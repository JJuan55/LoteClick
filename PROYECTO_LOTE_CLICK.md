# PROYECTO LOTE CLICK — Mirador de San Antonio (ALMAROS)

---

## 1. Contexto de la Aplicación y Objetivos

### Contexto

La empresa **ALMAROS** gestiona la parcelación del proyecto inmobiliario **"Mirador de San Antonio"**. Actualmente, toda la operación crítica (trazabilidad de compradores, asignación de lotes, cuotas de separación, plazos de pago y balances de utilidad frente a egresos como maquinaria o servicios) se controla manualmente en libros de Excel. Esto genera riesgos de pérdida de datos, errores de digitación manual y falta de un registro histórico centralizado de contratos y comprobantes de pago.

### Objetivo General

Desarrollar un sistema web seguro y responsive que centralice y automatice la gestión comercial y financiera del proyecto de parcelación "Mirador de San Antonio", permitiendo el control estricto del estado de los lotes, la recaudación de pagos por cuotas de compradores, el registro de egresos por rubros operativos y la visualización en tiempo real de los estados financieros de la empresa.

### Objetivos Específicos

- **Gestionar el Inventario Inmobiliario:** Permitir el registro técnico de los lotes clasificados por Etapas (1, 2, 3, 4), almacenando su área en m², precio de venta base y estado de disponibilidad.

- **Controlar el Ciclo de Venta y Recaudo:** Registrar los contratos de compra de lotes asociados a compradores, permitiendo diferir el pago en plazos (meses), capturar la cuota de separación y amortizar los pagos subsecuentes con descarga de comprobantes en PDF.

- **Administrar el Flujo de Caja Total:** Registrar los ingresos (recaudos ordinarios y extraordinarios) y clasificar los egresos del proyecto por rubros específicos (maquinaria, agua, excavación, servicios públicos, etc.).

- **Proveer Analítica Financiera Interna:** Generar vistas consolidadas y tableros que reflejen las Ventas Efectivas, el Total Recaudado, el Pendiente por Recaudar y la Utilidad neta del proyecto.

---

## 2. Roles de Usuario

El **Comprador** en el sistema es una **entidad pasiva** (un registro en la base de datos asociado a un lote y a sus pagos). Los únicos que operan el sistema son el personal interno.

| Rol | Descripción |
|-----|-------------|
| **Administrador (Gerencia)** | Acceso total. Modifica parámetros del proyecto, aprueba contratos, consulta la auditoría y visualiza los reportes macro de utilidades y aportes de socios. |
| **Contador** | Encargado del Módulo de Ingresos y Egresos. Registra y clasifica los egresos por rubros, concilia los pagos de lotes y vigila los balances financieros. |
| **Vendedor (Asesor Comercial)** | Encargado de la operación diaria. Consulta disponibilidad de lotes, registra compradores, crea ventas/contratos, registra pagos de cuotas y descarga comprobantes PDF. |

---

## 3. Casos de Uso

### Vendedor

- **Consultar Inventario de Lotes:** Filtrar lotes por Etapa, tamaño (m²) o Estado (Disponible, Separado con Abono, Pagado por Completo).
- **Registrar / Actualizar Comprador:** Crear la ficha de datos básicos del cliente (Nombre, Cédula, Teléfono, Correo).
- **Ejecutar Contrato de Venta:** Seleccionar un lote disponible, asociarlo al comprador, definir el plazo en meses y registrar el pago de la cuota de separación.
- **Registrar Pago de Cuota Mensual:** Buscar al comprador, seleccionar su lote en proceso de pago, ingresar el monto abonado en el periodo y guardarlo.
- **Descargar Comprobante de Pago:** Generar el archivo PDF del recibo recién capturado para el cliente.

### Contador

- **Registrar Egreso Operativo:** Ingresar salidas de dinero asociándolas a un rubro específico (Maquinaria, Compra de Agua, Servicios Públicos, Excavación, Nómina, etc.).
- **Visualizar Registro General de Ingresos:** Monitorear el listado cronológico de todos los pagos que han ingresado por concepto de venta de lotes o aportes.
- **Consultar Reportes de Cobranza y Saldos:** Ver listas de clientes al día (verde) o atrasados/con saldo pendiente (naranja/rojo) para gestión de cartera.

### Administrador

> Tiene permiso de ejecutar todas las acciones de Vendedores y Contadores, más:

- **Gestionar Usuarios:** Crear, editar o dar de baja cuentas de acceso de Vendedores y Contadores.
- **Visualizar Tablero de Control Financiero (Dashboard):** Cálculo en tiempo real de *Ventas Efectivas*, *Total Recaudado*, *Pendiente de Recaudo* e *Indicadores de Utilidad* neta del proyecto.
- **Controlar Aportes de Socios:** Módulo exclusivo para registrar capital inicial de los inversionistas y calcular su participación porcentual automática.
- **Consultar Logs de Auditoría:** Verificar qué usuario realizó modificaciones delicadas (alterar precio de un lote, eliminar un egreso, etc.).

---

## 4. Implementación de los Casos de Uso

### Módulo 0: Autenticación y Control de Accesos

#### CU01: Autenticación de Usuario (Login)

- **Actor:** Administrador, Contador o Vendedor.
- **Descripción:** Permite a los usuarios internos ingresar de manera segura a la plataforma según sus privilegios.

**Flujo Exitoso:**
1. El usuario ingresa a la app y es recibido por la **Ventana de Login**.
2. Digita su Correo Electrónico y Contraseña corporativa, y presiona "Iniciar Sesión".
3. El backend valida las credenciales en Supabase de forma encriptada.
4. El backend extrae el rol activo del usuario.
5. Se genera un token de sesión seguro.
6. **Redirección por rol:**
   - Administrador → **Dashboard Financiero Global**
   - Contador → **Control de Egresos e Ingresos**
   - Vendedor → **Inventario de Lotes por Etapa**

**Excepciones:**
- **Excepción A (Credenciales incorrectas):** Alerta en rojo: *"Credenciales incorrectas. Intente nuevamente"*. El usuario permanece en el Login.
- **Excepción B (Usuario Inactivo):** Alerta: *"Cuenta deshabilitada. Contacte al Administrador"*. El usuario permanece en el Login.

---

#### CU02: Crear y Asignar Usuario (Exclusivo Administrador)

- **Actor:** Administrador.
- **Descripción:** Permite dar de alta al personal que operará el sistema. No hay registro público.

**Flujo Exitoso:**
1. El Administrador navega a la **Ventana de Gestión de Usuarios** y presiona "Agregar Nuevo Empleado".
2. Llena: Nombre Completo, Correo Corporativo, Contraseña Temporal y Rol (Vendedor o Contador).
3. Presiona "Guardar Usuario".
4. El backend valida e inserta el registro en Supabase.
5. Se escribe en la auditoría: *Administrador [Nombre] creó al usuario [Correo] con rol [Rol]*.
6. Muestra mensaje de éxito y recarga la tabla de usuarios.

**Excepciones:**
- **Excepción A (Correo Duplicado):** Campo de correo se pinta en rojo con mensaje: *"Este correo electrónico ya se encuentra registrado"*. El formulario se mantiene abierto.

---

### Módulo 1: Gestión Comercial, Lotes y Ventas

#### CU03: Ejecutar Venta de Lote y Apertura de Contrato

- **Actor:** Vendedor.
- **Descripción:** Formalizar la venta de un lote disponible parametrizando los plazos de amortización y guardando el contrato legal perpetuo.

**Flujo Exitoso:**
1. El Vendedor entra a la **Ventana de Inventario de Lotes**, selecciona una Etapa y hace clic en un lote disponible.
2. Presiona "Iniciar Proceso de Venta". El sistema abre la **Ventana de Contrato de Venta**.
3. Digita la Cédula del comprador (si es nuevo, completa datos; si ya existía, se auto-completa).
4. Digita: *Cuota de Separación*, *Plazo en meses* y *Fecha de la venta*.
5. Presiona "Finalizar y Registrar Compra".
6. **El backend ejecuta una transacción atómica:**
   - Almacena/Actualiza los datos del Comprador.
   - Modifica el estado del lote a **"Separado con Abono"** o **"Vendido"**.
   - Registra un ingreso financiero automático por el valor de la cuota de separación.
   - Genera automáticamente la tabla de proyección de cuotas mensuales.
   - Genera el contrato en PDF, lo sube al **Bucket de Storage de Supabase** y guarda la URL vinculada a la transacción.
   - Guarda trazabilidad en auditoría con el ID del vendedor.
7. **Redirección:** Ventana de Resumen de Venta con botón permanente: 📄 *"Descargar Contrato de Venta (PDF)"*.

**Excepciones:**
- **Excepción A (Conflicto de Lote Ocupado):** Si otro asesor cerró el mismo lote mientras el formulario estaba abierto, el sistema cancela la operación con mensaje emergente: *"Error: El lote ya no se encuentra disponible"* y redirige al Inventario de Lotes actualizado.

---

#### CU04: Consultar Lotes Pagados y Vendidos

- **Actor:** Vendedor, Contador o Administrador.
- **Descripción:** Seguimiento y auditoría del estado actual de todos los lotes.

**Flujo Exitoso:**
1. El usuario ingresa a la **Ventana de Reporte de Inventario Inmobiliario**.
2. Selecciona filtros opcionales: *Etapa (1, 2, 3, 4)* o *Estado del Lote*.
3. Presiona "Filtrar".
4. El sistema realiza una consulta relacional (JOIN) cruzando etapas, lotes, compradores y contratos.
5. Renderiza una tabla dinámica con: *Etapa, Número de Lote, Área (m²), Comprador, Precio de Venta, Total Recaudado a la fecha y Estado*.
6. Cada fila incluye botón de acceso al **Detalle de la Venta** con descarga del PDF del contrato.

---

### Módulo 2: Gestión de Recaudos y Cartera

#### CU05: Registrar Pago de Cuota Mensual

- **Actor:** Vendedor o Contador.
- **Descripción:** Registra los abonos diarios de cuotas para amortizar la deuda del comprador, emitiendo un recibo físico descargable.

**Flujo Exitoso:**
1. El usuario entra a la **Ventana de Gestión de Cartera/Clientes** y busca al comprador por cédula.
2. El sistema despliega el **Estado de Cuenta del Cliente** con la lista de cuotas semaforizada:
   - 🟢 **Verde** = Pagado
   - 🟠 **Naranja/Rojo** = Pendiente/Vencido
3. El usuario presiona "Registrar Abono".
4. En un Modal flotante, digita: *Monto Recibido* y *Concepto de Pago* (Cuota ordinaria, Intereses por mora, Penalidad).
5. Hace clic en "Confirmar Pago".
6. **El backend:**
   - Añade el abono a la tabla de ingresos.
   - Actualiza el estado de la cuota a "Pagada" (Verde).
   - Recalcula los saldos pendientes del lote.
   - Actualiza los acumulados generales de recaudación.
   - Genera un recibo de caja en PDF con fecha, hora y token único de validación.
   - Escribe la acción en auditoría.
7. **Interfaz:** Cierra el modal, actualiza la lista semaforizada y **abre el PDF del comprobante en una pestaña nueva**.

**Excepciones:**
- **Excepción A (Monto Inválido):** Si se digitan letras, valores negativos o $0, el input se resalta en rojo con mensaje: *"El monto de abono debe ser un valor numérico superior a cero"*. El modal se mantiene abierto.

---

### Módulo 3: Control de Egresos Operativos

#### CU06: Registrar Egreso Operativo por Rubro

- **Actor:** Contador o Administrador.
- **Descripción:** Controla las salidas de dinero clasificadas por rubros operativos reales del proyecto.

**Flujo Exitoso:**
1. El usuario va a la **Ventana de Control de Egresos** y presiona "Registrar Nuevo Gasto".
2. Completa: *Fecha del Gasto*, *Monto en USD*, *Descripción detallada*.
3. Selecciona obligatoriamente el **Rubro de Egreso** del desplegable:
   - Maquinaria
   - Suministro de Agua
   - Servicios Públicos
   - Excavación
   - Otros
4. Presiona "Registrar Egreso".
5. El backend valida e inserta el egreso vinculado al rubro.
6. El motor financiero **resta automáticamente** el monto del cálculo global de utilidades.
7. Muestra aviso de éxito y actualiza la tabla cronológica con el nuevo registro en la primera fila.

**Excepciones:**
- **Excepción A (Rubro no seleccionado):** La casilla se resalta en rojo con aviso: *"Debe asignar este egreso a un rubro operativo válido"*. El flujo se mantiene detenido en el formulario.

---

#### CU07: Liquidar y Consultar Rubros de Egresos

- **Actor:** Contador o Administrador.
- **Descripción:** Informes de egresos agrupados por categorías operativas para auditoría de costos.

**Flujo Exitoso:**
1. El usuario ingresa a la **Ventana de Liquidación de Rubros**.
2. Configura rango de fechas y selecciona rubro específico o consolidado completo.
3. Presiona "Generar Informe".
4. El sistema renderiza tabla acumulada por rubros (ej: Total Maquinaria: $X USD, Total Excavación: $Y USD).
5. Permite exportar la vista o desglosar cada rubro para ver facturas y descripciones individuales.

---

### Módulo 4: Alta Dirección y Analítica Financiera

#### CU08: Visualizar Cuadro de Mando Financiero (Dashboard)

- **Actor:** Administrador o Contador.
- **Descripción:** Pantalla analítica principal que emula los recuadros del Excel de ALMAROS para toma de decisiones estratégicas.

**Flujo Exitoso:**
1. El usuario inicia sesión o hace clic en **Inicio / Dashboard** del menú lateral.
2. El backend ejecuta consultas sumatorias en tiempo real y renderiza:

| KPI | Descripción |
|-----|-------------|
| **Ventas Efectivas** | Suma total de precios pactados en contratos firmados |
| **Total Recaudado** | Suma total de dinero real ingresado a caja |
| **Pendiente de Recaudo** | Diferencia matemática por cobrar a clientes |
| **Utilidad Neta** | Total recaudado menos el consolidado de egresos por rubros |

3. Adicionalmente dibuja una tabla comparativa mensual de **Ingresos vs Egresos**.

---

#### CU09: Gestionar Aportes de Socios

- **Actor:** Exclusivo Administrador.
- **Descripción:** Controla el capital inyectado por los inversionistas y calcula su cuota de participación porcentual automática.

**Flujo Exitoso:**
1. El Administrador ingresa a la **Ventana de Aportes de Socios**.
2. El sistema muestra la lista de socios con sus aportes y porcentaje actual.
3. Para añadir capital, presiona "Registrar Inyección de Capital", selecciona al socio e introduce el monto en USD.
4. El backend registra el nuevo aporte en el histórico del socio.
5. **Recalcula instantáneamente** la participación porcentual de cada inversionista frente al nuevo total de capital inyectado.
6. La pantalla se actualiza con la tabla de socios con los nuevos valores y porcentajes.

---

## 5. Requisitos del Sistema

### A. Requerimientos Funcionales (RF)

| Código | Requerimiento Funcional | Descripción | CU Asociado |
|--------|------------------------|-------------|-------------|
| **RF01** | Autenticación Segura | Validar credenciales de usuarios internos contra la base de datos de manera encriptada. | CU01 |
| **RF02** | Control de Accesos (RBAC) | Restringir vistas, menús y permisos según el rol (Administrador, Contador, Vendedor). | CU01 |
| **RF03** | Registro Centralizado de Usuarios | Permitir al Administrador crear nuevos empleados asignándoles un rol, sin registro público. | CU02 |
| **RF04** | Filtro Jerárquico por Etapas | Listar el inventario de lotes segmentado por Etapa (1, 2, 3 o 4). | CU03 / CU04 |
| **RF05** | Gestión de Ficha de Compradores | Permitir el registro y consulta de datos personales básicos de los compradores. | CU03 / CU05 |
| **RF06** | Cierre de Venta y Amortización | Registrar la venta de un lote, calcular la tabla de cuotas mensuales fijas e iguales, y cambiar el estado del lote. | CU03 |
| **RF07** | Persistencia de Contrato e Historial | Compilar los datos de la venta en un PDF, subirlo al Storage y permitir su descarga perpetua. | CU03 / CU04 |
| **RF08** | Carga de Documento de Propiedad | Permitir al Vendedor subir y asociar el Documento de Propiedad digitalizado (PDF/Imagen) a la venta. | CU03 / CU04 |
| **RF09** | Recaudo de Cuotas en Dinero | Registrar abonos financieros manuales (en USD/COP) afectando el estado de cuenta de la cuota del cliente. | CU05 |
| **RF10** | Emisión Automatizada de Recibos | Tras cada recaudo exitoso, generar un comprobante de pago en PDF y abrirlo en una pestaña nueva. | CU05 |
| **RF11** | Control de Egresos por Rubros | Registrar los gastos de la empresa categorizados por: Maquinaria, Suministro de Agua, Servicios Públicos, Excavación u Otros. | CU06 |
| **RF12** | Reportes de Liquidación de Costos | Consolidar y filtrar los egresos acumulados agrupados por rubro en rangos de fechas específicos. | CU07 |
| **RF13** | Cuadro de Mando (Dashboard) | Calcular en tiempo real los KPIs: Ventas Efectivas, Total Recaudado, Pendiente de Recaudo y Utilidad Neta. | CU08 |
| **RF14** | Cálculo de Participación de Socios | Registrar aportes de capital de socios y computar automáticamente su porcentaje de participación. | CU09 |
| **RF15** | Bitácora de Auditoría Interna | Registrar automáticamente en un log inalterable qué usuario realizó cada inserción o modificación financiera. | Todos |

### B. Requerimientos No Funcionales (RNF)

| Código | Categoría | Requerimiento No Funcional |
|--------|-----------|---------------------------|
| **RNF01** | Seguridad | Las contraseñas deben almacenarse con algoritmos hash seguros (ej: BCrypt en Spring Boot). |
| **RNF02** | Usabilidad | La interfaz debe ser completamente *Responsive* para escritorio y tablets. |
| **RNF03** | Disponibilidad | La base de datos opera sobre infraestructura Cloud (Supabase PostgreSQL) con conectividad 24/7. |
| **RNF04** | Integridad | Toda transacción monetaria que altere tablas críticas debe ejecutarse de forma atómica (`@Transactional` en Spring Boot). |
| **RNF05** | Restricción de Alcance | El sistema procesará exclusivamente transacciones en dinero real, sin pasarelas de pago online ni registros en especie. |

---

## 6. Diseño de la Base de Datos Relacional (Supabase PostgreSQL)

> El número de lote (ej: "Lote 1") se repite en distintas etapas. Se usa un campo `id` tipo UUID como **Llave Primaria (PK)** y una **Restricción Única (UNIQUE)** combinando `etapa_id` y `numero_lote`.

---

### Tabla: `roles`
*Guarda los roles de la aplicación para el control de accesos.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | INT | PK, Autoincremental |
| `nombre_rol` | VARCHAR(30) | UNIQUE — Valores: `'ADMINISTRADOR'`, `'CONTADOR'`, `'VENDEDOR'` |

---

### Tabla: `usuarios`
*Almacena al personal interno de ALMAROS que opera la app.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK, Default: `gen_random_uuid()` |
| `nombre_completo` | VARCHAR(100) | |
| `correo` | VARCHAR(100) | UNIQUE |
| `contrasena_hash` | VARCHAR(255) | |
| `rol_id` | INT | FK → `roles.id` |
| `activo` | BOOLEAN | Default: `TRUE` |

---

### Tabla: `compradores`
*Registro pasivo de los clientes de la parcelación.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK |
| `cedula` | VARCHAR(20) | UNIQUE |
| `nombre` | VARCHAR(100) | |
| `telefono` | VARCHAR(20) | |
| `direccion` | VARCHAR(150) | |
| `correo` | VARCHAR(100) | |

---

### Tabla: `etapas`
*Representa las zonas de la parcelación.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | INT | PK, Autoincremental |
| `nombre_etapa` | VARCHAR(50) | UNIQUE — Valores: `'Etapa 1'`, `'Etapa 2'`, etc. |

---

### Tabla: `lotes`
*Inventario técnico de los lotes del proyecto "Mirador de San Antonio".*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK |
| `numero_lote` | INT | Ej: 1, 2, 3... |
| `etapa_id` | INT | FK → `etapas.id` |
| `area_m2` | NUMERIC(10,2) | |
| `precio_base` | NUMERIC(12,2) | |
| `estado` | VARCHAR(30) | Default: `'DISPONIBLE'` — `'DISPONIBLE'`, `'SEPARADO'`, `'VENDIDO'` |
| *Constraint* | | `UNIQUE(numero_lote, etapa_id)` — Evita duplicar el Lote 1 dentro de la misma Etapa |

---

### Tabla: `ventas_contratos`
*Modela el acuerdo comercial. Vincula al lote, al comprador y guarda los PDFs en Supabase Storage.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK |
| `lote_id` | UUID | FK → `lotes.id`, UNIQUE — Un lote solo puede tener un contrato activo |
| `comprador_id` | UUID | FK → `compradores.id` |
| `vendedor_id` | UUID | FK → `usuarios.id` — Trazabilidad de quién vendió |
| `precio_venta_pactado` | NUMERIC(12,2) | |
| `cuota_separacion` | NUMERIC(12,2) | |
| `plazo_meses` | INT | Ej: 12, 24, 40... |
| `fecha_venta` | DATE | |
| `url_pdf_contrato` | VARCHAR(255) | Enlace al archivo generado en Supabase Storage |
| `url_pdf_propiedad` | VARCHAR(255) | Enlace al documento de propiedad cargado en el Storage |

---

### Tabla: `cuotas_amortizacion`
*La proyección de pagos mensuales constantes calculada en el CU03.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK |
| `venta_id` | UUID | FK → `ventas_contratos.id` |
| `numero_cuota` | INT | Ej: Cuota 1 de 12, Cuota 2 de 12... |
| `monto_cuota` | NUMERIC(12,2) | |
| `fecha_vencimiento` | DATE | |
| `estado_pago` | VARCHAR(20) | Default: `'PENDIENTE'` — `'PENDIENTE'`, `'PAGADA'`, `'VENCIDA'` |

---

### Tabla: `pagos_ingresos`
*Historial de transacciones de dinero que entran a la caja de ALMAROS.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK |
| `cuota_id` | UUID | FK → `cuotas_amortizacion.id` — Nullable (puede ser cuota inicial de separación) |
| `venta_id` | UUID | FK → `ventas_contratos.id` |
| `usuario_id` | UUID | FK → `usuarios.id` — Quién recibió el dinero |
| `monto_pagado` | NUMERIC(12,2) | |
| `fecha_pago` | TIMESTAMP | Default: `now()` |
| `concepto` | VARCHAR(50) | `'CUOTA_SEPARACION'`, `'CUOTA_ORDINARIA'`, `'INTERES_MORA'` |
| `url_pdf_recibo` | VARCHAR(255) | Enlace al recibo generado |

---

### Tabla: `egresos`
*Control de gastos por rubros operativos del proyecto.*

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | UUID | PK |
| `contador_id` | UUID | FK → `usuarios.id` — Trazabilidad de quién lo registró |
| `monto` | NUMERIC(12,2) | |
| `fecha_egreso` | DATE | |
| `rubro` | VARCHAR(50) | `'MAQUINARIA'`, `'SUMINISTRO_AGUA'`, `'SERVICIOS_PUBLICOS'`, `'EXCAVACION'`, `'OTROS'` |
| `descripcion` | TEXT | |

---

## 7. Esquema de Ventanas y Flujo de Navegación

### Arquitectura UX

La aplicación usa un esquema de **SPA (Single Page Application)** con una barra de navegación lateral fija (**Sidebar**) y un contenedor central dinámico donde se cargan las ventanas.

---

### V01: Ventana de Login

- Cuadro central con el logo de ALMAROS.
- Campos: Correo Electrónico y Contraseña.
- Botón destacado: "Ingresar".
- Sin Sidebar ni barras de navegación pública.

---

### V02: Ventana Dashboard Financiero Global *(Vista Inicial: Administrador/Contador)*

- **4 Cards superiores:** Ventas Efectivas, Total Recaudado, Pendiente de Recaudo y Utilidad Neta.
- **Tabla comparativa** mensual de doble columna: **Ingresos vs Egresos**.

---

### V03: Ventana de Inventario de Lotes por Etapa *(Vista Inicial: Vendedor)*

- Selector desplegable para conmutar entre **Etapa 1, 2, 3 y 4**.
- **Cuadrícula/grilla interactiva** de tarjetas que representan los lotes.
- **Código de colores:**
  - 🔵 **Azul/Blanco:** Disponible
  - 🟠 **Naranja:** Separado con abono (amortización en progreso)
  - 🔴 **Rojo:** Bloqueado / Vendido por completo
- Al hacer clic en un lote, se abre una **barra lateral de detalles** con área (m²) y precio base.

---

### V04: Ventana de Contrato de Venta *(Formulario CU03)*

- **Sección 1:** Datos fijos del lote seleccionado (Etapa, Número, Área, Precio Base).
- **Sección 2:** Buscador de Cédula del comprador. Si no existe, despliega campos para: Nombre, Teléfono, Dirección y Correo.
- **Sección 3:** Campos de configuración del crédito: *Precio de Venta Pactado*, *Cuota de Separación*, *Plazo en Meses* (selector numérico).
- **Sección 4:** Input de carga de archivos (File Uploader) para el **Documento de Propiedad digitalizado**.
- Botón inferior: "Finalizar y Registrar Compra".

---

### V05: Ventana Estado de Cuenta del Cliente *(Gestión de Cartera — CU04/CU05)*

- Panel superior con la ficha del cliente.
- Tabla cronológica con proyección de cuotas mensuales semaforizada:
  - 🟢 Verde = Pagada
  - 🟠/🔴 Naranja/Rojo = Pendiente o Vencida
- Botón destacado: "Registrar Abono".
- Botones fijos: **"Descargar Contrato (PDF)"**.

---

### V06: Ventana Control de Egresos y Liquidación por Rubros *(CU06/CU07)*

- Botón superior "Agregar Gasto Operativo" → abre un Modal.
- Sección de filtros por: rango de fechas y **Rubro Operativo** (Maquinaria, Agua, Servicios Públicos, Excavación, Otros).
- Al aplicar filtros, renderiza la lista de egresos y el monto acumulado por categoría.

---

## 8. Contrato de la API — Endpoints Clave (Spring Boot)

### Módulo de Seguridad y Usuarios

```
POST /api/auth/login
```
Envía correo y contraseña; devuelve el **Token JWT** y el **Rol** del usuario.

```
POST /api/usuarios/registro
```
Crea un nuevo operador interno en Supabase (solo Admin).

---

### Módulo de Lotes e Inventario

```
GET /api/lotes?etapaId={id}
```
Devuelve el listado de lotes filtrados por etapa con su estado actual (Disponible, Vendido, Separado).

---

### Módulo de Ventas y Contratos

```
POST /api/ventas
```
Recibe el JSON con datos del comprador, el lote, los meses de plazo y los archivos PDF. El backend procesa, guarda en Supabase Storage, inserta en la BD relacional y genera el plan de cuotas fijas.

```
GET /api/ventas/{id}/contrato-pdf
```
Recupera el enlace del Storage de Supabase para descargar el contrato original en PDF.

---

### Módulo de Pagos y Caja

```
POST /api/pagos
```
Envía la cuota afectada, el monto recibido y el concepto. Retorna el flujo del PDF del recibo generado.

```
GET /api/clientes/{cedula}/estado-cuenta
```
Devuelve la ficha completa del comprador con el listado de cuotas proyectadas y su estado de amortización (semaforización).

---

### Módulo de Egresos y Analítica

```
POST /api/egresos
```
Registra un nuevo gasto cargado a un rubro operativo específico.

```
GET /api/egresos/liquidar?fechaInicio={f1}&fechaFin={f2}
```
Devuelve los gastos acumulados agrupados por rubros para reportes contables.

```
GET /api/analitica/dashboard
```
Ejecuta las funciones SQL sumatorias en Supabase para retornar los cuatro KPIs generales del proyecto.

---

## 9. Decisiones Tecnológicas

### Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| **Backend** | Java (Spring Boot) |
| **Base de Datos** | PostgreSQL (Supabase) |
| **Frontend** | HTML / JavaScript / Bootstrap |

### Arquitectura del Proyecto

- **Patrón:** SPA con Sidebar fija y contenedor dinámico.
- **Seguridad:** JWT para sesiones + BCrypt para contraseñas.
- **Transacciones:** `@Transactional` en Spring Boot para operaciones atómicas.
- **Almacenamiento de archivos:** Supabase Storage (Buckets para PDFs de contratos, recibos y documentos de propiedad).
- **Auditoría:** Log inalterable automático en cada operación crítica.

### Configuración de Conexión (`application.properties`)

```properties
spring.datasource.url=jdbc:postgresql://<SUPABASE_HOST>:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=<SUPABASE_PASSWORD>
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

*Documento generado a partir de PROYECTO_LOTE_CLICK.docx — ALMAROS / Mirador de San Antonio*
