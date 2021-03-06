package ru.mralexeimk.objector.models;

import ru.mralexeimk.objector.other.LayerType;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeuronsLayer extends Layer implements Serializable {
    private List<Double> data;
    private Matrix W;

    public NeuronsLayer(int units, int size, LayerType layerType) {
        super(units, size, 0, layerType);
        data = new ArrayList<>();
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

    public void addData(List<Double> data) {
        if(this.data.size() >= getUnits()) {
            this.data.clear();
        }
        this.data.addAll(data);
    }

    public void setW(Matrix W) {
        this.W = W;
    }

    @Override
    public void toDefault() {
        data = new ArrayList<>();
        W = new Matrix(getUnits(), getNextLayer().getUnits(),
                -1/Math.sqrt(getNextLayer().getUnits()), 1/Math.sqrt(getNextLayer().getUnits()));
    }

    @Override
    public void addRow() {
        Matrix bottomRow = new Matrix(getUnits(), 1);
        W.joinBottom(bottomRow);
    }

    @Override
    public void removeRow(int index) {
        W.removeRow(index);
    }

    @Override
    public void evaluate() {
        List<Double> res = NeuralNetworkListener.evaluateByDefault(data, W);
        if(getNextLayer() instanceof NeuronsLayer nl) {
            nl.setData(res);
        }
        else if(getNextLayer() instanceof OutputLayer ol) {
            ol.setData(res);
        }
    }
}
