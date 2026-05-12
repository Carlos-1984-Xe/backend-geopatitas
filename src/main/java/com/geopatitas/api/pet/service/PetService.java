package com.geopatitas.api.pet.service;

import com.geopatitas.api.matching.HuggingFaceService;
import com.geopatitas.api.pet.dto.PetRequestDTO;
import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.repository.PetRepository;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final HuggingFaceService huggingFaceService;

    @Transactional
    public Pet reportarMascota(PetRequestDTO dto) {
        // 1. Llamamos a la IA para convertir la descripción en un vector de características
        float[] vector = huggingFaceService.generateEmbedding(dto.getDescripcion());
        PGvector embedding = new PGvector(vector);

        // 2. Creamos la entidad
        Pet pet = Pet.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .sexo(dto.getSexo())
                .fotos(dto.getFotos())
                .latitud(dto.getLatitud())
                .longitud(dto.getLongitud())
                .embedding(embedding) // Asignamos el vector numérico
                .build();
        
        // 3. Guardamos en base de datos
        return petRepository.save(pet);
    }

    public List<Pet> buscarCoincidencias(String descripcion) {
        // 1. Convertir la descripción de búsqueda a un embedding
        float[] vectorBusqueda = huggingFaceService.generateEmbedding(descripcion);
        PGvector embeddingQuery = new PGvector(vectorBusqueda);

        // 2. Buscar en la base de datos usando similitud del coseno (Límite 5)
        return petRepository.findNearestPets(embeddingQuery, 5);
    }
}
