package com.aring.model;

public enum TipoTarea {
    MANTENIMIENTO("M", "Mantenimiento"),
    REPARACION("R", "Reparación"),
    COTIZACION("C", "Cotización");

    private final String prefijo;
    private final String etiqueta;

    TipoTarea(String prefijo, String etiqueta) {
        this.prefijo = prefijo;
        this.etiqueta = etiqueta;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
