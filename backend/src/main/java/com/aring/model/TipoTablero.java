package com.aring.model;

public enum TipoTablero {
    MONOFASICO("Monofásico"),
    TRIFASICO("Trifásico");

    private final String etiqueta;

    TipoTablero(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
