package com.geopatitas.api.notification.dto;

import java.util.UUID;

public class PetSummaryDTO {
    private UUID id;
    private String nombre;
    private String especie;

    public PetSummaryDTO() {}

    public PetSummaryDTO(UUID id, String nombre, String especie) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }
}
