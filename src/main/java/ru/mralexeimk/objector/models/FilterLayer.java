package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FilterLayer extends Layer implements Serializable {
    private List<Matrix> data;
    private int K;

    public FilterLayer(int units, int size, LayerType layerType) {
        super(units, size, layerType);
        data = new ArrayList<>();
        K = 2;
    }

    public List<Matrix> getData() {
        return data;
    }

    public void setData(List<Matrix> data) {
        this.data = data;
    }

    @Override
    public void toDefault() {
        K = getSize()/ getNextLayer().getSize();
    }

    @Override
    public void evaluate() {
        Layer next = getNextLayer();
        if(next instanceof PullingLayer pl) {
            pl.setData(evaluateByPulling(data, K));
        }
    }
}
