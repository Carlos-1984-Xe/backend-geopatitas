package com.geopatitas.api.notification.entity;

import com.geopatitas.api.pet.entity.Pet;
import com.geopatitas.api.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_notifications")
public class MatchNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_reportado_id", nullable = false)
    private Pet petReportado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_coincidencia_id", nullable = false)
    private Pet petCoincidencia;

    @Column(nullable = false)
    private Double porcentajeSimilitud;

    @Column
    private Double distanciaKm;

    @Column(nullable = false)
    private Boolean leida = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Pet getPetReportado() {
        return petReportado;
    }

    public void setPetReportado(Pet petReportado) {
        this.petReportado = petReportado;
    }

    public Pet getPetCoincidencia() {
        return petCoincidencia;
    }

    public void setPetCoincidencia(Pet petCoincidencia) {
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
