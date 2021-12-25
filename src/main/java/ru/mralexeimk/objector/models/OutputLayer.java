package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OutputLayer extends Layer implements Serializable {
    private List<Double> data;

    public OutputLayer(int units, int size, LayerType layerType) {
        super(units, size, layerType);
        data = new ArrayList<>();
    }

    public List<Double> getData() {
        return data;
    }

    public void setData(List<Double> data) {
        this.data = data;
    }
}
