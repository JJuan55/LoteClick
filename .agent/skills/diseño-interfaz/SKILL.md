name: diseño-interfaz
description: "Directrices estéticas, paleta de colores corporativa y uso de librerías visuales para el frontend estático de LoteClick."

# Estilo Visual y Diseño de Interfaz
1. **Paleta de Colores Corporativa (ALMAROS):**
   - [cite_start]Azul Marino (`#0B2545`): Para barras de navegación laterales (Sidebar), encabezados de tablas y botones principales.
   - [cite_start]Azul Claro (`#134074`): Para estados de selección y botones de acciones secundarias.
   - [cite_start]Gris Claro (`#EEF4F8`): Fondo general de todas las ventanas de la aplicación.
2. **Semaforización de Estados (Código de Colores del Excel original):**
   - [cite_start]Lotes Disponibles / Cuotas Pagadas: Color Verde (`#2A9D8F`).
   - [cite_start]Lotes Separados con abono / Cuotas Pendientes: Color Naranja/Amarillo (`#E9C46A`).
   - [cite_start]Lotes Vendidos / Cuotas Vencidas: Color Rojo (`#E63946`).
3. **Librería de Iconos:**
   - [cite_start]Utilizar exclusivamente **Bootstrap Icons** incorporado mediante CDN. [cite_start]Usar iconos descriptivos como `bi-person` para compradores, `bi-cash-coin` para registrar abonos y `bi-file-earmark-pdf` para los comprobantes.
4. **Animaciones Fluidas:**
   - [cite_start]Incorporar la librería **Animate.css** vía CDN. [cite_start]Agregar la clase `animate__animated animate__fadeInUp` en contenedores dinámicos, tarjetas de lotes y paneles del Dashboard para transiciones suaves de entrada.
   - [cite_start]Los formularios flotantes y modales de registro de pagos deben abrirse utilizando las animaciones nativas de Bootstrap 5 (`fade`).