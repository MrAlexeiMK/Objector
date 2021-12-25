package ru.mralexeimk.objector.other;

import java.io.Serializable;

public enum LayerType implements Serializable {
    INPUT("INPUT"),
    OUTPUT("OUTPUT"),
    FILTER("FILTER"),
    PULLING("PULLING"),
    NEURONS("NEURONS");

    private String layer;

    LayerType(String layer) {
        this.layer = layer;
    }

    public String toString() {
        return layer;
    }
}
