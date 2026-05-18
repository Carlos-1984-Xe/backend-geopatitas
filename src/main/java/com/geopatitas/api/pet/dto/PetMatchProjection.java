package com.geopatitas.api.pet.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PetMatchProjection {
    UUID getId();
    String getTipoReporte();
    String getNombre();
    String getEspecie();
    String getRaza();
    String getColor();
    String getTamano();
    String getDescripcion();
    String getSexo();
    String getEstado();
    LocalDateTime getFechaReporte();
    Double getLatitud();
    Double getLongitud();
    
    // Alias from PostGIS query
    Double getPorcentajeSimilitud();
    Double getDistanciaKm();
}
