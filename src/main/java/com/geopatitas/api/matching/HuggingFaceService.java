package com.geopatitas.api.matching;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class HuggingFaceService {

    private final RestClient restClient;
    private final String modelUrl;

    public HuggingFaceService(
            @Value("${geopatitas.ai.huggingface.api-key}") String apiKey,
            @Value("${geopatitas.ai.huggingface.model-id}") String modelId) {

        this.modelUrl = "https://api-inference.huggingface.co/pipeline/feature-extraction/" + modelId;
        
        // Configuramos el RestClient (la nueva forma recomendada en Spring Boot 3)
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Envía un texto a la API de Hugging Face y recibe el vector de embeddings.
     */
    public float[] generateEmbedding(String text) {
        // El payload esperado por HuggingFace para feature-extraction
        Map<String, String> body = Map.of("inputs", text);

        try {
            // La API de HuggingFace para sentence-transformers suele devolver un List<Float>
            // para una sola entrada, o un List<List<Float>> si hay múltiples.
            List<Float> response = restClient.post()
                    .uri(modelUrl)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Float>>() {});

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Respuesta vacía desde Hugging Face");
            }

            // Convertimos la Lista de Float a un array primitivo de float[]
            float[] vector = new float[response.size()];
            for (int i = 0; i < response.size(); i++) {
                vector[i] = response.get(i);
            }

            return vector;

        } catch (Exception e) {
            // En un sistema real lanzaríamos una CustomException (ej. AiServiceException)
            throw new RuntimeException("Error al generar el embedding con la IA: " + e.getMessage(), e);
        }
    }
}
