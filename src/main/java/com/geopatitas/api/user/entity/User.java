package com.geopatitas.api.user.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol;

    @Column
    private String telefono;

    public User() {}

    public User(UUID id, String nombre, String email, String password, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
