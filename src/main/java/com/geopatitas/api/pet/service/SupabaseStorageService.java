package com.geopatitas.api.pet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class SupabaseStorageService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String bucketName;

    public SupabaseStorageService(
            @Value("${geopatitas.supabase.url}") String supabaseUrl,
            @Value("${geopatitas.supabase.api-key}") String apiKey,
            @Value("${geopatitas.supabase.bucket}") String bucketName) {
            
        this.supabaseUrl = supabaseUrl;
        this.bucketName = bucketName;
        
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    /**
     * Sube un archivo a Supabase Storage y devuelve la URL pública.
     */
    public String uploadFile(MultipartFile file) {
        try {
            // Generar un nombre único para el archivo
            String extension = "";
            if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
                extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;
            
            // Ruta del endpoint de Storage: /storage/v1/object/{bucketName}/{fileName}
            String uri = "/storage/v1/object/" + bucketName + "/" + fileName;

            // Enviar archivo a Supabase
            restClient.post()
                    .uri(uri)
                    .contentType(MediaType.parseMediaType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();

            // Devolver la URL pública del archivo subido
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
            
        } catch (Exception e) {
            throw new RuntimeException("Error al subir imagen a Supabase Storage: " + e.getMessage(), e);
        }
    }
}
