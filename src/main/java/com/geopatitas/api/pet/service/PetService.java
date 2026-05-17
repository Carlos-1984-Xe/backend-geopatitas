package com.geopatitas.api.pet.service;

import com.geopatitas.api.matching.HuggingFaceService;
import com.geopatitas.api.pet.dto.PetRequestDTO;
import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.repository.PetRepository;
import com.geopatitas.api.user.entity.User;
import com.geopatitas.api.user.repository.UserRepository;
import com.geopatitas.api.notification.entity.MatchNotification;
import com.geopatitas.api.notification.repository.MatchNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final HuggingFaceService huggingFaceService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchNotificationRepository matchNotificationRepository;

    public PetService(PetRepository petRepository, HuggingFaceService huggingFaceService, UserRepository userRepository, PasswordEncoder passwordEncoder, MatchNotificationRepository matchNotificationRepository) {
        this.petRepository = petRepository;
        this.huggingFaceService = huggingFaceService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.matchNotificationRepository = matchNotificationRepository;
    }

    @Transactional
    public Pet reportarMascota(PetRequestDTO dto) {
        User user;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.getUserId()));
        } else {
            if (dto.getContactoEmail() == null || dto.getContactoEmail().isEmpty()) {
                throw new RuntimeException("Se requiere un email de contacto para reportar como invitado");
            }
            // Lógica de usuario fantasma
            user = userRepository.findByEmail(dto.getContactoEmail()).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(dto.getContactoEmail());
                newUser.setNombre("Invitado");
                // Contraseña aleatoria imposible de adivinar
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                newUser.setRol("GUEST");
                return userRepository.save(newUser);
            });
        }

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
        pet.setTamano(dto.getTamano());
        pet.setColor(dto.getColor());
        pet.setContactoNombre(dto.getContactoNombre());
        pet.setContactoEmail(dto.getContactoEmail());
        pet.setContactoTelefono(dto.getContactoTelefono());
        pet.setFotos(dto.getFotos());
        pet.setLatitud(dto.getLatitud());
        pet.setLongitud(dto.getLongitud());
        pet.setEmbedding(vector); 
        
        // 3. Guardamos en base de datos
        Pet savedPet = petRepository.save(pet);
        
        // 4. Buscar coincidencias automáticamente (MATCHMAKING)
        try {
            List<Pet> matches = petRepository.findMatchesWithThreshold(vector, dto.getTipoReporte().name());
            for (Pet match : matches) {
                // Calcular similitud aproximada
                double similarity = calculateCosineSimilarity(vector, match.getEmbedding()) * 100.0;
                
                // Evitar notificaciones para el mismo usuario
                if (savedPet.getUser().getId().equals(match.getUser().getId())) {
                    continue;
                }

                // Notificar al dueño del nuevo reporte
                MatchNotification notif1 = new MatchNotification();
                notif1.setUser(savedPet.getUser());
                notif1.setPetReportado(savedPet);
                notif1.setPetCoincidencia(match);
                notif1.setPorcentajeSimilitud(similarity);
                matchNotificationRepository.save(notif1);

                // Notificar al dueño del reporte antiguo
                MatchNotification notif2 = new MatchNotification();
                notif2.setUser(match.getUser());
                notif2.setPetReportado(match);
                notif2.setPetCoincidencia(savedPet);
                notif2.setPorcentajeSimilitud(similarity);
                matchNotificationRepository.save(notif2);
            }
        } catch (Exception e) {
            System.err.println("Error generando notificaciones de match: " + e.getMessage());
            // No detenemos la creación del reporte si falla el matchmaking
        }

        return savedPet;
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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

    public Pet obtenerPorId(UUID id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada"));
    }

    @Transactional
    public Pet cambiarEstado(UUID id, String nuevoEstado) {
        Pet pet = obtenerPorId(id);
        pet.setEstado(nuevoEstado);
        return petRepository.save(pet);
    }

    public List<Pet> obtenerMascotasPorUsuario(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));
        return petRepository.findByUserId(user.getId());
    }

    @Transactional
    public Pet actualizarMascota(UUID id, PetRequestDTO dto, String email) {
        Pet pet = obtenerPorId(id);
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        if (!pet.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("No tienes permiso para editar este reporte");
        }

        pet.setTipoReporte(dto.getTipoReporte());
        pet.setNombre(dto.getNombre());
        pet.setEspecie(dto.getEspecie());
        pet.setRaza(dto.getRaza());
        
        // Si la descripción cambia, regenerar el embedding
        if (!pet.getDescripcion().equals(dto.getDescripcion())) {
            pet.setDescripcion(dto.getDescripcion());
            pet.setEmbedding(huggingFaceService.generateEmbedding(dto.getDescripcion()));
        }
        
        pet.setSexo(dto.getSexo());
        pet.setTamano(dto.getTamano());
        pet.setColor(dto.getColor());
        
        if (dto.getFotos() != null && !dto.getFotos().isEmpty()) {
            pet.setFotos(dto.getFotos());
        }
        
        if (dto.getLatitud() != null) pet.setLatitud(dto.getLatitud());
        if (dto.getLongitud() != null) pet.setLongitud(dto.getLongitud());

        return petRepository.save(pet);
    }

    @Transactional
    public void eliminarMascota(UUID id, String email) {
        Pet pet = obtenerPorId(id);
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        if (!pet.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar este reporte");
        }
        
        petRepository.delete(pet);
    }
}
