package com.almaros.loteclick.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
     * Simula la generación de un PDF de contrato legal compilando la información y guardándola.
     */
    public String generarContratoPdfSimulado(String numeroLote, String nombreEtapa, String compradorNombre, 
                                             String compradorCedula, String precioPactado, String cuotaSeparacion, 
                                             int plazoMeses) throws IOException {
        
        String filename = "contrato_" + UUID.randomUUID().toString().substring(0, 8) + "_lote_" + numeroLote + ".txt";
        
        // Texto simulado del contrato
        String contenidoContrato = "============================================================\n" +
                "         CONTRATO DE ADJUDICACION Y COMPRAVENTA DE LOTE      \n" +
                "             PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO    \n" +
                "============================================================\n\n" +
                "PROYECTO: Mirador de San Antonio - ALMAROS S.A.S\n" +
                "UBICACION: Lote " + numeroLote + " - " + nombreEtapa + "\n\n" +
                "DATOS DEL COMPRADOR:\n" +
                "  Nombre: " + compradorNombre + "\n" +
                "  Cedula: " + compradorCedula + "\n\n" +
                "ACUERDOS COMERCIALES:\n" +
                "  Precio de Venta Pactado: $ " + precioPactado + " COP\n" +
                "  Cuota de Separacion (Abono Inicial): $ " + cuotaSeparacion + " COP\n" +
                "  Saldo Restante por Financiar: $ " + (Double.parseDouble(precioPactado) - Double.parseDouble(cuotaSeparacion)) + " COP\n" +
                "  Plazo Otorgado: " + plazoMeses + " meses\n\n" +
                "------------------------------------------------------------\n" +
                "Este documento certifica de forma perpetua y legal la compra \n" +
                "y separacion del lote citado bajo las cuotas proyectadas.    \n" +
                "============================================================\n";

        byte[] bytes = contenidoContrato.getBytes();

        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(bytes, "contratos/" + filename, "text/plain");
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir contrato a Supabase: " + e.getMessage() + ". Ejecutando Fallback local.");
            }
        }

        return guardarLocalmente(bytes, filename);
    }

    /**
     * Simula la generación de un recibo de caja en formato de texto plano y lo guarda.
     */
    public String generarReciboPdfSimulado(String numeroRecibo, String numeroLote, String compradorNombre, 
                                           String compradorCedula, String montoRecibido, String concepto, 
                                           String vendedorNombre) throws IOException {
        
        String filename = "recibo_" + UUID.randomUUID().toString().substring(0, 8) + "_num_" + numeroRecibo + ".txt";
        
        // Texto simulado del recibo de caja
        String contenidoRecibo = "============================================================\n" +
                "                   COMPROBANTE DE RECIBO DE CAJA            \n" +
                "             PROYECTO INMOBILIARIO MIRADOR DE SAN ANTONIO    \n" +
                "============================================================\n\n" +
                "RECIBO DE CAJA NRO: " + numeroRecibo + "\n" +
                "FECHA/HORA: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                "DATOS DEL COMPRADOR (CLIENTE):\n" +
                "  Nombre: " + compradorNombre + "\n" +
                "  Cedula: " + compradorCedula + "\n\n" +
                "DETALLES DEL ABONO:\n" +
                "  Lote Adquirido: Lote " + numeroLote + "\n" +
                "  Concepto de Pago: " + concepto + "\n" +
                "  Monto Pagado: $ " + montoRecibido + " COP\n\n" +
                "DATOS DE REGISTRO INTERNO:\n" +
                "  Recibido por: " + vendedorNombre + "\n" +
                "------------------------------------------------------------\n" +
                "Este recibo es un comprobante de abono valido para el saldo  \n" +
                "del lote citado en el plan de amortizacion.                 \n" +
                "============================================================\n";

        byte[] bytes = contenidoRecibo.getBytes();

        if (supabaseKey != null && !supabaseKey.trim().isEmpty()) {
            try {
                return subirASupabase(bytes, "recibos/" + filename, "text/plain");
            } catch (Exception e) {
                System.err.println(">>> ERROR al subir recibo a Supabase: " + e.getMessage() + ". Ejecutando Fallback local.");
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
}
