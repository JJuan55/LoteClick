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
            document.open();
            
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            
            document.add(new Paragraph("============================================================", bodyFont));
            Paragraph title = new Paragraph("CONTRATO DE ADJUDICACION Y COMPRAVENTA DE LOTE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph subtitle = new Paragraph("PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO", boldFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph("============================================================", bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("PROYECTO: Mirador de San Antonio - ALMAROS S.A.S", boldFont));
            document.add(new Paragraph("UBICACION: Lote " + numeroLote + " - " + nombreEtapa, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("DATOS DEL COMPRADOR:", boldFont));
            document.add(new Paragraph("  Nombre: " + compradorNombre, bodyFont));
            document.add(new Paragraph("  Cedula: " + compradorCedula, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("ACUERDOS COMERCIALES:", boldFont));
            document.add(new Paragraph("  Precio de Venta Pactado: $ " + precioPactado + " COP", bodyFont));
            document.add(new Paragraph("  Cuota de Separacion (Abono Inicial): $ " + cuotaSeparacion + " COP", bodyFont));
            
            // Limpiar formatos antes de parsear
            String precioLimpio = precioPactado.replace(".", "").replace(",", "").replace("$", "").trim();
            String separacionLimpia = cuotaSeparacion.replace(".", "").replace(",", "").replace("$", "").trim();
            double saldoVal = Double.parseDouble(precioLimpio) - Double.parseDouble(separacionLimpia);
            
            document.add(new Paragraph("  Saldo Restante por Financiar: $ " + saldoVal + " COP", bodyFont));
            document.add(new Paragraph("  Plazo Otorgado: " + plazoMeses + " meses", bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("------------------------------------------------------------", bodyFont));
            document.add(new Paragraph("Este documento certifica de forma de contrato de adjudicación la compra", bodyFont));
            document.add(new Paragraph("y separacion del lote citado bajo las cuotas proyectadas.", bodyFont));
            document.add(new Paragraph("============================================================", bodyFont));
            
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
            document.open();
            
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            
            document.add(new Paragraph("============================================================", bodyFont));
            Paragraph title = new Paragraph("COMPROBANTE DE RECIBO DE CAJA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph subtitle = new Paragraph("PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO", boldFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph("============================================================", bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("RECIBO DE CAJA NRO: " + numeroRecibo, boldFont));
            document.add(new Paragraph("FECHA/HORA: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("DATOS DEL COMPRADOR (CLIENTE):", boldFont));
            document.add(new Paragraph("  Nombre: " + compradorNombre, bodyFont));
            document.add(new Paragraph("  Cedula: " + compradorCedula, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("DETALLES DEL ABONO:", boldFont));
            document.add(new Paragraph("  Lote Adquirido: Lote " + numeroLote, bodyFont));
            document.add(new Paragraph("  Concepto de Pago: " + concepto, bodyFont));
            document.add(new Paragraph("  Monto Pagado: $ " + montoRecibido + " COP", bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("DATOS DE REGISTRO INTERNO:", boldFont));
            document.add(new Paragraph("  Recibido por: " + vendedorNombre, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("------------------------------------------------------------", bodyFont));
            document.add(new Paragraph("Este recibo es un comprobante de abono valido para el saldo", bodyFont));
            document.add(new Paragraph("del lote citado en el plan de amortizacion.", bodyFont));
            document.add(new Paragraph("============================================================", bodyFont));
            
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
            document.open();
            
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            
            document.add(new Paragraph("============================================================", bodyFont));
            Paragraph title = new Paragraph("TITULO DE PROPIEDAD DIGITALIZADO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph subtitle = new Paragraph("PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO", boldFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph("============================================================", bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("PROYECTO: Mirador de San Antonio - ALMAROS S.A.S", boldFont));
            document.add(new Paragraph("UBICACION: Lote " + numeroLote + " - " + nombreEtapa, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("DATOS DEL PROPIETARIO:", boldFont));
            document.add(new Paragraph("  Nombre: " + compradorNombre, bodyFont));
            document.add(new Paragraph("  Cedula: " + compradorCedula, bodyFont));
            document.add(new Paragraph("\n", bodyFont));
            
            document.add(new Paragraph("------------------------------------------------------------", bodyFont));
            document.add(new Paragraph("Este documento representa el título de propiedad digitalizado del lote citado.", bodyFont));
            document.add(new Paragraph("============================================================", bodyFont));
            
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
