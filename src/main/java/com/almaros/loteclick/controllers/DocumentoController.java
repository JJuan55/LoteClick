package com.almaros.loteclick.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {

    private static final String LOCAL_UPLOAD_DIR = "src/main/resources/static/uploads";
    private static final String TARGET_UPLOAD_DIR = "target/classes/static/uploads";

    @GetMapping("/descargar")
    public ResponseEntity<?> descargarDocumento(
            @RequestParam("url") String fileUrl,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {
        
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("La URL del archivo es requerida");
        }

        try {
            byte[] fileBytes;
            String filename = "documento.pdf";

            // Extraer el nombre del archivo de la URL/ruta
            if (fileUrl.contains("/")) {
                filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            }

            // Si es un archivo local (empieza con /uploads/ o es relativo)
            if (fileUrl.startsWith("/") || !fileUrl.startsWith("http")) {
                // Limpiar el path para obtener solo el nombre del archivo
                String cleanFilename = filename;
                
                // Buscar el archivo en la carpeta de uploads local o target
                Path targetPath = Paths.get(TARGET_UPLOAD_DIR).resolve(cleanFilename);
                Path srcPath = Paths.get(LOCAL_UPLOAD_DIR).resolve(cleanFilename);
                
                File file = targetPath.toFile();
                if (!file.exists()) {
                    file = srcPath.toFile();
                }
                
                if (!file.exists()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Archivo no encontrado en el servidor local: " + cleanFilename);
                }
                
                fileBytes = Files.readAllBytes(file.toPath());
            } else {
                // Si es un archivo de Supabase (comienza con http/https)
                // Hacemos una petición GET para obtener los bytes y servirla localmente
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(fileUrl))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                
                if (response.statusCode() != 200) {
                    return ResponseEntity.status(response.statusCode())
                            .body("Error al descargar de almacenamiento en la nube: Status " + response.statusCode());
                }
                fileBytes = response.body();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            
            String dispositionType = download ? "attachment" : "inline";
            headers.setContentDispositionFormData(dispositionType, filename);
            // También establecer la cabecera directamente para asegurar compatibilidad en todos los navegadores
            headers.set(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + filename + "\"");
            
            // Deshabilitar cache para asegurar que se descarguen archivos actualizados
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar el archivo: " + e.getMessage());
        }
    }
}
