package com.aring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "medicion_tablero")
public class MedicionTablero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variable_id", nullable = false)
    private String variableId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private String valor;

    @Column(name = "foto_base64", columnDefinition = "text")
    private String fotoBase64;
}
