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

    // Búsqueda vectorial simple (usa operador <=> para calcular distancia coseno)
    @Query(value = "SELECT * FROM pets p ORDER BY p.embedding <=> CAST(:embedding AS vector) LIMIT :limit", nativeQuery = true)
    List<Pet> findNearestPets(@Param("embedding") float[] embedding, @Param("limit") int limit);

    // Búsqueda principal del Matchmaking (PostGIS + pgvector) con castigos matemáticos
    @Query(value = "SELECT p.* FROM pets p " +
                   "WHERE p.tipo_reporte = :tipoOpuesto " +
                   "  AND p.estado = 'ACTIVO' " +
                   "  AND (CAST(:especie AS text) IS NULL OR p.especie = CAST(:especie AS text)) " +
                   "  AND ST_DWithin(CAST(ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326) AS geography), CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography), :maxRadiusMeters) " +
                   "  AND (1 - (p.embedding <=> CAST(:queryEmbedding AS vector))) >= :minScore " +
                   "ORDER BY (" +
                   "  0.75 * (1 - (p.embedding <=> CAST(:queryEmbedding AS vector))) + " +
                   "  0.25 * GREATEST(0, 1.0 - (ST_Distance(CAST(ST_SetSRID(ST_MakePoint(p.longitud, p.latitud), 4326) AS geography), CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)) / 1000.0) / :maxRadiusKm) " +
                   "  - (CASE WHEN CAST(:color AS text) IS NOT NULL AND p.color IS NOT NULL AND LOWER(p.color) <> LOWER(CAST(:color AS text)) THEN 0.15 ELSE 0 END) " +
                   "  - (CASE WHEN CAST(:sexo AS text) IS NOT NULL AND p.sexo IS NOT NULL AND LOWER(p.sexo) <> LOWER(CAST(:sexo AS text)) THEN 0.15 ELSE 0 END) " +
                   "  - (CASE WHEN CAST(:tamano AS text) IS NOT NULL AND p.tamano IS NOT NULL AND LOWER(p.tamano) <> LOWER(CAST(:tamano AS text)) THEN 0.15 ELSE 0 END) " +
                   ") DESC LIMIT :limit", nativeQuery = true)
    List<Pet> findMatchesWithCombinedScore(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("tipoOpuesto") String tipoOpuesto,
            @Param("especie") String especie,
            @Param("color") String color,
            @Param("sexo") String sexo,
            @Param("tamano") String tamano,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("maxRadiusKm") double maxRadiusKm,
            @Param("maxRadiusMeters") double maxRadiusMeters,
            @Param("minScore") double minScore,
            @Param("limit") int limit);

    // Obtiene mascotas en un radio específico usando PostGIS (usamos CAST para evitar errores de linting en Java)
    @Query(value = "SELECT * FROM pets p WHERE p.latitud IS NOT NULL AND p.longitud IS NOT NULL AND ST_DWithin(CAST(ST_MakePoint(p.longitud, p.latitud) AS geography), CAST(ST_MakePoint(:lng, :lat) AS geography), :radiusInMeters) ORDER BY ST_Distance(CAST(ST_MakePoint(p.longitud, p.latitud) AS geography), CAST(ST_MakePoint(:lng, :lat) AS geography))", nativeQuery = true)
    List<Pet> findPetsNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusInMeters") double radiusInMeters);
}
