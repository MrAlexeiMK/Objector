package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NeuronsLayer extends Layer implements Serializable {
    private List<Double> data;
    private Matrix W;

    public NeuronsLayer(int units, int size, LayerType layerType) {
        super(units, size, layerType);
        data = new ArrayList<>();
        W = new Matrix(1, 1);
    }

    public List<Double> getData() {
        return data;
    }

    public Matrix getW() {
        return W;
    }

    public void setData(List<Double> data) {
        this.data = data;
    }

    public void setW(Matrix W) {
        this.W = W;
    }

    @Override
    public void toDefault() {
        W = new Matrix(getUnits(), getNextLayer().getUnits(),
                -1/Math.sqrt(getNextLayer().getUnits()), 1/Math.sqrt(getNextLayer().getUnits()));
    }

    @Override
    public void evaluate() {
        List<Double> res = evaluateByDefault(data, W);
        if(getNextLayer() instanceof NeuronsLayer nl) {
            nl.setData(res);
        }
        else if(getNextLayer() instanceof OutputLayer ol) {
            ol.setData(res);
        }
    }
}
