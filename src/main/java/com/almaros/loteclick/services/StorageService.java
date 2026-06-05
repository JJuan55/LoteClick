package com.almaros.loteclick.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Phrase;
import java.awt.Color;

/**
 * Servicio encargado de gestionar el almacenamiento de archivos (Documentos de Propiedad y Contratos).
 * Admite carga a Supabase Storage y cuenta con fallback local automático.
 */
@Service
public class StorageService {

    @Value("${supabase.url:https://lomqhgclvuooviyesnsx.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.key:}")
    private String supabaseKey;

    private static final String BUCKET_NAME = "loteclick-documents";
    private static final String LOCAL_UPLOAD_DIR = "src/main/resources/static/uploads";
    private static final String TARGET_UPLOAD_DIR = "target/classes/static/uploads";

    /**
     * Guarda el documento de propiedad digitalizado.
     * Si las credenciales de Supabase están vacías, se guarda localmente en /uploads/.
     */
    public String guardarDocumentoPropiedad(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "documento.pdf";
        
        // Limpiar el nombre de archivo de caracteres extraños y espacios
        String sanitizedFilename = originalFilename.replaceAll("\\s+", "_");
        String finalFilename = "propiedad_" + UUID.randomUUID().toString().substring(0, 8) + "_" + sanitizedFilename;

        // Si tenemos la clave de Supabase, intentamos subirlo
        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(file.getBytes(), "propiedades/" + finalFilename, file.getContentType());
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir a Supabase Storage: " + e.getMessage() + ". Ejecutando Fallback local.");
            }
        }

        // Fallback: guardar en carpeta local
        return guardarLocalmente(file.getBytes(), finalFilename);
    }

    /**
     * Helper para agregar celdas estilizadas a tablas de OpenPDF.
     */
    private void addTableCell(PdfPTable table, String text, boolean isBold, Color bgColor, Color textColor, int alignment) {
        Font font = new Font(Font.HELVETICA, 10, isBold ? Font.BOLD : Font.NORMAL, textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    /**
     * Genera un PDF de contrato legal compilando la información y guardándola.
     */
    public String generarContratoPdfSimulado(String numeroLote, String nombreEtapa, String compradorNombre, 
                                             String compradorCedula, String precioPactado, String cuotaSeparacion, 
                                             int plazoMeses) throws IOException {
        
        String filename = "contrato_" + UUID.randomUUID().toString().substring(0, 8) + "_lote_" + numeroLote + ".pdf";
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.setMargins(40f, 40f, 40f, 40f);
            document.open();
            
            Color navy = new Color(11, 37, 69);
            Color darkText = new Color(30, 41, 59);
            Font titleFont = new Font(Font.HELVETICA, 15, Font.BOLD, navy);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, darkText);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, navy);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            
            // Banner superior
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell(new Phrase("PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO - ALMAROS S.A.S.", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            headerCell.setBackgroundColor(navy);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(8f);
            headerCell.setBorder(0);
            headerTable.addCell(headerCell);
            document.add(headerTable);
            
            document.add(new Paragraph("\n"));
            
            Paragraph title = new Paragraph("CONTRATO DE PROMESA DE COMPRAVENTA Y ADJUDICACIÓN DE LOTE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph nitText = new Paragraph("ALMAROS S.A.S. - NIT: 901.428.115-3", boldFont);
            nitText.setAlignment(Element.ALIGN_CENTER);
            document.add(nitText);
            
            document.add(new Paragraph("\n"));
            
            Paragraph intro = new Paragraph(
                "Entre los suscritos a saber, por una parte ALMAROS S.A.S., sociedad comercial legalmente constituida, " +
                "identificada con el NIT indicado anteriormente (en adelante, EL PROMETIENTE VENDEDOR), y por la otra parte, " +
                compradorNombre.toUpperCase() + ", mayor de edad, identificado(a) con la Cédula de Ciudadanía No. " + 
                compradorCedula + " (en adelante, EL PROMETIENTE COMPRADOR), se ha convenido celebrar el presente contrato de adjudicación " +
                "y promesa de compraventa de bien inmueble, el cual se regirá por las siguientes cláusulas y la legislación colombiana vigente:",
                bodyFont
            );
            intro.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(intro);
            document.add(new Paragraph("\n"));
            
            Paragraph c1Title = new Paragraph("CLÁUSULA PRIMERA - OBJETO Y REFERENCIA:", boldFont);
            document.add(c1Title);
            Paragraph c1Text = new Paragraph(
                "EL PROMETIENTE VENDEDOR promete transferir a título de venta a favor de EL PROMETIENTE COMPRADOR, " +
                "y éste se obliga a adquirir a igual título, el bien inmueble distinguido comercialmente como LOTE NUMERO " + 
                numeroLote + " ubicado en la " + nombreEtapa + " del Proyecto Inmobiliario Mirador de San Antonio, municipio de La Calera, Cundinamarca.",
                bodyFont
            );
            c1Text.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(c1Text);
            document.add(new Paragraph("\n"));
            
            Paragraph c2Title = new Paragraph("CLÁUSULA SEGUNDA - PRECIO Y CONDICIONES FINANCIERAS:", boldFont);
            document.add(c2Title);
            Paragraph c2Text = new Paragraph(
                "El precio pactado de común acuerdo por las partes para la venta del lote de terreno prometido en venta es la suma de los valores que se relacionan a continuación:",
                bodyFont
            );
            document.add(c2Text);
            document.add(new Paragraph("\n"));
            
            // Tabla de condiciones
            PdfPTable termsTable = new PdfPTable(2);
            termsTable.setWidthPercentage(100);
            termsTable.setWidths(new float[] { 1.5f, 2f });
            
            addTableCell(termsTable, "CONCEPTO COMERCIAL", true, new Color(19, 64, 116), Color.WHITE, Element.ALIGN_LEFT);
            addTableCell(termsTable, "VALOR / DETALLE", true, new Color(19, 64, 116), Color.WHITE, Element.ALIGN_LEFT);
            
            String precioLimpio = precioPactado.replace(".", "").replace(",", "").replace("$", "").trim();
            String separacionLimpia = cuotaSeparacion.replace(".", "").replace(",", "").replace("$", "").trim();
            double saldoVal = Double.parseDouble(precioLimpio) - Double.parseDouble(separacionLimpia);
            
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
            String precioFormateado = "$ " + df.format(Double.parseDouble(precioLimpio)) + " COP";
            String separacionFormateada = "$ " + df.format(Double.parseDouble(separacionLimpia)) + " COP";
            String saldoFormateado = "$ " + df.format(saldoVal) + " COP";
            
            addTableCell(termsTable, "Precio de Venta Pactado", false, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            addTableCell(termsTable, precioFormateado, false, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            
            addTableCell(termsTable, "Cuota de Separación (Abono Inicial)", false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            addTableCell(termsTable, separacionFormateada, false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            addTableCell(termsTable, "Saldo Pendiente por Financiar", false, new Color(238, 244, 248), darkText, Element.ALIGN_LEFT);
            addTableCell(termsTable, saldoFormateado, false, new Color(238, 244, 248), darkText, Element.ALIGN_LEFT);
            
            addTableCell(termsTable, "Plazo de Financiación Directa", false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            addTableCell(termsTable, plazoMeses + " Meses", false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            document.add(termsTable);
            document.add(new Paragraph("\n"));
            
            Paragraph c3Title = new Paragraph("CLÁUSULA TERCERA - PROTOCOLIZACIÓN Y ESCRITURACIÓN:", boldFont);
            document.add(c3Title);
            Paragraph c3Text = new Paragraph(
                "La firma de la escritura pública que perfeccione la transferencia de propiedad se llevará a cabo en la Notaría acordada, una vez el PROMETIENTE COMPRADOR haya cancelado la totalidad del saldo financiado y cumpla con los requisitos legales exigidos.",
                bodyFont
            );
            c3Text.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(c3Text);
            document.add(new Paragraph("\n\n\n\n"));
            
            // Bloque de firmas
            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setWidths(new float[] { 1f, 1f });
            
            PdfPCell sigCell1 = new PdfPCell();
            sigCell1.setBorder(0);
            sigCell1.addElement(new Paragraph("____________________________________", bodyFont));
            sigCell1.addElement(new Paragraph("EL PROMETIENTE VENDEDOR", boldFont));
            sigCell1.addElement(new Paragraph("ALMAROS S.A.S. - Representante Legal", bodyFont));
            sigCell1.addElement(new Paragraph("NIT. 901.428.115-3", smallFont));
            signatureTable.addCell(sigCell1);
            
            PdfPCell sigCell2 = new PdfPCell();
            sigCell2.setBorder(0);
            sigCell2.addElement(new Paragraph("____________________________________", bodyFont));
            sigCell2.addElement(new Paragraph("EL PROMETIENTE COMPRADOR", boldFont));
            sigCell2.addElement(new Paragraph(compradorNombre, bodyFont));
            sigCell2.addElement(new Paragraph("C.C. No. " + compradorCedula, smallFont));
            signatureTable.addCell(sigCell2);
            
            document.add(signatureTable);
            
            document.close();
        } catch (Exception e) {
            throw new IOException("Error al generar PDF: " + e.getMessage(), e);
        }
        
        byte[] bytes = baos.toByteArray();

        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(bytes, "contratos/" + filename, "application/pdf");
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir contrato a Supabase: " + e.getMessage() + ". Ejecutando Fallback local.");
            }
        }

        return guardarLocalmente(bytes, filename);
    }

    /**
     * Genera un recibo de caja en formato PDF y lo guarda.
     */
    public String generarReciboPdfSimulado(String numeroRecibo, String numeroLote, String compradorNombre, 
                                           String compradorCedula, String montoRecibido, String concepto, 
                                           String vendedorNombre) throws IOException {
        
        String filename = "recibo_" + UUID.randomUUID().toString().substring(0, 8) + "_num_" + numeroRecibo + ".pdf";
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.setMargins(40f, 40f, 40f, 40f);
            document.open();
            
            Color navy = new Color(11, 37, 69);
            Color darkText = new Color(30, 41, 59);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, navy);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, darkText);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            
            // Tabla de encabezado: Empresa a la izquierda, recibo a la derecha
            PdfPTable topTable = new PdfPTable(2);
            topTable.setWidthPercentage(100);
            topTable.setWidths(new float[] { 2f, 1f });
            
            PdfPCell cellLeft = new PdfPCell();
            cellLeft.setBorder(0);
            cellLeft.addElement(new Paragraph("ALMAROS S.A.S.", new Font(Font.HELVETICA, 14, Font.BOLD, navy)));
            cellLeft.addElement(new Paragraph("NIT: 901.428.115-3", bodyFont));
            cellLeft.addElement(new Paragraph("Mirador de San Antonio - Desarrollo Campestre", smallFont));
            topTable.addCell(cellLeft);
            
            PdfPCell cellRight = new PdfPCell();
            cellRight.setBorder(0);
            Paragraph rcNum = new Paragraph("RECIBO DE CAJA", boldFont);
            rcNum.setAlignment(Element.ALIGN_RIGHT);
            Paragraph rcVal = new Paragraph("No. " + numeroRecibo, new Font(Font.HELVETICA, 14, Font.BOLD, Color.RED));
            rcVal.setAlignment(Element.ALIGN_RIGHT);
            cellRight.addElement(rcNum);
            cellRight.addElement(rcVal);
            topTable.addCell(cellRight);
            
            document.add(topTable);
            document.add(new Paragraph("\n"));
            
            // Banner de soporte
            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            PdfPCell bCell = new PdfPCell(new Phrase("SOPORTE OFICIAL DE PAGO Y REGISTRO EN CARTERA", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            bCell.setBackgroundColor(new Color(19, 64, 116));
            bCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bCell.setPadding(4f);
            bCell.setBorder(0);
            banner.addCell(bCell);
            document.add(banner);
            
            document.add(new Paragraph("\n"));
            
            // Tabla de datos
            PdfPTable dataTable = new PdfPTable(2);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[] { 1f, 2f });
            
            addTableCell(dataTable, "FECHA Y HORA DE REGISTRO", true, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            String fechaHora = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            addTableCell(dataTable, fechaHora, false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            addTableCell(dataTable, "CLIENTE / PAGADOR", true, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            addTableCell(dataTable, compradorNombre + " (C.C. " + compradorCedula + ")", false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            addTableCell(dataTable, "MONTO RECIBIDO", true, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            
            String montoLimpio = montoRecibido.replace(".", "").replace(",", "").replace("$", "").trim();
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
            String montoFormateado = "$ " + df.format(Double.parseDouble(montoLimpio)) + " COP";
            
            addTableCell(dataTable, montoFormateado, true, Color.WHITE, new Color(42, 157, 143), Element.ALIGN_LEFT);
            
            addTableCell(dataTable, "LOTE DE REFERENCIA", true, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            addTableCell(dataTable, "Lote " + numeroLote, false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            addTableCell(dataTable, "CONCEPTO DE PAGO", true, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            addTableCell(dataTable, concepto, false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            addTableCell(dataTable, "REGISTRADO POR", true, new Color(238, 244, 248), navy, Element.ALIGN_LEFT);
            addTableCell(dataTable, vendedorNombre, false, Color.WHITE, darkText, Element.ALIGN_LEFT);
            
            document.add(dataTable);
            document.add(new Paragraph("\n"));
            
            Paragraph note = new Paragraph(
                "NOTA: Este comprobante oficial acredita la recepción de fondos por el concepto indicado anteriormente. " +
                "El abono se verá reflejado inmediatamente en el estado de cuenta y plan de amortización del lote en el sistema LoteClick.",
                smallFont
            );
            note.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(note);
            document.add(new Paragraph("\n\n\n\n"));
            
            // Firmas
            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setWidths(new float[] { 1f, 1f });
            
            PdfPCell sigCell1 = new PdfPCell();
            sigCell1.setBorder(0);
            sigCell1.addElement(new Paragraph("____________________________________", bodyFont));
            sigCell1.addElement(new Paragraph("QUIEN ENTREGA (CLIENTE)", boldFont));
            sigCell1.addElement(new Paragraph("C.C. / NIT.", smallFont));
            signatureTable.addCell(sigCell1);
            
            PdfPCell sigCell2 = new PdfPCell();
            sigCell2.setBorder(0);
            sigCell2.addElement(new Paragraph("____________________________________", bodyFont));
            sigCell2.addElement(new Paragraph("QUIEN RECIBE (FIRMA AUTORIZADA)", boldFont));
            sigCell2.addElement(new Paragraph("ALMAROS S.A.S. - Tesorería", bodyFont));
            signatureTable.addCell(sigCell2);
            
            document.add(signatureTable);
            
            document.close();
        } catch (Exception e) {
            throw new IOException("Error al generar PDF: " + e.getMessage(), e);
        }
        
        byte[] bytes = baos.toByteArray();

        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(bytes, "recibos/" + filename, "application/pdf");
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir recibo a Supabase: " + e.getMessage() + ". Ejecutando Fallback local.");
            }
        }

        return guardarLocalmente(bytes, filename);
    }

    /**
     * Genera un PDF de título de propiedad simulado y lo guarda.
     */
    public String generarTituloPropiedadPdfSimulado(String numeroLote, String nombreEtapa, String compradorNombre, 
                                                    String compradorCedula) throws IOException {
        String filename = "propiedad_test_lote_" + numeroLote + "_" + nombreEtapa.replace(" ", "") + ".pdf";
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.setMargins(20f, 20f, 20f, 20f);
            document.open();
            
            Color navy = new Color(11, 37, 69);
            Color darkText = new Color(30, 41, 59);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, navy);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, darkText);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            
            // Tabla contenedora para el marco exterior
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);
            
            PdfPCell cell = new PdfPCell();
            cell.setBorder(PdfPCell.BOX);
            cell.setBorderWidth(5f);
            cell.setBorderColor(navy);
            cell.setPadding(35f);
            
            // Contenido del certificado
            Paragraph logoText = new Paragraph("ALMAROS S.A.S.", new Font(Font.HELVETICA, 12, Font.BOLD, new Color(19, 64, 116)));
            logoText.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(logoText);
            
            Paragraph nitText = new Paragraph("NIT: 901.428.115-3", smallFont);
            nitText.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(nitText);
            
            cell.addElement(new Paragraph("\n\n"));
            
            Paragraph title = new Paragraph("TÍTULO OFICIAL DE ADJUDICACIÓN DE PROPIEDAD", new Font(Font.HELVETICA, 18, Font.BOLD, navy));
            title.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(title);
            
            Paragraph subtitle = new Paragraph("PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO", new Font(Font.HELVETICA, 10, Font.BOLD, new Color(19, 64, 116)));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(subtitle);
            
            cell.addElement(new Paragraph("\n\n\n"));
            
            Paragraph text1 = new Paragraph(
                "La sociedad constructora ALMAROS S.A.S., por medio del presente título formal de adjudicación, hace constar que:",
                bodyFont
            );
            text1.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(text1);
            
            cell.addElement(new Paragraph("\n"));
            
            Paragraph ownerNameText = new Paragraph(compradorNombre.toUpperCase(), new Font(Font.HELVETICA, 16, Font.BOLD, navy));
            ownerNameText.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(ownerNameText);
            
            Paragraph ownerIdText = new Paragraph("Identificado(a) con la Cédula de Ciudadanía No. " + compradorCedula, boldFont);
            ownerIdText.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(ownerIdText);
            
            cell.addElement(new Paragraph("\n"));
            
            Paragraph text2 = new Paragraph(
                "Ha cancelado a satisfacción el cien por ciento (100%) de las obligaciones financieras adquiridas sobre el bien raíz campestre:",
                bodyFont
            );
            text2.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(text2);
            
            cell.addElement(new Paragraph("\n"));
            
            Paragraph propertyDetails = new Paragraph("LOTE NÚMERO " + numeroLote + " - " + nombreEtapa.toUpperCase(), new Font(Font.HELVETICA, 13, Font.BOLD, new Color(42, 157, 143)));
            propertyDetails.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(propertyDetails);
            
            cell.addElement(new Paragraph("\n"));
            
            Paragraph text3 = new Paragraph(
                "En mérito de lo anterior, la junta directiva y el comité comercial declaran formalmente adjudicado dicho inmueble a su favor, " +
                "autorizando el inicio del trámite de protocolización de escrituras de compraventa para la correspondiente transferencia de dominio ante la Notaría Pública respectiva.",
                bodyFont
            );
            text3.setAlignment(Element.ALIGN_JUSTIFIED);
            cell.addElement(text3);
            
            cell.addElement(new Paragraph("\n\n\n\n"));
            
            Paragraph line = new Paragraph("_____________________________________________", bodyFont);
            line.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(line);
            
            Paragraph repTitle = new Paragraph("REPRESENTANTE LEGAL Y GERENTE DE PROYECTO", boldFont);
            repTitle.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(repTitle);
            
            Paragraph repCompany = new Paragraph("ALMAROS S.A.S. - Mirador de San Antonio", smallFont);
            repCompany.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(repCompany);
            
            outerTable.addCell(cell);
            document.add(outerTable);
            
            document.close();
        } catch (Exception e) {
            throw new IOException("Error al generar PDF: " + e.getMessage(), e);
        }
        
        byte[] bytes = baos.toByteArray();
        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(bytes, "propiedades/" + filename, "application/pdf");
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir título a Supabase: " + e.getMessage() + ". Fallback local.");
            }
        }
        return guardarLocalmente(bytes, filename);
    }



    /**
     * Realiza un HTTP POST directo a la API de Supabase Storage para subir el archivo.
     */
    private String subirASupabase(byte[] fileBytes, String filePath, String contentType) throws Exception {
        // Endpoint: POST /storage/v1/object/<bucket>/<path>
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + filePath;
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            // El archivo se subió correctamente. Su URL pública es:
            // https://<project>.supabase.co/storage/v1/object/public/<bucket>/<path>
            return supabaseUrl + "/storage/v1/object/public/" + BUCKET_NAME + "/" + filePath;
        } else {
            throw new RuntimeException("Error en respuesta de Supabase Storage (Status " + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Guarda el archivo localmente en la carpeta static/uploads tanto en la carpeta de código fuente como en target para que se sirva de inmediato.
     */
    private String guardarLocalmente(byte[] bytes, String filename) throws IOException {
        // Asegurar que el directorio src/main/resources/static/uploads exista
        Path srcUploadPath = Paths.get(LOCAL_UPLOAD_DIR);
        if (!Files.exists(srcUploadPath)) {
            Files.createDirectories(srcUploadPath);
        }

        // Guardar archivo en código fuente
        Path srcFilePath = srcUploadPath.resolve(filename);
        Files.write(srcFilePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Asegurar que el directorio target/classes/static/uploads exista (para servir instantáneamente en caliente)
        Path targetUploadPath = Paths.get(TARGET_UPLOAD_DIR);
        if (!Files.exists(targetUploadPath)) {
            Files.createDirectories(targetUploadPath);
        }

        // Guardar archivo en target
        Path targetFilePath = targetUploadPath.resolve(filename);
        Files.write(targetFilePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println(">>> ARCHIVO guardado localmente en fallback: /uploads/" + filename);
        return "/uploads/" + filename;
    }

    /**
     * Guarda una imagen de lote en el storage o carpeta local.
     */
    public String guardarImagenLote(org.springframework.web.multipart.MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "lote.jpg";
        
        String sanitizedFilename = originalFilename.replaceAll("\\s+", "_");
        String finalFilename = "lote_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + sanitizedFilename;

        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(file.getBytes(), "imagenes_lotes/" + finalFilename, file.getContentType());
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir imagen a Supabase Storage: " + e.getMessage() + ". Ejecutando Fallback local.");
            }
        }

        return guardarLocalmente(file.getBytes(), finalFilename);
    }
}
