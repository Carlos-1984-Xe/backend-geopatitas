package com.geopatitas.api.pet.repository;

import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.entity.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {

    List<Pet> findByTipoReporte(TipoReporte tipoReporte);
    
    List<Pet> findByUserId(UUID userId);

    // Búsqueda por similitud del coseno: el operador <=> devuelve la distancia.
    // A menor distancia, mayor similitud.
    @Query(value = "SELECT * FROM pets p ORDER BY p.embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Pet> findNearestPets(@Param("embedding") float[] embedding, @Param("limit") int limit);

    // Búsqueda con umbral semántico y geográfico combinado (PostGIS + pgvector)
    @Query(value = "SELECT p.* FROM pets p " +
                   "WHERE p.tipo_reporte = :tipoOpuesto " +
                   "  AND p.estado = 'ACTIVO' " +
                   "  AND (CAST(:especie AS text) IS NULL OR p.especie = CAST(:especie AS text)) " +
                   "  AND ST_DWithin(CAST(ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326) AS geography), CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography), :maxRadiusMeters) " +
                   "  AND (1 - (p.embedding <=> CAST(:queryEmbedding AS vector))) >= :minScore " +
                   "ORDER BY (" +
                   "  0.6 * (1 - (p.embedding <=> CAST(:queryEmbedding AS vector))) + " +
                   "  0.4 * GREATEST(0, 1.0 - (ST_Distance(CAST(ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326) AS geography), CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)) / 1000.0) / :maxRadiusKm) " +
                   ") DESC LIMIT :limit", nativeQuery = true)
    List<Pet> findMatchesWithCombinedScore(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("tipoOpuesto") String tipoOpuesto,
            @Param("especie") String especie,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("maxRadiusKm") double maxRadiusKm,
            @Param("maxRadiusMeters") double maxRadiusMeters,
            @Param("minScore") double minScore,
            @Param("limit") int limit);

    // Búsqueda geográfica usando PostGIS
    // Usamos CAST(... AS geography) en lugar de \\:\\:geography para evitar que el linter del IDE marque falsos errores visuales.
    @Query(value = "SELECT * FROM pets p WHERE p.latitud IS NOT NULL AND p.longitud IS NOT NULL AND ST_DWithin(CAST(ST_MakePoint(p.longitud, p.latitud) AS geography), CAST(ST_MakePoint(:lng, :lat) AS geography), :radiusInMeters) ORDER BY ST_Distance(CAST(ST_MakePoint(p.longitud, p.latitud) AS geography), CAST(ST_MakePoint(:lng, :lat) AS geography))", nativeQuery = true)
    List<Pet> findPetsNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusInMeters") double radiusInMeters);
}
