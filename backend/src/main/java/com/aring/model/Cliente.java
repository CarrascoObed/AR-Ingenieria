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
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String direccion;

    @Column(name = "nombre_recinto", nullable = false)
    private String nombreRecinto;

    @Column(name = "imagen_base64", columnDefinition = "text")
    private String imagenBase64;

    @Column(name = "imagen_tipo")
    private String imagenTipo;

    @Column(name = "fecha_creacion")
    private Instant fechaCreacion;
}
