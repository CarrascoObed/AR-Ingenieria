package com.aring.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ActualizarAyudantesRequest {

    @Size(max = 5, message = "Una tarea admite hasta cinco ayudantes.")
    private List<Long> ayudantesIds = new ArrayList<>();
}
