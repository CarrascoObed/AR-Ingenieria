package com.aring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String nombre;
    private String apellido;
    private String iniciales;
    private String username;
    private String rol;
    private Long tecnicoId;
}
