package com.aring.dto;

import com.aring.model.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDto {

    private Long id;
    private String nombre;
    private String apellido;
    private String username;

    public static AdminDto from(Admin admin) {
        return new AdminDto(admin.getId(), admin.getNombre(), admin.getApellido(), admin.getUsername());
    }
}
