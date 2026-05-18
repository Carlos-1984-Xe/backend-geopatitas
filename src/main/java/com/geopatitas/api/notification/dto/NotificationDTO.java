package com.geopatitas.api.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDTO {
    private UUID id;
    private PetSummaryDTO petReportado;
    private PetSummaryDTO petCoincidencia;
    private Double porcentajeSimilitud;
    private Double distanciaKm;
    private Boolean leida;
    private LocalDateTime fechaCreacion;

    public NotificationDTO() {}

    public NotificationDTO(UUID id, PetSummaryDTO petReportado, PetSummaryDTO petCoincidencia, Double porcentajeSimilitud, Double distanciaKm, Boolean leida, LocalDateTime fechaCreacion) {
        this.id = id;
        this.petReportado = petReportado;
        this.petCoincidencia = petCoincidencia;
        this.porcentajeSimilitud = porcentajeSimilitud;
        this.distanciaKm = distanciaKm;
        this.leida = leida;
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PetSummaryDTO getPetReportado() {
        return petReportado;
    }

    public void setPetReportado(PetSummaryDTO petReportado) {
        this.petReportado = petReportado;
    }

    public PetSummaryDTO getPetCoincidencia() {
        return petCoincidencia;
    }

    public void setPetCoincidencia(PetSummaryDTO petCoincidencia) {
        this.petCoincidencia = petCoincidencia;
    }

    public Double getPorcentajeSimilitud() {
        return porcentajeSimilitud;
    }

    public void setPorcentajeSimilitud(Double porcentajeSimilitud) {
        this.porcentajeSimilitud = porcentajeSimilitud;
    }

    public Double getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(Double distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public Boolean getLeida() {
        return leida;
    }

    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
