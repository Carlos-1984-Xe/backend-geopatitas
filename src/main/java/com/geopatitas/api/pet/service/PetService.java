package com.geopatitas.api.pet.service;

import com.geopatitas.api.matching.HuggingFaceService;
import com.geopatitas.api.pet.dto.PetRequestDTO;
import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.repository.PetRepository;
import com.geopatitas.api.user.entity.User;
import com.geopatitas.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final HuggingFaceService huggingFaceService;
    private final UserRepository userRepository;

    public PetService(PetRepository petRepository, HuggingFaceService huggingFaceService, UserRepository userRepository) {
        this.petRepository = petRepository;
        this.huggingFaceService = huggingFaceService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Pet reportarMascota(PetRequestDTO dto) {
        // Buscamos el usuario por el ID (Por ahora simulamos que viene en el DTO, en Fase 2 vendrá del Token JWT)
        User user = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.getUserId()));

        // 1. Llamamos a la IA para convertir la descripción en un vector de características
        float[] vector = huggingFaceService.generateEmbedding(dto.getDescripcion());

        // 2. Creamos la entidad
        Pet pet = new Pet();
        pet.setTipoReporte(dto.getTipoReporte());
        pet.setUser(user);
        pet.setNombre(dto.getNombre());
        pet.setEspecie(dto.getEspecie());
        pet.setRaza(dto.getRaza());
        pet.setDescripcion(dto.getDescripcion());
        pet.setSexo(dto.getSexo());
        pet.setFotos(dto.getFotos());
        pet.setLatitud(dto.getLatitud());
        pet.setLongitud(dto.getLongitud());
        pet.setEmbedding(vector); 
        
        // 3. Guardamos en base de datos
        return petRepository.save(pet);
    }

    public List<Pet> listarTodas() {
        return petRepository.findAll();
    }

    public List<Pet> buscarCoincidencias(String descripcion) {
        try {
            System.out.println("Buscando coincidencias para: " + descripcion);
            // 1. Convertir la descripción de búsqueda a un embedding
            float[] vectorBusqueda = huggingFaceService.generateEmbedding(descripcion);

            System.out.println("Vector generado correctamente. Consultando base de datos...");
            // 2. Buscar en la base de datos usando similitud del coseno (Límite 5)
            List<Pet> result = petRepository.findNearestPets(vectorBusqueda, 5);
            System.out.println("Búsqueda finalizada. Resultados encontrados: " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO EN BUSCAR COINCIDENCIAS: " + e.getMessage());
            throw e;
        }
    }

    public List<Pet> buscarCercanos(double lat, double lng, double radioMetros) {
        System.out.println("Buscando mascotas a " + radioMetros + " metros de lat: " + lat + ", lng: " + lng);
        return petRepository.findPetsNearby(lat, lng, radioMetros);
    }
}
