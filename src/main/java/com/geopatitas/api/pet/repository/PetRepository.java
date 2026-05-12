package com.geopatitas.api.pet.repository;

import com.geopatitas.api.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {

    // Búsqueda por similitud del coseno: el operador <=> devuelve la distancia.
    // A menor distancia, mayor similitud.
    @Query(value = "SELECT * FROM pets p ORDER BY p.embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Pet> findNearestPets(@Param("embedding") float[] embedding, @Param("limit") int limit);
}
