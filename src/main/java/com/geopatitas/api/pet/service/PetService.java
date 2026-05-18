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
            if (dto.getLatitud() != null && dto.getLongitud() != null) {
                String tipoOpuesto = dto.getTipoReporte().name().equals("PERDIDO") ? "ENCONTRADO" : "PERDIDO";
                List<Pet> matches = petRepository.findMatchesWithCombinedScore(
                    vector, tipoOpuesto, dto.getLatitud(), dto.getLongitud(), 
                    15.0, 15000.0, 0.30, 5
                );
                
                for (Pet match : matches) {
                    // Ignorar si no tiene coordenadas
                    if (match.getLatitud() == null || match.getLongitud() == null) continue;

                    double distKm = distanceInKm(dto.getLatitud(), dto.getLongitud(), match.getLatitud(), match.getLongitud());
                    double sim = calculateCosineSimilarity(vector, match.getEmbedding());
                    double combinedScore = (0.6 * sim + 0.4 * Math.max(0, 1.0 - distKm / 15.0)) * 100.0;
                    
                    // Solo notificar si la coincidencia combinada es alta
                    if (combinedScore < 60.0) continue;

                    // Evitar notificaciones para el mismo usuario
                    if (savedPet.getUser().getId().equals(match.getUser().getId())) {
                        continue;
                    }

                    // Notificar al dueño del nuevo reporte
                    MatchNotification notif1 = new MatchNotification();
                    notif1.setUser(savedPet.getUser());
                    notif1.setPetReportado(savedPet);
                    notif1.setPetCoincidencia(match);
                    notif1.setPorcentajeSimilitud(combinedScore);
                    notif1.setDistanciaKm(distKm);
                    matchNotificationRepository.save(notif1);

                    // Notificar al dueño del reporte antiguo
                    MatchNotification notif2 = new MatchNotification();
                    notif2.setUser(match.getUser());
                    notif2.setPetReportado(match);
                    notif2.setPetCoincidencia(savedPet);
                    notif2.setPorcentajeSimilitud(combinedScore);
                    notif2.setDistanciaKm(distKm);
                    matchNotificationRepository.save(notif2);
                }
            }
        } catch (Exception e) {
            System.err.println("Error generando notificaciones de match: " + e.getMessage());
            e.printStackTrace();
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

    public List<com.geopatitas.api.pet.dto.PetMatchResponseDTO> buscarCoincidencias(
            String descripcion, String tipoOpuesto, double lat, double lng, 
            double maxRadiusKm, double minScore, int limit) {
        try {
            float[] vectorBusqueda = huggingFaceService.generateEmbedding(descripcion);
            double maxRadiusMeters = maxRadiusKm * 1000.0;
            
            List<Pet> result = petRepository.findMatchesWithCombinedScore(
                vectorBusqueda, tipoOpuesto, lat, lng, maxRadiusKm, maxRadiusMeters, minScore, limit
            );

            return result.stream().map(pet -> {
                double distKm = distanceInKm(lat, lng, pet.getLatitud(), pet.getLongitud());
                double sim = calculateCosineSimilarity(vectorBusqueda, pet.getEmbedding());
                double combinedScore = (0.6 * sim + 0.4 * Math.max(0, 1.0 - distKm / maxRadiusKm)) * 100.0;
                return new com.geopatitas.api.pet.dto.PetMatchResponseDTO(pet, combinedScore, distKm);
            }).toList();
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO EN BUSCAR COINCIDENCIAS: " + e.getMessage());
            throw e;
        }
    }

    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        int R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2 - lat1);
        double dLon = deg2rad(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; 
    }

    private double deg2rad(double deg) {
        return deg * (Math.PI / 180);
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
        
        if (dto.getEstado() != null) {
            pet.setEstado(dto.getEstado());
        }
        
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
