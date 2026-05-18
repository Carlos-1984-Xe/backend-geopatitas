package com.geopatitas.api.pet.dto;

import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.pet.entity.TipoReporte;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PetMatchResponseDTO {
    private UUID id;
    private TipoReporte tipoReporte;
    private String nombre;
    private String especie;
    private String raza;
    private String color;
    private String tamano;
    private String descripcion;
    private String sexo;
    private List<String> fotos;
    private String estado;
    private LocalDateTime fechaReporte;
    private Double latitud;
    private Double longitud;
    
    // Custom match fields
    private Double porcentajeSimilitud;
    private Double distanciaKm;

    public PetMatchResponseDTO() {}

    public PetMatchResponseDTO(Pet pet, Double porcentajeSimilitud, Double distanciaKm) {
        this.id = pet.getId();
        this.tipoReporte = pet.getTipoReporte();
        this.nombre = pet.getNombre();
        this.especie = pet.getEspecie();
        this.raza = pet.getRaza();
        this.color = pet.getColor();
        this.tamano = pet.getTamano();
        this.descripcion = pet.getDescripcion();
        this.sexo = pet.getSexo();
        this.fotos = pet.getFotos();
        this.estado = pet.getEstado();
        this.fechaReporte = pet.getFechaReporte();
        this.latitud = pet.getLatitud();
        this.longitud = pet.getLongitud();
        this.porcentajeSimilitud = porcentajeSimilitud;
        this.distanciaKm = distanciaKm;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TipoReporte getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(TipoReporte tipoReporte) { this.tipoReporte = tipoReporte; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Double getPorcentajeSimilitud() { return porcentajeSimilitud; }
    public void setPorcentajeSimilitud(Double porcentajeSimilitud) { this.porcentajeSimilitud = porcentajeSimilitud; }
    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double distanciaKm) { this.distanciaKm = distanciaKm; }
}
