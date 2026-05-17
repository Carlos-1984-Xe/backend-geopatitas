package com.geopatitas.api.pet.controller;

import com.geopatitas.api.pet.dto.PetRequestDTO;
import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.service.PetService;
import com.geopatitas.api.pet.service.SupabaseStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pets")
public class PetController {

    private final PetService petService;
    private final SupabaseStorageService storageService;

    public PetController(PetService petService, SupabaseStorageService storageService) {
        this.petService = petService;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<Pet> reportarMascota(@Valid @RequestBody PetRequestDTO requestDTO) {
        Pet petGuardado = petService.reportarMascota(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(petGuardado);
    }

    @PostMapping("/guest")
    public ResponseEntity<Pet> reportarMascotaInvitado(@Valid @RequestBody PetRequestDTO requestDTO) {
        // userId debería ser null, se usará contactoEmail para crear el fantasma
        Pet petGuardado = petService.reportarMascota(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(petGuardado);
    }

    @GetMapping
    public ResponseEntity<List<Pet>> listarTodas() {
        return ResponseEntity.ok(petService.listarTodas());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarMascota(@PathVariable java.util.UUID id, @Valid @RequestBody PetRequestDTO requestDTO) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("No autorizado");
        }
        
        Pet petActualizado = petService.actualizarMascota(id, requestDTO, auth.getName());
        return ResponseEntity.ok(petActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMascota(@PathVariable java.util.UUID id) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("No autorizado");
        }
        
        petService.eliminarMascota(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pet> obtenerPorId(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(petService.obtenerPorId(id));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Pet> cambiarEstado(@PathVariable java.util.UUID id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(petService.cambiarEstado(id, nuevoEstado));
    }

    @GetMapping("/match")
    public ResponseEntity<List<Pet>> buscarCoincidencias(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Pet> coincidencias = petService.buscarCoincidencias(query);
        return ResponseEntity.ok(coincidencias);
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String publicUrl = storageService.uploadFile(file);
            return ResponseEntity.ok(Map.of("url", publicUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Pet>> buscarCercanos(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam(value = "radius", defaultValue = "5000") Double radius,
            @RequestParam(value = "tipoReporte", required = false) String tipoReporte,
            @RequestParam(value = "especie", required = false) String especie,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "color", required = false) String color,
            @RequestParam(value = "tamano", required = false) String tamano) {
        
        List<Pet> cercanos = petService.buscarCercanos(lat, lng, radius);
        
        // Filtros en memoria
        if (tipoReporte != null && !tipoReporte.isEmpty()) {
            cercanos = cercanos.stream().filter(p -> tipoReporte.equalsIgnoreCase(p.getTipoReporte().name())).toList();
        }
        if (especie != null && !especie.isEmpty()) {
            cercanos = cercanos.stream().filter(p -> especie.equalsIgnoreCase(p.getEspecie())).toList();
        }
        if (estado != null && !estado.isEmpty()) {
            cercanos = cercanos.stream().filter(p -> estado.equalsIgnoreCase(p.getEstado())).toList();
        }
        if (color != null && !color.isEmpty()) {
            cercanos = cercanos.stream().filter(p -> color.equalsIgnoreCase(p.getColor())).toList();
        }
        if (tamano != null && !tamano.isEmpty()) {
            cercanos = cercanos.stream().filter(p -> tamano.equalsIgnoreCase(p.getTamano())).toList();
        }

        return ResponseEntity.ok(cercanos);
    }
}
