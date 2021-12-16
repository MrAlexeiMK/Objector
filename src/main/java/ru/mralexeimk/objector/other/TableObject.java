package ru.mralexeimk.objector.other;

import java.util.List;

public class TableObject {
    private String id;
    private List<Object> layers;
    private double lr;

    public TableObject(String id, List<Object> layers, double lr) {
        this.id = id;
        this.layers = layers;
        this.lr = lr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Object> getLayers() {
        return layers;
    }

    public void setLayers(List<Object> layers) {
        this.layers = layers;
    }

    public double getLr() {
        return lr;
    }

    public void setLr(double lr) {
        this.lr = lr;
    }
}
