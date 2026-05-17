package com.geopatitas.api.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import com.geopatitas.api.pet.entity.TipoReporte;

public class PetRequestDTO {
    private UUID userId;

    private String contactoNombre;
    private String contactoEmail;
    private String contactoTelefono;

    @NotNull(message = "El tipo de reporte es obligatorio (PERDIDO/ENCONTRADO)")
    private TipoReporte tipoReporte;

    private String nombre;

    @NotBlank(message = "La especie es obligatoria")
    private String especie;
    private String raza;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    private String sexo;
    private String tamano;
    private String color;
    private List<String> fotos;

    private Double latitud;

    private Double longitud;

    // Getters and Setters
    public java.util.UUID getUserId() { return userId; }
    public void setUserId(java.util.UUID userId) { this.userId = userId; }
    public String getContactoNombre() { return contactoNombre; }
    public void setContactoNombre(String contactoNombre) { this.contactoNombre = contactoNombre; }
    public String getContactoEmail() { return contactoEmail; }
    public void setContactoEmail(String contactoEmail) { this.contactoEmail = contactoEmail; }
    public String getContactoTelefono() { return contactoTelefono; }
    public void setContactoTelefono(String contactoTelefono) { this.contactoTelefono = contactoTelefono; }
    public com.geopatitas.api.pet.entity.TipoReporte getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(com.geopatitas.api.pet.entity.TipoReporte tipoReporte) { this.tipoReporte = tipoReporte; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<String> getFotos() { return fotos; }
    public void setFotos(List<String> fotos) { this.fotos = fotos; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
}
