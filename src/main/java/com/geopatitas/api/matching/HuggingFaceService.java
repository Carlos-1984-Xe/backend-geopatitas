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

        this.modelUrl = "https://router.huggingface.co/hf-inference/models/" + modelId + "/pipeline/feature-extraction";
        
        // Cliente HTTP con token de HuggingFace
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Convierte texto a embedding (vector numérico)
    public float[] generateEmbedding(String text) {
        Map<String, String> body = Map.of("inputs", text);

        try {
            // Retorna un array de floats con el vector
            List<Float> response = restClient.post()
                    .uri(modelUrl)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Float>>() {});

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Respuesta vacía desde Hugging Face");
            }

            float[] vector = new float[response.size()];
            for (int i = 0; i < response.size(); i++) {
                vector[i] = response.get(i);
            }

            return vector;

        } catch (Exception e) {
            throw new RuntimeException("Error llamando a Hugging Face: " + e.getMessage(), e);
        }
    }
}
