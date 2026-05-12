package com.geopatitas.api.pet.controller;

import com.geopatitas.api.pet.dto.PetRequestDTO;
import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /**
     * Endpoint para reportar una nueva mascota (perdida o encontrada).
     */
    @PostMapping
    public ResponseEntity<Pet> reportarMascota(@Valid @RequestBody PetRequestDTO requestDTO) {
        Pet petGuardado = petService.reportarMascota(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(petGuardado);
    }

    /**
     * Endpoint para buscar mascotas similares usando IA.
     * Ejemplo: GET /api/v1/pets/match?q=perrito blanco con collar rojo
     */
    @GetMapping("/match")
    public ResponseEntity<List<Pet>> buscarCoincidencias(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Pet> coincidencias = petService.buscarCoincidencias(query);
        return ResponseEntity.ok(coincidencias);
    }
}
