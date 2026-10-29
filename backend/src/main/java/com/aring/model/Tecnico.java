package com.aring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tecnico")
public class Tecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String rut;

    @Column(nullable = false)
    private String password;

    @Column(name = "contacto_nombre")
    private String contactoNombre;

    @Column(name = "contacto_apellido")
    private String contactoApellido;

    @Column(name = "contacto_telefono")
    private String contactoTelefono;

    @Column(name = "fecha_creacion")
    private Instant fechaCreacion;
}
