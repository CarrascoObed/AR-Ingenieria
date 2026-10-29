package com.aring.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class IniciarRequest {

    private List<Long> ayudantesIds = new ArrayList<>();
}
