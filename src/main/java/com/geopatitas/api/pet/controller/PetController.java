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

    @GetMapping
    public ResponseEntity<List<Pet>> listarTodas() {
        return ResponseEntity.ok(petService.listarTodas());
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
            @RequestParam(value = "radius", defaultValue = "5000") Double radius) {
        
        List<Pet> cercanos = petService.buscarCercanos(lat, lng, radius);
        return ResponseEntity.ok(cercanos);
    }
}
