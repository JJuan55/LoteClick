# DOCUMENTO MAESTRO DE CONTEXTO E INGENIERÍA: SISTEMA LOTECLICK

---

## 1. Contexto de la Aplicación y Objetivos

### Contexto
La empresa ALMAROS gestiona la parcelación del proyecto inmobiliario "Mirador de San Antonio". Actualmente, toda la operación crítica (trazabilidad de compradores, asignación de lotes, cuotas de separación, plazos de pago y balances de utilidad frente a egresos como maquinaria o servicios) se controla manualmente en libros de Excel. Esto genera riesgos de pérdida de datos, errores de digitación manual y falta de un registro histórico centralizado de contratos y comprobantes de pago.

### Objetivo General
Desarrollar un sistema web seguro y responsive que centralice y automatice la gestión comercial y financiera del proyecto de parcelación "Mirador de San Antonio", permitiendo el control estricto del estado de los lotes, la recaudación de pagos por cuotas de compradores, el registro de egresos por rubros operativos y la visualización en tiempo real de los estados financieros de la empresa.

### Objetivos Específicos
* **Gestionar el Inventario Inmobiliario:** Permitir el registro técnico de los lotes clasificados por Etapas (1, 2, 3, 4), almacenando su área en $m^2$, precio de venta base y estado de disponibilidad.
* **Controlar el Ciclo de Venta y Recaudo:** Registrar los contratos de compra de lotes asociados a compradores, permitiendo diferir el pago en plazos (meses), capturar la cuota de separación y amortizar los pagos subsecuentes con descarga de comprobantes en PDF.
* **Administrar el Flujo de Caja Total:** Registrar los ingresos (recaudos ordinarios y extraordinarios) y clasificar los egresos del proyecto por rubros específicos (maquinaria, agua, excavación, servicios públicos, etc.).
* **Proveer Analítica Financiera Interna:** Generar vistas consolidadas y tableros que reflejen las Ventas Efectivas, el Total Recaudado, el Pendiente por Recaudar y la Utilidad neta del proyecto (reproduciendo las métricas automatizadas del Excel actual).

---

## 2. Roles de Usuario (Personal Interno)

El Comprador en el sistema será una entidad pasiva (un registro en la base de datos asociado a un lote y a sus pagos). Los únicos que operan el sistema son el personal interno. Los roles de usuario se unifican y expanden así:

* **Administrador (Gerencia de ALMAROS):** Acceso total. Modifica parámetros del proyecto, aprueba contratos, consulta la auditoría (quién registró qué) y visualiza los reportes macro de utilidades y aportes de socios.
* **Contador:** Encargado del Módulo de Ingresos y Egresos. Registra y clasifica los egresos por rubros (maquinaria, excavación, etc.), concilia los pagos de lotes y vigila los balances financieros.
* **Vendedor (Asesor Comercial):** Encargado de la operación diaria. Consulta disponibilidad de lotes, registra compradores, crea las ventas/contratos (definiendo el plazo y la cuota inicial), registra los pagos de cuotas diarios y descarga los comprobantes de pago PDF para entregárselos al cliente de forma física o por correo.

---

## 3. Acciones de los Casos de Uso por Rol

### Vendedor
* **Consultar Inventario de Lotes:** Filtrar lotes por Etapa, tamaño ($m^2$) o Estado (Disponible, Separado con Abono, Pagado por Completo).
* **Registrar / Actualizar Comprador:** Crear la ficha de datos básicos del cliente (Nombre, Cédula, Teléfono, Correo).
* **Ejecutar Contrato de Venta:** Seleccionar un lote disponible, asociarlo al comprador, visualizar los datos del contrato base, definir el plazo en meses (ej. 12, 24, 40 meses según el Excel) y registrar el pago de la cuota de separación.
* **Registrar Pago de Cuota Mensual:** Buscar al comprador, seleccionar su lote en proceso de pago, ingresar el monto abonado en el periodo y guardarlo.
* **Descargar Comprobante de Pago:** Generar el archivo PDF del recibo recién capturado para el cliente.

### Contador
* **Registrar Egreso Operativo:** Ingresar salidas de dinero asociándolas a un rubro específico (Maquinaria, Compra de Agua, Servicios Públicos, Excavación, Nómina, etc.).
* **Visualizar Registro General de Ingresos:** Monitorear el listado cronológico de todos los pagos que han ingresado por concepto de venta de lotes o aportes.
* **Consultar Reportes de Cobranza y Saldos:** Ver listas de clientes que van al día (en verde) o atrasados/con saldo pendiente (en naranja/rojo) para gestión de cartera.

### Administrador
* Tiene el permiso de ejecutar todas las acciones de los Vendedores y Contadores.
* **Gestionar Usuarios:** Crear, editar o dar de baja cuentas de acceso de los Vendedores y Contadores.
* **Visualizar Tablero de Control Financiero (Dashboard):** Pantalla principal que emula los cuadros inferiores de la imagen Excel: cálculo en tiempo real de Ventas Efectivas, Total Recaudado, Pendiente de Recaudo e Indicadores de Utilidad neta del proyecto.
* **Controlar Aportes de Socios:** Módulo exclusivo para registrar capital inicial de los inversionistas de ALMAROS y calcular su participación porcentual automática basada en las utilidades actuales.
* **Consultar Logs de Auditoría:** Verificar qué usuario del sistema realizó modificaciones delicadas (por ejemplo, alterar el precio de un lote o eliminar un egreso).

---

## 4. Implementación Detallada de los Casos de Uso

### Módulo 0: Autenticación y Control de Accesos

#### CU01: Autenticación de Usuario (Login)
* **Actor:** Administrador, Contador o Vendedor.
* **Descripción:** Permite a los usuarios internos ingresar de manera segura a la plataforma según sus privilegios.
* **Flujo Paso a Paso:**
  1. El usuario ingresa a la aplicación web y es recibido por la Ventana de Login.
  2. El usuario digita su Correo Electrónico y Contraseña corporativa, y presiona el botón "Iniciar Sesión".
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend valida en la base de datos (Supabase) que las credenciales existan y coincidan de forma encriptada.
  2. El backend extrae el rol activo asociado al usuario (Administrador, Contador o Vendedor).
  3. Se genera un token de sesión seguro.
  4. **Redirección:**
     * Si el rol es Administrador, redirige a la ventana Dashboard Financiero Global[cite: 3].
     * Si el rol es Contador, redirige a la ventana Control de Egresos e Ingresos[cite: 3].
     * Si el rol es Vendedor, redirige a la ventana Inventario de Lotes por Etapa[cite: 3].
* **Respuesta del Sistema (Caminos de Excepción):**
  * **Excepción A (Credenciales incorrectas):** El backend detecta que la contraseña no coincide o el correo no existe[cite: 3]. El sistema limpia el campo de contraseña, muestra una alerta de texto rojo: "Credenciales incorrectas. Intente nuevamente" y mantiene al usuario en la Ventana de Login bloqueando el acceso[cite: 3].
  * **Excepción B (Usuario Inactivo):** El backend detecta que las credenciales son correctas, pero el estado del empleado es "Inactivo"[cite: 3]. El sistema muestra la alerta: "Cuenta deshabilitada. Contacte al Administrador" y mantiene al usuario en la Ventana de Login[cite: 3].

#### CU02: Crear y Asignar Usuario (Exclusivo Administrador)
* **Actor:** Administrador[cite: 3].
* **Descripción:** Permite dar de alta al personal que operará el sistema. No hay registro público[cite: 3].
* **Flujo Paso a Paso:**
  1. El Administrador navega a la Ventana de Gestión de Usuarios y presiona "Agregar Nuevo Empleado"[cite: 3].
  2. Llena el formulario con: Nombre Completo, Correo Corporativo, Contraseña Temporal y selecciona el Rol corporativo (Vendedor o Contador) de una lista desplegable[cite: 3].
  3. Presiona el botón "Guardar Usuario"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend valida los formatos del formulario y la robustez de la contraseña[cite: 3].
  2. Inserta el registro en la base de datos de Supabase[cite: 3].
  3. Escribe de forma transparente en la tabla de auditoría: Administrador [Nombre] creó al usuario [Correo] con rol [Rol][cite: 3].
  4. El sistema cierra el formulario, muestra un mensaje de éxito ("Usuario creado correctamente") y recarga la tabla en la misma Ventana de Gestión de Usuarios mostrando el nuevo registro[cite: 3].
* **Respuesta del Sistema (Caminos de Excepción):**
  * **Excepción A (Correo Duplicado):** El backend detecta que el correo electrónico ya pertenece a un usuario existente[cite: 3]. El sistema frena la inserción, pinta el campo de correo en rojo, despliega el mensaje: "Este correo electrónico ya se encuentra registrado" y mantiene el formulario abierto con los datos previos intactos[cite: 3].

### Módulo 1: Gestión Comercial, Lotes y Ventas

#### CU03: Ejecutar Venta de Lote y Apertura de Contrato
* **Actor:** Vendedor[cite: 3].
* **Descripción:** Permite a cualquier vendedor de la empresa formalizar la venta de un lote disponible del inventario global compartida por el equipo, parametrizando los plazos de amortización y guardando el contrato legal perpetuo[cite: 3].
* **Flujo Paso a Paso:**
  1. El Vendedor ingresa a la Ventana de Inventario de Lotes, selecciona una Etapa (1, 2, 3 o 4) mediante un filtro desplegable y hace clic sobre un lote disponible[cite: 3].
  2. Al ver el tamaño ($m^2$) y precio base en pantalla, presiona el botón "Iniciar Proceso de Venta"[cite: 3].
  3. El sistema abre la Ventana de Contrato de Venta[cite: 3].
  4. El Vendedor digita la Cédula del comprador[cite: 3]. Si es nuevo, completa sus datos básicos (Nombre, Teléfono, Dirección, Correo); si ya existía, el sistema auto-completa los campos[cite: 3].
  5. El Vendedor digita los acuerdos de la venta: Cuota de Separación (Pago inicial), Plazo en meses (ej: 12, 24, 40 meses) y la fecha de la venta[cite: 3].
  6. Presiona el botón "Finalizar y Registrar Compra"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend ejecuta una transacción atómica segura:
     * Almacena/Actualiza los datos del Comprador[cite: 3].
     * Modifica el estado del lote a "Separado con Abono" o "Vendido"[cite: 3].
     * Registra un ingreso financiero automático por el valor de la cuota de separación[cite: 3].
     * Genera automáticamente una tabla con la proyección de las cuotas mensuales (Fechas de vencimiento y montos) según el plazo en meses digitado[cite: 3].
     * **Generación y Persistencia del Contrato:** El sistema genera dinámicamente el documento del contrato legal en formato PDF compilando los datos de la compra[cite: 3]. Sube este archivo directamente al Bucket de Storage de Supabase y guarda la URL del documento vinculada de forma permanente a la transacción[cite: 3].
     * Guarda la trazabilidad en la auditoría vinculando el ID del vendedor que cerró la transacción[cite: 3].
  2. **Redirección:** Redirecciona al Vendedor a la Ventana de Resumen de Venta, muestra una notificación de éxito y habilita un botón permanente de: 📄 "Descargar Contrato de Venta (PDF)" (disponible para consultas futuras en cualquier estado)[cite: 3].
* **Respuesta del Sistema (Caminos de Excepción):**
  * **Excepción A (Conflicto de Lote Ocupado en simultáneo):** Si otro asesor cerró una venta por el mismo lote mientras el formulario actual seguía abierto, el backend frena la transacción por cambio de estado del lote[cite: 3]. El sistema cancela la operación, arroja un mensaje emergente: "Error: El lote ya no se encuentra disponible" y redirige automáticamente al Vendedor a la Ventana de Inventario de Lotes con el mapa completamente actualizado[cite: 3].

#### CU04: Consultar Lotes Pagados y Vendidos
* **Actor:** Vendedor, Contador o Administrador[cite: 3].
* **Descripción:** Permite realizar el seguimiento y auditoría del estado actual de todos los lotes de la parcelación "Mirador de San Antonio"[cite: 3].
* **Flujo Paso a Paso:**
  1. El usuario ingresa a la Ventana de Reporte de Inventario Inmobiliario[cite: 3].
  2. Selecciona filtros específicos opcionales como: Etapa (1, 2, 3, 4) o Estado del Lote (Disponible, Separado, Pagado Total)[cite: 3].
  3. Presiona el botón "Filtrar"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El sistema realiza una consulta relacional (JOIN) cruzando las etapas, lotes, datos del comprador y contratos activos[cite: 3].
  2. Renderiza una tabla dinámica interactiva con los campos: Etapa, Número de Lote, Área ($m^2$), Comprador, Precio de Venta, Total Recaudado a la fecha y Estado[cite: 3].
  3. Cada fila incluye un botón para acceder al Detalle de la Venta, donde se podrá visualizar e invocar en cualquier momento la descarga del PDF del contrato de venta original almacenado en el Storage de Supabase[cite: 3].

### Módulo 2: Gestión de Recaudos y Cartera

#### CU05: Registrar Pago de Cuota Mensual
* **Actor:** Vendedor o Contador[cite: 3].
* **Descripción:** Registra de forma manual los abonos diarios de cuotas que hacen los compradores para amortizar su deuda, emitiendo un recibo físico descargable[cite: 3].
* **Flujo Paso a Paso:**
  1. El usuario entra a la Ventana de Gestión de Cartera/Clientes y busca al comprador digitando su número de cédula en la barra de búsqueda[cite: 3].
  2. Selecciona al cliente y el sistema despliega el Estado de Cuenta del Cliente, reflejando la lista completa de sus cuotas en un formato semaforizado (Verde = Pagado, Naranja/Rojo = Pendiente/Vencido)[cite: 3].
  3. El usuario presiona el botón "Registrar Abono"[cite: 3].
  4. Se despliega un formulario flotante (Modal) donde digita el Monto Recibido en efectivo/transferencia y el Concepto de Pago (Cuota ordinaria, Intereses por mora, Penalidad)[cite: 3].
  5. Hace clic en "Confirmar Pago"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend añade el abono a la tabla de ingresos[cite: 3].
  2. Actualiza la fila de la cuota correspondiente cambiando su estado a "Pagada" (Verde) y recalcula automáticamente los saldos pendientes del lote del cliente[cite: 3].
  3. Modifica en tiempo real los acumulados generales de recaudación del proyecto[cite: 3].
  4. Genera de forma automatizada un recibo de caja en formato PDF con los datos de la transacción, fecha, hora y token único de validación[cite: 3].
  5. Escribe la acción en el registro de auditoría[cite: 3].
  6. **Redirección / Interfaz:** Cierra el modal, actualiza la lista semaforizada del cliente en pantalla y abre una pestaña nueva del navegador con el PDF del comprobante listo para imprimir o descargar[cite: 3].
* **Respuesta del Sistema (Caminos de Excepción):**
  * **Excepción A (Monto Inválido):** Si se digitan letras, valores negativos o un valor igual a $0, el frontend y backend bloquean el envío[cite: 3]. El sistema resalta el input en rojo y emite el texto: "El monto de abono debe ser un valor numérico superior a cero"[cite: 3]. El modal se mantiene abierto reteniendo los datos para su corrección[cite: 3].

### Módulo 3: Control de Egresos Operativos

#### CU06: Registrar Egreso Operativo por Rubro
* **Actor:** Contador o Administrador[cite: 3].
* **Descripción:** Permite llevar el control estricto de las salidas de dinero de la empresa, clasificándolas por los rubros operativos reales del proyecto de parcelación[cite: 3].
* **Flujo Paso a Paso:**
  1. El usuario se dirige a la Ventana de Control de Egresos y presiona el botón "Registrar Nuevo Gasto"[cite: 3].
  2. Completa los campos solicitados: Fecha del Gasto, Monto en USD, Descripción detallada de la compra o servicio adquirido[cite: 3].
  3. Selecciona de manera obligatoria de un menú desplegable el Rubro de Egreso: Maquinaria, Suministro de Agua, Servicios Públicos, Excavación u Otros[cite: 3].
  4. Presiona el botón "Registrar Egreso"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend valida e inserta el egreso en la base de datos relacional vinculándolo al rubro elegido[cite: 3].
  2. El motor financiero resta automáticamente dicho monto del cálculo global de utilidades de la empresa ALMAROS[cite: 3].
  3. **Redirección:** El sistema cierra el formulario, despliega un aviso temporal de éxito ("Gasto operativo registrado con éxito") y actualiza de inmediato la tabla cronológica de la Ventana de Control de Egresos mostrando el registro en la primera fila[cite: 3].
* **Respuesta del Sistema (Caminos de Excepción):**
  * **Excepción A (Rubro no seleccionado):** Si el usuario deja la selección del menú desplegable por defecto (vacía), el sistema impide el guardado, resalta la casilla en rojo y añade el aviso: "Debe asignar este egreso a un rubro operativo válido"[cite: 3]. El flujo se mantiene detenido en el formulario de egreso[cite: 3].

#### CU07: Liquidar y Consultar Rubros de Egresos
* **Actor:** Contador o Administrador[cite: 3].
* **Descripción:** Despliega informes detallados de los egresos de la empresa agrupados por categorías operativas para auditoría de costos[cite: 3].
* **Flujo Paso a Paso:**
  1. El usuario ingresa a la Ventana de Liquidación de Rubros[cite: 3].
  2. Configura un rango de fechas (Fecha Inicio - Fecha Fin) y selecciona si desea ver un rubro específico o el consolidado completo[cite: 3]. Presiona "Generar Informe"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El sistema computa los egresos en Supabase y renderiza una tabla acumulada por rubros operativos (ej: Total Maquinaria: $X USD, Total Excavación: $Y USD)[cite: 3].
  2. Permite exportar la vista o desgolsar cada rubro para ver los detalles individuales de facturas y descripciones de gastos registrados[cite: 3].

### Módulo 4: Alta Dirección y Analítica Financiera

#### CU08: Visualizar Cuadro de Mando Financiero (Dashboard)
* **Actor:** Administrador o Contador[cite: 3].
* **Descripción:** Pantalla analítica principal que emula los recuadros automatizados de la hoja de cálculo real de ALMAROS para la toma de decisiones estratégicas[cite: 3].
* **Flujo Paso a Paso:**
  1. El usuario inicia sesión o hace clic en el menú lateral en la sección de Inicio / Dashboard[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend ejecuta consultas sumatorias en tiempo real sobre la base de datos y renderiza un cuadro estadístico dinámico con cuatro indicadores clave:
     * **Ventas Efectivas:** Suma total de los precios pactados en los contratos firmados[cite: 3].
     * **Total Recaudado:** Suma total de dinero real que ha ingresado a caja por cuotas iniciales y mensuales[cite: 3].
     * **Pendiente de Recaudo:** La diferencia matemática que falta por cobrar a los clientes para completar el proyecto[cite: 3].
     * **Utilidad Neta:** Cálculo automático restando el consolidado de egresos por rubros del total recaudado real[cite: 3].
  2. Adicionalmente, dibuja una tabla comparativa mensual de Ingresos vs Egresos (reproduciendo fielmente la estructura visual de tu imagen de Excel)[cite: 3].

#### CU09: Gestionar Aportes de Socios
* **Actor:** Exclusivo Administrador[cite: 3].
* **Descripción:** Permite controlar el capital inyectado por los inversionistas y conocer su cuota de participación porcentual automática basada en los rendimientos del proyecto inmobiliario[cite: 3].
* **Flujo Paso a Paso:**
  1. El Administrador ingresa a la Ventana de Aportes de Socios[cite: 3].
  2. El sistema muestra la lista de socios actuales con sus aportes y porcentaje[cite: 3].
  3. Para añadir capital, presiona "Registrar Inyección de Capital", selecciona al socio, introduce el monto en USD y da clic en "Guardar"[cite: 3].
* **Respuesta del Sistema (Caso Exitoso):**
  1. El backend registra el nuevo aporte en el histórico del socio[cite: 3].
  2. Recalcula instantáneamente el fondo común de aportes y actualiza de manera automática la Participación Porcentual de cada uno de los inversionistas del proyecto frente al nuevo total de capital inyectado[cite: 3].
  3. La pantalla se actualiza refrescando la tabla de socios con los nuevos valores y porcentajes calculados matemáticamente[cite: 3].

---

## 5. Requisitos Técnicos del Sistema

### A. Requerimientos Funcionales (RF)
| Código | Requerimiento Funcional | Descripción | Caso de Uso Asociado |
| :--- | :--- | :--- | :--- |
| **RF01** | Autenticación Segura | Validar las credenciales de usuarios internos contra la base de datos de manera encriptada[cite: 3]. | CU01[cite: 3] |
| **RF02** | Control de Accesos (RBAC) | Restringir vistas, menús y ejecuciones según el rol (Admin, Contador, Vendedor)[cite: 3]. | CU01[cite: 3] |
| **RF03** | Registro Centralizado | Permitir al Administrador crear empleados asignándoles un rol sin registro público[cite: 3]. | CU02[cite: 3] |
| **RF04** | Filtro por Etapas | Listar el inventario de lotes segmentado por su respectiva Etapa (1, 2, 3 o 4)[cite: 3]. | CU03 / CU04[cite: 3] |
| **RF05** | Ficha de Compradores | Permitir el registro y consulta de datos personales básicos de los compradores de parcelas[cite: 3]. | CU03 / CU05[cite: 3] |
| **RF06** | Cierre de Venta y Amortización | Registrar la venta de un lote, calcular la tabla de cuotas mensuales fijas y constantes, y mutar estado de lote[cite: 3]. | CU03[cite: 3] |
| **RF07** | Persistencia de Contratos | Compilar los datos de venta en PDF, guardarlo en Storage y permitir descarga perpetua[cite: 3]. | CU03 / CU04[cite: 3] |
| **RF08** | Carga de Títulos de Propiedad | Permitir subir y asociar el Documento de Propiedad digitalizado (PDF/Imagen) a la venta en Supabase Storage[cite: 3]. | CU03 / CU04[cite: 3] |
| **RF09** | Recaudo de Cuotas en Dinero | Registrar abonos financieros manuales (USD/COP) afectando el plan de cuotas[cite: 3]. | CU05[cite: 3] |
| **RF10** | Emisión de Recibos | Tras cada recaudo exitoso, generar un recibo en PDF y abrirlo de forma automática[cite: 3]. | CU05[cite: 3] |
| **RF11** | Egresos por Rubros | Registrar gastos categorizados por: Maquinaria, Agua, Servicios Públicos, Excavación u Otros[cite: 3]. | CU06[cite: 3] |
| **RF12** | Liquidación de Costos | Consolidar y filtrar egresos acumulados agrupados por su rubro en rangos de fechas[cite: 3]. | CU07[cite: 3] |
| **RF13** | KPIs en Tiempo Real | Calcular en tiempo real: Ventas Efectivas, Recaudado, Pendiente y Utilidad Neta[cite: 3]. | CU08[cite: 3] |
| **RF14** | Participación de Socios | Registrar aportes e inyecciones calculando la participación porcentual sobre utilidades[cite: 3]. | CU09[cite: 3] |
| **RF15** | Log de Auditoría Interna | Registrar automáticamente en bitácora inalterable qué usuario hizo cada acción financiera[cite: 3]. | Todos[cite: 3] |

### B. Requerimientos No Funcionales (RNF)
* **RNF01 (Seguridad):** Las contraseñas de los usuarios deben ser almacenadas en la base de datos utilizando algoritmos de encriptación hash seguros (ej: BCrypt en Spring Boot)[cite: 3].
* **RNF02 (Usabilidad):** La interfaz de usuario debe ser completamente Responsive para garantizar una navegación óptima tanto en computadoras de escritorio como en tablets[cite: 3].
* **RNF03 (Disponibilidad):** La base de datos debe operar sobre una infraestructura Cloud (Supabase PostgreSQL) garantizando conectividad remota estable las 24/7[cite: 3].
* **RNF04 (Integridad):** Toda transacción monetaria que altere tablas críticas (ventas, pagos, egresos) debe ejecutarse de forma atómica en el backend (Spring Boot `@Transactional`)[cite: 3].
* **RNF05 (Restricción de Alcance):** El sistema procesará exclusivamente transacciones financieras en dinero real, omitiendo pasarelas de pago online y registros en especie[cite: 3].

---

## 6. Estructura Física de la Base de Datos (PostgreSQL SQL Script)

Utilizar este script DDL directamente en el **SQL Editor de Supabase** para instanciar las entidades[cite: 3]:

```sql
-- Creación de Tablas e Integridad Relacional
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nombre_rol VARCHAR(30) UNIQUE NOT NULL
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_completo VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol_id INT REFERENCES roles(id),
    activo BOOLEAN DEFAULT TRUE 
);

CREATE TABLE compradores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cedula VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(150),
    correo VARCHAR(100)
);

CREATE TABLE etapas (
    id SERIAL PRIMARY KEY,
    nombre_etapa VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE lotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_lote INT NOT NULL,
    etapa_id INT REFERENCES etapas(id),
    area_m2 NUMERIC(10,2) NOT NULL,
    precio_base NUMERIC(12,2) NOT NULL,
    estado VARCHAR(30) DEFAULT 'DISPONIBLE',
    CONSTRAINT unique_lote_etapa UNIQUE(numero_lote, etapa_id)
);

CREATE TABLE ventas_contratos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lote_id UUID REFERENCES lotes(id) UNIQUE,
    comprador_id UUID REFERENCES compradores(id),
    vendedor_id UUID REFERENCES usuarios(id),
    precio_venta_pactado NUMERIC(12,2) NOT NULL,
    cuota_separacion NUMERIC(12,2) NOT NULL,
    plazo_meses INT NOT NULL,
    fecha_venta DATE NOT NULL,
    url_pdf_contrato VARCHAR(255),
    url_pdf_propiedad VARCHAR(255)
);

CREATE TABLE cuotas_amortizacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id UUID REFERENCES ventas_contratos(id) ON DELETE CASCADE,
    numero_cuota INT NOT NULL,
    monto_cuota NUMERIC(12,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado_pago VARCHAR(20) DEFAULT 'PENDIENTE'
);

CREATE TABLE pagos_ingresos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cuota_id UUID REFERENCES cuotas_amortizacion(id),
    venta_id UUID REFERENCES ventas_contratos(id),
    usuario_id UUID REFERENCES usuarios(id),
    monto_pagado NUMERIC(12,2) NOT NULL,
    fecha_pago TIMESTAMP DEFAULT now(),
    concepto VARCHAR(50) NOT NULL,
    url_pdf_recibo VARCHAR(255)
);

CREATE TABLE egresos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contador_id UUID REFERENCES usuarios(id),
    monto NUMERIC(12,2) NOT NULL,
    fecha_egreso DATE NOT NULL,
    rubro VARCHAR(50) NOT NULL,
    descripcion TEXT
);

-- Inserción de Datos Maestros Iniciales
INSERT INTO roles (nombre_rol) VALUES ('ADMINISTRADOR'), ('CONTADOR'), ('VENDEDOR');
INSERT INTO etapas (nombre_etapa) VALUES ('Etapa 1'), ('Etapa 2'), ('Etapa 3'), ('Etapa 4');


##7. Esquema de Ventanas y Flujo de Interacción UI/UXMapa de Ventanas y Arquitectura UX (Layout por Rol)La aplicación utilizará un esquema de SPA (Single Page Application) o renderizado por plantillas con una barra de navegación lateral fija (Sidebar) y un contenedor central dinámico donde se cargan las ventanas.

+-----------------------------------------------------------------------+
|  LoteClick - ALMAROS                                    [Cerrar Sesión] |
+------------------+----------------------------------------------------+
|  SIDEBAR         |  CONTENEDOR CENTRAL DINÁMICO                        |
|                  |                                                    |
|  - Dashboard     |  (Aquí se renderiza la ventana activa según el     |
|  - Lotes/Etapas  |   rol del usuario y las acciones que ejecute)      |
|  - Cartera/Pagos |                                                    |
|  - Egresos       |                                                    |
|  - Usuarios      |                                                    |
+------------------+----------------------------------------------------+

##Especificación Detallada de las Ventanas
  V01 (Ventana de Login): Formulario central aislado[cite: 3]. Solicita correo electrónico y clave corporativa[cite: 3]. Botón "Ingresar" desencadena autenticación e inyecta token[cite: 3]. No posee barras públicas[cite: 3].

  V02 (Dashboard Financiero): Cuatro tarjetas superiores informativas calculadas desde Supabase (Ventas Efectivas, Total Recaudado, Pendiente de Recaudo, Utilidad Neta)[cite: 3]. Panel inferior de doble columna para la tabla consolidada e histórica de Ingresos vs Egresos por mes[cite: 3].

  V03 (Inventario de Lotes): Desplegable de Etapas (1, 2, 3, 4) en cabecera[cite: 3]. Grid de tarjetas dinámicas simulando la geografía del Excel[cite: 3]. Colores: Azul/Blanco (Disponible), Naranja (Separado en amortización), Rojo (Vendido)[cite: 3]. Clic abre panel lateral con metadata técnica[cite: 3].

  V04 (Contrato de Venta): Formulario modular estructurado[cite: 3]. Sección 1: Datos fijos del lote[cite: 3]. Sección 2: Captura/Búsqueda de cédula de comprador[cite: 3]. Sección 3: Parámetros comerciales (Precio final, separación, meses)[cite: 3]. Sección 4: Carga de documentos de propiedad (File Uploaders)[cite: 3].

  V05 (Estado de Cuenta Cliente): Cabecera con datos del propietario[cite: 3]. Tabla central con el plan de cuotas constantes (Número, vencimiento, monto, estado semaforizado en verde/naranja/rojo)[cite: 3]. Botones permanentes para "Registrar Abono" y "Descargar Contrato PDF"[cite: 3].

  V06 (Control de Egresos): Historial contable de compras[cite: 3]. Botón superior abre modal flotante parametrizado con selector de rubros obligatorios (Maquinaria, Agua, Servicios, Excavación, Otros)[cite: 3]. Filtros por rangos temporales[cite: 3].

 ## 8. Transcripción de Flujos de Interacción de la Interfaz

  **Transcripción de Flujo Técnico: CU03 - Cierre de Venta

  [V03: Inventario Lotes] 
       --> Clic Lote Disponible --> Botón "Iniciar Venta" 
       --> [V04: Formulario Contrato de Venta]
               |
               +--> Botón "Finalizar Compra" --> Envío de datos al Backend
                       |
                       +---> [Backend Responde ÉXITO] 
                       |        --> Alerta flotante "Venta Guardada con éxito"
                       |        --> Redirección automática a [V05: Estado de Cuenta del Cliente]
                       |        --> Abre en pestaña nueva el PDF del Contrato generado.
                       |
                       +---> [Backend Responde EXCEPCIÓN: Lote Ocupado]
                                --> Frena el envío.
                                --> Muestra Modal de error: "Este lote acaba de ser vendido por otro asesor."
                                --> Redirección forzada a [V03: Inventario Lotes] (Mapa refrescado).

    **Transcripción de Flujo Técnico: CU05 - Registro de Abonos

    [V05: Estado de Cuenta Cliente]
       --> Botón "Registrar Abono" --> Abre Modal Emergente [Formulario de Pago]
               |
               +--> Introduce Monto y Concepto --> Botón "Confirmar Pago"
                       |
                       +---> [Backend Responde ÉXITO]
                       |        --> Cierra el Modal automáticamente.
                       |        --> Refresca la tabla interna de V05 (La cuota cambia a color Verde).
                       |        --> Dispara la descarga del PDF del Recibo de Caja en pestaña nueva.
                       |
                       +---> [Backend Responde EXCEPCIÓN: Monto <= 0]
                                --> No cierra el Modal.
                                --> Pinta el borde del input en rojo.
                                --> Texto de alerta: "Monto inválido". El usuario corrige los datos ahí mismo.


    ##9. Contrato de la API y Endpoints REST (Spring Boot)

        POST /api/auth/login: Autentica al operador y retorna JWT y Rol[cite: 3].

        POST /api/usuarios/registro: Permite al Administrador dar de alta personal interno[cite: 3].

        GET /api/lotes?etapaId={id}: Retorna el inventario de lotes asociados a una etapa[cite: 3].

        POST /api/ventas: Recibe multipart del contrato, datos de cliente y genera el plan de cuotas[cite: 3].

        GET /api/ventas/{id}/contrato-pdf: Obtiene la URL pública del Storage de Supabase para el contrato[cite: 3].

        POST /api/pagos: Procesa un abono en dinero a una cuota, mutando el estado y generando recibo[cite: 3].

        GET /api/clientes/{cedula}/estado-cuenta: Retorna el estado consolidado y semaforizado de deudas[cite: 3].

        POST /api/egresos: Inserta un gasto operativo ligado a un rubro del proyecto inmobiliario[cite: 3].

        GET /api/egresos/liquidar: Computa resúmenes agregados por rubros financieros[cite: 3].

        GET /api/analitica/dashboard: Retorna agregaciones SQL sumatorias en tiempo real de los KPIs del proyecto[cite: 3].


      ##10. Arquitectura Física e Implementación (Monolito con Front Estático)


              LoteClick/
              ├── src/
              │   └── main/
              │       ├── java/com/almaros/loteclick/
              │       │   ├── controllers/      # Controladores REST (Atienden al Frontend)
              │       │   ├── models/           # Entidades Java mapeadas a Supabase
              │       │   ├── repositories/     # Interfaces para consultas SQL automáticas
              │       │   └── services/         # Lógica: Amortización de cuotas y cálculos
              │       │
              │       └── main/resources/
              │           ├── static/           # ¡AQUÍ VA TU FRONTEND PROFESIONAL!
              │           │   ├── css/          # Estilos personalizados y Animate.css
              │           │   ├── js/           # Lógica JavaScript (Peticiones fetch)
              │           │   │   ├── auth.js   # Control de sesión y Login
              │           │   │   ├── lotes.js  # Control de la grilla de lotes y ventas
              │           │   │   └── pagos.js  # Control de abonos y egresos
              │           │   │
              │           │   ├── index.html    # Ventana de Login (V01)
              │           │   ├── dashboard.html# Tablero analítico principal (V02)
              │           │   ├── inventario.html# Mapa interativo de lotes (V03)
              │           │   └── cartera.html  # Estados de cuenta de compradores (V05)
              │           │
              │  


    ##Archivo Final Solucionado: src/main/resources/application.properties

            # CONFIGURACIÓN DE CONEXIÓN A SUPABASE CLOUD
            spring.datasource.url=jdbc:postgresql://db.lomqhgclvuooviyesnsx.supabase.co:5432/postgres
            spring.datasource.username=postgres
            spring.datasource.password=Cr@ft:2347

            # Configuración del Driver de PostgreSQL e Hibernate
            spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
            spring.jpa.hibernate.ddl-auto=update
            spring.jpa.show-sql=true

            # Permitir la carga de archivos grandes en el sistema (Contratos y documentos de propiedad en PDF)
            spring.servlet.multipart.max-file-size=5MB
            spring.servlet.multipart.max-request-size=5MB