package com.aring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardStatsDto {

    private long noIniciadas;
    private long enProceso;
    private long finalizadas;
}
