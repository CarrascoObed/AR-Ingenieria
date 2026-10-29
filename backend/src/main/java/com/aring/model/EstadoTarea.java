package com.aring.model;

public enum EstadoTarea {
    NO_INICIADA("No iniciada"),
    EN_PROCESO("En proceso"),
    FINALIZADA("Finalizada");

    private final String etiqueta;

    EstadoTarea(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
