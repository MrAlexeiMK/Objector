package ru.mralexeimk.objector.models;

import lombok.Data;
import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public abstract class Layer implements Serializable {
    private int units;
    private int size;
    private int index;
    private LayerType layerType;
    private Layer nextLayer;

    public Layer(int units, int size, int index, LayerType layerType) {
        this.units = units;
        this.size = size;
        this.layerType = layerType;
        this.index = index;
        nextLayer = null;
    }

    public String toString() {
        return getUnits()+"@"+getSize()+"x"+getSize() +", "+getLayerType().toString();
    }

    public void toDefault() {};

    public void addRow() {};

    public void removeRow(int index) {};

    public void evaluate() {};
}
