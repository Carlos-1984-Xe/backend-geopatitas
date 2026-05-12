package com.geopatitas.api.pet.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(length = 20)
    private String sexo;

    // Using Hibernate 6 Array mapping for list of URLs
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fotos", columnDefinition = "text[]")
    private List<String> fotos;

    // GPS coordinates
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // PGVector for embeddings
    // Adjust dimensions (e.g., 384 for all-MiniLM-L6-v2) according to your model
    @Column(columnDefinition = "vector(384)")
    private PGvector embedding;
}
